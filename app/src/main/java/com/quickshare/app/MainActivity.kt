package com.quickshare.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.quickshare.app.data.HistoryDb
import com.quickshare.app.data.HistoryItem
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var root: LinearLayout
    private lateinit var accountRow: LinearLayout
    private lateinit var accountEmail: TextView
    private lateinit var btnSignIn: MaterialButton
    private lateinit var btnSignOut: MaterialButton
    private lateinit var btnPick: MaterialButton
    private lateinit var btnCopy: MaterialButton
    private lateinit var btnOpen: MaterialButton
    private lateinit var btnClear: MaterialButton
    private lateinit var status: TextView
    private lateinit var progress: LinearProgressIndicator
    private lateinit var linkCard: LinearLayout
    private lateinit var linkName: TextView
    private lateinit var linkUrl: TextView
    private lateinit var emptyText: TextView
    private lateinit var recycler: RecyclerView

    private lateinit var signInClient: GoogleSignInClient
    private lateinit var db: HistoryDb
    private lateinit var adapter: HistoryAdapter

    private var account: GoogleSignInAccount? = null
    private var pendingSignIn = false
    private var busy = false
    private var lastLink: String? = null
    private var recoveryAttempted = false
    private var pendingUploadUri: Uri? = null

    private val gso: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveService.SCOPE))
            .build()
    }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleSignIn(result.data)
        }

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                recoveryAttempted = false
                upload(uri)
            }
        }

    private val recoverLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri = pendingUploadUri
            pendingUploadUri = null
            if (uri != null) upload(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        accountRow = findViewById(R.id.accountRow)
        accountEmail = findViewById(R.id.accountEmail)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignOut = findViewById(R.id.btnSignOut)
        btnPick = findViewById(R.id.btnPick)
        btnCopy = findViewById(R.id.btnCopy)
        btnOpen = findViewById(R.id.btnOpen)
        btnClear = findViewById(R.id.btnClear)
        status = findViewById(R.id.status)
        progress = findViewById(R.id.progress)
        linkCard = findViewById(R.id.linkCard)
        linkName = findViewById(R.id.linkName)
        linkUrl = findViewById(R.id.linkUrl)
        emptyText = findViewById(R.id.emptyText)
        recycler = findViewById(R.id.recycler)

        signInClient = GoogleSignIn.getClient(this, gso)
        db = HistoryDb(this)

        adapter = HistoryAdapter(
            onCopy = { item -> copyLink(item.link) },
            onOpen = { item -> openLink(item.link) },
            onDelete = { item -> deleteItem(item) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        account = GoogleSignIn.getLastSignedInAccount(this)
        renderAccount()
        refreshHistory()

        btnSignIn.setOnClickListener { pendingSignIn = false; startSignIn() }
        btnSignOut.setOnClickListener { signOut() }
        btnPick.setOnClickListener { chooseFile() }
        btnCopy.setOnClickListener { lastLink?.let { copyLink(it) } }
        btnOpen.setOnClickListener { lastLink?.let { openLink(it) } }
        btnClear.setOnClickListener { clearHistory() }
    }

    private fun startSignIn() {
        setHint(getString(R.string.status_signing_in))
        signInLauncher.launch(signInClient.signInIntent)
    }

    private fun handleSignIn(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            account = task.getResult(ApiException::class.java)
            renderAccount()
            if (pendingSignIn) {
                pendingSignIn = false
                filePicker.launch(arrayOf("*/*"))
            } else {
                setHint(getString(R.string.status_idle))
            }
        } catch (e: ApiException) {
            pendingSignIn = false
            renderAccount()
            setHint(getString(R.string.status_cancelled), error = true)
        }
    }

    private fun signOut() {
        pendingSignIn = false
        signInClient.signOut().addOnCompleteListener {
            account = null
            lastLink = null
            linkCard.isVisible = false
            renderAccount()
            setHint(getString(R.string.status_idle))
        }
    }

    private fun renderAccount() {
        val acc = account
        accountRow.isVisible = acc != null
        btnSignIn.isVisible = acc == null
        if (acc != null) {
            accountEmail.text = acc.email ?: acc.displayName ?: "Signed in"
        }
    }

    private fun chooseFile() {
        if (busy) return
        if (account == null) {
            pendingSignIn = true
            setHint(getString(R.string.status_signin_required))
            startSignIn()
        } else {
            filePicker.launch(arrayOf("*/*"))
        }
    }

    private fun upload(uri: Uri) {
        val acc = account
        if (acc == null) {
            pendingSignIn = true
            setHint(getString(R.string.status_signin_required))
            startSignIn()
            return
        }
        if (busy) return

        busy = true
        progress.isVisible = true
        progress.isIndeterminate = true
        btnPick.isEnabled = false
        btnSignIn.isEnabled = false
        linkCard.isVisible = false

        lifecycleScope.launch {
            try {
                setHint(getString(R.string.status_reading))
                val (name, size) =
                    withContext(Dispatchers.IO) { DriveService.meta(contentResolver, uri) }

                setHint(getString(R.string.status_uploading))
                val token =
                    withContext(Dispatchers.IO) { DriveService.token(this@MainActivity, acc) }
                val fileId = withContext(Dispatchers.IO) {
                    DriveService.upload(applicationContext, token, uri, name, size)
                }

                setHint(getString(R.string.status_sharing))
                withContext(Dispatchers.IO) { DriveService.makePublic(token, fileId) }

                val link = DriveService.shareLink(fileId)
                withContext(Dispatchers.IO) { db.insert(name, link) }

                lastLink = link
                linkName.text = name
                linkUrl.text = link
                linkCard.isVisible = true

                setHint(getString(R.string.status_done))
                copyLink(link, announce = true)
                refreshHistory()
                recoveryAttempted = false
            } catch (e: UserRecoverableAuthException) {
                if (!recoveryAttempted) {
                    recoveryAttempted = true
                    pendingUploadUri = uri
                    setHint(getString(R.string.status_reauth))
                    try {
                        recoverLauncher.launch(e.intent)
                    } catch (inner: Exception) {
                        fail("Drive permission could not be refreshed. Please sign in again.")
                    }
                } else {
                    fail("Drive access was not granted. Please sign out and sign in again.")
                }
            } catch (e: GoogleAuthException) {
                fail(
                    "Google Drive permission problem: " +
                        (e.message ?: "access was not granted") +
                        ".\nCheck the Google Cloud setup (package name + SHA-1) and sign in again."
                )
            } catch (e: IOException) {
                fail(e.message ?: "Network error while uploading.")
            } catch (e: Exception) {
                fail(e.message ?: "Something went wrong.")
            } finally {
                busy = false
                progress.isVisible = false
                btnPick.isEnabled = true
                btnSignIn.isEnabled = true
            }
        }
    }

    private fun refreshHistory() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { db.all() }
            adapter.submit(items)
            emptyText.isVisible = items.isEmpty()
        }
    }

    private fun deleteItem(item: HistoryItem) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { db.delete(item.id) }
            refreshHistory()
        }
    }

    private fun clearHistory() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { db.clear() }
            refreshHistory()
            Toast.makeText(this@MainActivity, R.string.history_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyLink(link: String, announce: Boolean = false) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Zynk link", link))
        if (announce) {
            Snackbar.make(root, R.string.link_copied, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun openLink(link: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        } catch (e: Exception) {
            Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setHint(text: String, error: Boolean = false) {
        status.text = text
        status.setTextColor(getColor(if (error) R.color.error else R.color.grey))
    }

    private fun fail(message: String) {
        setHint(message, error = true)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
