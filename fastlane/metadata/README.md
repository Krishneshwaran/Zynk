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

## Replace these placeholders before publishing

| File | Placeholder |
|---|---|
| `contact_email.txt` | your support email |
| `contact_website.txt` | your project / website URL |
| `privacy_policy_url.txt` | a live privacy policy URL (**required** by Play) |

Screenshots and graphics go in `images/` — see `README.md` for the upload flow.

## Upload to Google Play

```bash
# after `gem install fastlane` and setting up a Play service account
fastlane supply --skip_upload_images --skip_upload_screenshots
```
