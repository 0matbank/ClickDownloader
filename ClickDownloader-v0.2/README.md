# Click Downloader v0.2 Advanced

Android media downloader starter built with Kotlin, Jetpack Compose, yt-dlp, FFmpeg, aria2, WorkManager and DataStore.

## Implemented

- Paste one or multiple URLs
- Android Share → Click Downloader
- Media title/uploader/duration analysis
- Quality presets: Auto, 1080p, 720p, 480p, 360p, audio MP3
- Batch URL queue
- Per-job progress and ETA
- Retry, cancel, clear finished
- Persistent download history
- Long-running WorkManager downloads with foreground notification
- Wi-Fi/unmetered-only constraint
- yt-dlp continuation (`--continue`) and `.part` files
- Fragment retries + exponential retry sleep
- aria2 parallel transfer
- FFmpeg merge
- Thumbnail + metadata embedding
- Subtitle download/embedding
- SponsorBlock option
- cookies.txt import into private app storage
- Filename template editor
- SAF folder picker with persisted access
- System dark/light mode
- No contacts, SMS, location, camera, microphone, accessibility, overlay, VPN, or APK-install permission

## Important v0.2 limitation

The custom SAF folder picker and persisted permission are implemented in the UI/settings layer,
but the yt-dlp engine still writes directly to `Downloads/ClickDownloader`.

Why: yt-dlp/FFmpeg expect filesystem paths while Android SAF gives a content URI.
A production implementation should download to app-controlled temporary storage and stream/copy
the completed output to the selected `DocumentFile` destination after post-processing.

The app does not claim the selected SAF directory is already used for yt-dlp output.

## Security

- Cookie imports are copied to the app's private files directory.
- `allowBackup=false`
- No self-updater / REQUEST_INSTALL_PACKAGES permission
- No DRM or paywall bypass
- Use cookies only for accounts/content you are authorized to access.

## Build

Open with a current Android Studio installation and Android 17 / API 37 SDK.

The project uses youtubedl-android 0.18.1. Native libraries are bundled, so APK size is expected
to be much larger than a normal Compose-only app.

## Recommended v0.3

- True raw-format matrix from yt-dlp JSON
- Playlist/channel expansion preview
- Proper SAF post-processing/export pipeline
- Concurrent queue limit (1/2/3 downloads)
- Per-site profiles
- Scheduled downloads
- Clipboard detector
- Duplicate URL detection
- Notification action buttons
- Per-job output location
- Engine-update screen with checksum/signature verification
- Full APK signing + reproducible build workflow
