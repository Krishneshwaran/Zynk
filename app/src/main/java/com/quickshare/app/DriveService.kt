package com.quickshare.app

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject

object DriveService {

    const val SCOPE = "https://www.googleapis.com/auth/drive.file"

    private const val UPLOAD_URL =
        "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name"
    private const val PERMISSION_URL =
        "https://www.googleapis.com/drive/v3/files/%s/permissions"

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .build()
    }

    fun token(context: Context, account: GoogleSignInAccount): String {
        val acct = account.account
            ?: throw IOException("No Google account selected on this device.")
        return GoogleAuthUtil.getToken(
            context.applicationContext,
            acct,
            "oauth2:$SCOPE"
        )
    }

    fun meta(resolver: ContentResolver, uri: Uri): Pair<String, Long?> {
        var name = "file"
        var size: Long? = null
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    name = cursor.getString(nameIndex)
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        if (name.isBlank()) {
            name = uri.lastPathSegment ?: "file"
        }
        return name to size
    }

    fun upload(
        context: Context,
        token: String,
        uri: Uri,
        fileName: String,
        size: Long?
    ): String {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"

        val metadata = JSONObject().put("name", fileName).toString()
        val boundary = "zynk_" + System.currentTimeMillis()
        val body = RelatedMultipartBody(
            boundary = boundary,
            metadata = metadata,
            mimeType = mimeType,
            resolver = resolver,
            uri = uri,
            fileSize = size
        )

        val request = Request.Builder()
            .url(UPLOAD_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(errorMessage(response.code, text))
            }
            val id = JSONObject(text).optString("id")
            if (id.isBlank()) throw IOException("Upload failed: no file id returned.")
            return id
        }
    }

    fun makePublic(token: String, fileId: String) {
        val payload = JSONObject()
            .put("role", "reader")
            .put("type", "anyone")
            .toString()

        val request = Request.Builder()
            .url(PERMISSION_URL.format(fileId))
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json; charset=UTF-8")
            .post(payload.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val text = response.body?.string().orEmpty()
                throw IOException(errorMessage(response.code, text))
            }
        }
    }

    fun shareLink(fileId: String): String =
        "https://drive.google.com/file/d/$fileId/view?usp=sharing"

    private fun errorMessage(code: Int, body: String): String {
        val detail = try {
            val message = JSONObject(body)
                .optJSONObject("error")
                ?.optString("message")
                .orEmpty()
            if (message.isBlank()) body else message
        } catch (e: Exception) {
            body
        }
        return "Drive error (HTTP $code): ${detail.take(300)}"
    }

    private class RelatedMultipartBody(
        private val boundary: String,
        private val metadata: String,
        private val mimeType: String,
        private val resolver: ContentResolver,
        private val uri: Uri,
        private val fileSize: Long?
    ) : RequestBody() {

        override fun contentType() =
            "multipart/related; boundary=$boundary".toMediaType()

        override fun contentLength(): Long {
            if (fileSize == null) return -1
            return head().size.toLong() + fileSize + tail().size
        }

        override fun writeTo(sink: BufferedSink) {
            sink.write(head())
            val input = resolver.openInputStream(uri)
                ?: throw IOException("Could not open the selected file.")
            input.use { stream ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    sink.write(buffer, 0, read)
                }
            }
            sink.write(tail())
        }

        private fun tail(): ByteArray =
            "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)

        private fun head(): ByteArray {
            val text = buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Type: ").append(mimeType).append("\r\n\r\n")
            }
            return text.toByteArray(Charsets.UTF_8)
        }
    }
}
