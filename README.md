# QuickShare

A tiny native Android app: pick any file → it uploads to **your Google Drive automatically**, is **shared publicly**, the **link is copied to your clipboard**, and every upload is saved to a local **history**.

Minimal Material 3 UI — white background, black accents. No clutter, no accounts of its own.

---

## Features

- Sign in with Google (Drive access only — no server, nothing leaves your device except the upload)
- Pick **any** file type (images, PDF, video, zip, …)
- **Automatic** upload to Google Drive the moment you pick a file
- **One tap → public**: the file is set to *anyone with the link can view*
- Link is **auto-copied to the clipboard** and shown on screen
- **History** of every upload (SQLite) — tap to copy the link again, ✕ to remove, *Clear* to wipe
- Handles Google's 7-day token expiry **automatically** (asks for permission again by itself)

## Tech

Kotlin · XML views · Material 3 · RecyclerView · `SQLiteOpenHelper` · Google Sign-In (`play-services-auth`) · OkHttp (Drive REST API)

Min SDK 26 (Android 8.0) · Target SDK 36

---

## Requirements

- Android Studio (or a JDK 17+ and the Android SDK)
- A Google account
- A Google Cloud project (free) — see below

---

## 1. Build the app

```bash
git clone https://github.com/<you>/QuickShare.git
cd QuickShare
```

Open the folder in **Android Studio** → let Gradle sync → **Run ▶**.

Or from the command line:

```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

On Windows PowerShell you may first need:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

---

## 2. Google Cloud setup (one-time, required)

The app talks directly to Google Drive, so you must register it once with Google.
**Without this step, sign-in returns `Error 403: access_denied`.**

### a. Create a project & enable the Drive API

1. <https://console.cloud.google.com/> → **New Project** → name it → **Create**
2. **APIs & Services → Library** → search **Google Drive API** → **Enable**

### b. OAuth consent screen

1. **APIs & Services → OAuth consent screen**
2. User type **External** → **Create**
3. App name (e.g. `QuickShare`), support email, developer contact → **Save and Continue**
4. **Scopes → Add or Remove Scopes** — add:
   - `https://www.googleapis.com/auth/userinfo.email`
   - `https://www.googleapis.com/auth/drive.file`
5. **Test users → Add users** → add every Gmail address that should be able to sign in → **Save**

> `drive.file` is a *sensitive* scope, so while the app is in **Testing** only listed test users can use it (up to 100).
> Want **anyone** to sign in instead? Click **Publish app** — users then see a
> *"Google hasn't verified this app"* warning and tap **Advanced → Go to \<app name\>**.

### c. Android OAuth client

1. **APIs & Services → Credentials → Create Credentials → OAuth client ID**
2. Application type: **Android**
3. **Package name:** `com.quickshare.app`
4. **SHA-1 certificate fingerprint:** your own debug fingerprint — get it with:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
```

Windows (Android Studio's bundled JDK):

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v `
  -keystore "$env:USERPROFILE\.android\debug.keystore" `
  -alias androiddebugkey -storepass android
```

Or in Android Studio: **Gradle sidebar → Tasks → android → signingReport**.

> There is **no client ID to paste into the code** — Play Services finds this credential
> automatically from your package name + SHA-1. A mismatch shows up as
> `Google Drive permission problem: …` in the app.

---

## 3. How to use it

1. Open **QuickShare** → **Sign in with Google** → pick your account → **Allow** (twice if asked: sign-in + Drive)
2. **Choose file to upload** → pick anything from your phone
3. Wait a second — it uploads, makes the file public, and copies the link
4. The link appears on screen; the file also appears under **Recent Uploads**
   - tap the row → open the link in the browser
   - tap 🗙 → remove it from history (the Drive file stays)
   - **Clear** → wipe the whole history

The file lands in **your own** Google Drive, in the root folder, as a link anyone can view.

---

## 4. Release build (optional)

```bash
./gradlew assembleRelease
```

The release APK is **unsigned** until you add a signing config. If you create a release
keystore, you must also add a **second Android OAuth client** in Google Cloud using the
release keystore's SHA-1 — otherwise sign-in will fail on the release build.

```bash
keytool -genkey -v -keystore release.keystore -alias quickshare -keyalg RSA -keysize 2048 -validity 10000
```

---

## Project structure

```
app/src/main/java/com/quickshare/app/
├── MainActivity.kt       UI, sign-in, upload flow, clipboard, history
├── DriveService.kt       OAuth token, multipart upload, "make public", share link
├── HistoryAdapter.kt     RecyclerView adapter for the history list
└── data/HistoryDb.kt     SQLiteOpenHelper + HistoryItem
```

| Concern | Where |
|---|---|
| Requesting Drive permission | `DriveService.SCOPE` |
| Uploading the file | `DriveService.upload()` → `POST /upload/drive/v3/files?uploadType=multipart` |
| Making it public | `DriveService.makePublic()` → `POST /drive/v3/files/{id}/permissions` |
| Share link | `DriveService.shareLink()` → `https://drive.google.com/file/d/{id}/view?usp=sharing` |
| History storage | `data/HistoryDb.kt` (table `history`) |

---

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Error 403: access_denied` on sign-in | Your account is not a **test user**. Add it in OAuth consent screen → Test users, or click **Publish app**. |
| `Google Drive permission problem: …` | Package name or SHA-1 doesn't match the Android OAuth client. Re-check `com.quickshare.app` and your debug SHA-1 (can take ~5 min to apply). |
| *"Google hasn't verified this app"* | Expected while unverified. Tap **Advanced → Go to \<app\>**. Complete Google's verification to remove it. |
| Sign-in asks for permission again every ~7 days | Normal — refresh tokens expire after 7 days while the app is in *Testing*. The app re-asks automatically. |
| `Drive error (HTTP 4xx)` in red text | The message includes Google's explanation — usually quota, network, or a permission change. |

---

## Privacy

The app has no backend. Your file is uploaded straight from your phone to **your own Google Drive** using Google's official API. The only data stored on the phone is the local upload history (file name, link, timestamp) in a SQLite database — clearing it from the app removes it.

---

## License

[MIT](LICENSE)
