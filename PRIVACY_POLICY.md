# Privacy Policy — QuickShare

**Last updated:** 29 September 2026

QuickShare is a small Android application that uploads files from your device to
your own Google Drive. This policy explains exactly what it does and does not do.

## What the app accesses

- **Your Google account name/email** — used only to show which account you are
  signed in with.
- **Google Drive (`drive.file` scope)** — used to upload the file you picked and
  to make that file viewable by anyone with the link.
- **Files you explicitly select** — QuickShare reads a file only after you choose
  it with the system file picker, and only to upload it.

The app requests no other Google scopes and no other device permissions.

## What is stored on your device

A local SQLite database containing, for each upload:

- the file name
- the shareable link
- the date and time of the upload

You can delete individual entries or clear the whole list from inside the app.
Uninstalling the app removes this database.

## What is NOT collected

- No analytics, no trackers, no advertising SDKs
- No crash reporting
- No account, username or password of our own
- No file contents stored anywhere other than your Google Drive

## Where your file goes

Your file is uploaded **directly from your device to your own Google Drive**
using Google's official Drive API. QuickShare has **no server**: at no point does
any file or metadata pass through infrastructure we control.

## Third parties

- **Google** — Google Sign-In and the Google Drive API. Your use of Google
  services is governed by [Google's Privacy Policy](https://policies.google.com/privacy).
- The source code is published on GitHub under the MIT License so anyone can
  verify these claims.

## Children

The app is not directed at children under 13 and does not knowingly collect
personal information from them.

## Changes

Any change to this policy will be published in this file with an updated date.

## Contact

Questions? Open an issue at
[https://github.com/OWNER/QuickShare/issues](https://github.com/OWNER/QuickShare/issues)
or email **[your-email@example.com]**.
