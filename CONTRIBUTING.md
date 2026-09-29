# Contributing to Zynk

Thanks for your interest in improving Zynk! 🎉
Please read this before opening a pull request, and also read our
[Code of Conduct](CODE_OF_CONDUCT.md).

## Getting started

1. Fork the repository and clone your fork.
2. Open the project in **Android Studio** (or run `./gradlew assembleDebug`).
3. Create a branch for your change:

   ```bash
   git checkout -b fix/short-description-of-change
   ```

## Google Cloud credentials (required to run the app)

The app talks to Google Drive, so a one-time OAuth setup is needed before it can
be signed in or tested. Follow **step 2 of the README**.

> Never commit your client secrets, keystores, `local.properties`, or any
> generated credentials. They are git-ignored for a reason — please don't force
> add them.

## Making a change

- Match the existing code style (Kotlin coding conventions, XML layout style).
- Keep the UI simple: white background, black accents, Material 3.
- Don't add a dependency if the platform already provides it.
- Test on a real device or emulator with Google Play services — sign-in and the
  upload flow must still work.

## Before you push

```bash
./gradlew assembleDebug
```

The build must succeed with no errors.

## Pull requests

1. Make sure your change is small and focused — one fix or one feature per PR.
2. Write a clear title and description:
   - **what** changed
   - **why** it was needed
   - **how** you tested it
3. Link the related issue (see below).
4. If the change affects the UI, include a screenshot or screen recording.

## Reporting a bug

Open an issue using the **Bug report** template and include:

- App version
- Android version and device
- The exact error text shown in the red status message (if any)
- Steps to reproduce

## Suggesting a feature

Open an issue using the **Feature request** template first, before writing any
code, so we can agree on the approach.

## Project structure

```
app/src/main/java/com/quickshare/app/
├── MainActivity.kt       UI, sign-in, upload flow, clipboard, history
├── DriveService.kt       OAuth token, multipart upload, make public, link
├── HistoryAdapter.kt     RecyclerView adapter for the history list
└── data/HistoryDb.kt     SQLiteOpenHelper + HistoryItem
```
