# Click Downloader v0.3 Fixed

This is a clean Android project intended to be uploaded directly to the ROOT of a GitHub repository.

## Important

Do not upload the outer folder as another nested project folder.

Correct repository root:

- `.github/workflows/build-apk.yml`
- `app/`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `README.md`

## Build APK on GitHub

1. Upload the CONTENTS of this folder to the repository root.
2. Open GitHub → Actions.
3. Select `Build Click Downloader APK`.
4. Click `Run workflow`.
5. Wait for the green check.
6. Open the completed run.
7. Download artifact `ClickDownloader-v0.3-APK`.
8. Extract it and install `ClickDownloader-v0.3.apk` on Android.

## Features

- Paste one or many media URLs
- Android Share → Click Downloader
- Analyze first URL/title
- Auto / 1080p / 720p / 480p / 360p / MP3
- Batch queue
- Foreground/background download service
- Notification progress + cancel action
- yt-dlp engine
- FFmpeg merge/audio conversion
- aria2 option
- Continue/retry/fragment retry
- Embed thumbnail
- Embed metadata
- Download/embed subtitles
- Private cookies.txt import
- Download history
- Files saved in `Download/ClickDownloader`

## Security

This project intentionally does NOT request:

- Contacts
- SMS
- Location
- Camera
- Microphone
- Accessibility
- Overlay
- VPN
- APK install permission
- Manage all files permission

`cookies.txt` is copied into the app's private internal storage.

Use only for content you are allowed to download. No DRM or paywall bypass is included.

## Build stack

- Android Gradle Plugin: 8.13.0
- Gradle: 8.13
- Java: 17
- compileSdk: 35
- targetSdk: 35
- youtubedl-android: 0.18.1
