# Fastlane metadata

Store listing text for Google Play (`fastlane supply`), in the standard
[fastlane metadata layout](https://docs.fastlane.tools/actions/supply/#language-and-directory-structure).

```
fastlane/metadata/android/
└── en-US/
    ├── title.txt              ≤ 30 characters
    ├── short_description.txt  ≤ 80 characters
    ├── full_description.txt   ≤ 4000 characters
    ├── changelogs/
    │   └── 1.txt              versionCode 1, ≤ 500 characters
    ├── contact_email.txt
    ├── contact_website.txt
    ├── privacy_policy_url.txt
    └── images/
        ├── icon.png             512×512
        ├── featureGraphic.png   1024×500
        └── phoneScreenshots/    16:9 or 9:16
```

## Filled in already

| File | Value |
|---|---|
| `contact_email.txt` | `info@ruroxz.in` (support) |
| `contact_website.txt` | `https://ruroxz.in` |

## Still to do

| File | What's needed |
|---|---|
| `privacy_policy_url.txt` | confirm the `OWNER` once the GitHub repo exists (or point it at `https://ruroxz.in/privacy`) |
| `images/icon.png` | 512×512 app icon |
| `images/featureGraphic.png` | 1024×500 Play feature graphic |
| `images/phoneScreenshots/` | store screenshots |
| `changelogs/1.txt` | edit if the app name or version changes |

## Upload to Google Play

```bash
# after `gem install fastlane` and setting up a Play service account
fastlane supply --skip_upload_images --skip_upload_screenshots
```
