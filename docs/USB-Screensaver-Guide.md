# USB Screensaver for Android TV

User and publisher guide · Version 1.3.23 · 28 November 2026

Choose photos from a connected USB drive or download the publisher’s **Internal Gallery**. Both sources work with **Turn on Screensaver Now** and the TV’s automatic screensaver.

[Set up the TV](#set-up-the-tv) · [Use the menu](#use-the-main-menu) · [Configure Internal Gallery](#configure-internal-gallery) · [Update the app](#update-the-tv-over-the-internet) · [Publish a version](#publish-a-new-version-as-the-owner) · [Troubleshooting](#troubleshooting)

## Set up the TV

1. Download **USB-Screensaver.apk** from the [latest official GitHub release](https://github.com/lukewallace/slideshow/releases/latest). Copy it to a USB drive and open it with the TV’s file manager. Choose **Install** and allow the file manager to install apps if Android asks.
2. Open **Image Source**. For **USB** (the default), grant photo or storage access and keep the drive connected. The app scans accessible USB folders up to five levels deep. The pictures and APK can be in different folders.
3. For **Internal Gallery**, connect the TV to the internet, choose **Internal Gallery**, and wait for the first download. Version 1.3.23 launched with 60 photos in a 40.6 MB download. The app saves the selection when the gallery is ready. Subsequent playback works offline with the USB drive removed.
4. Select **Turn on Screensaver Now** to preview playback. For automatic playback, select **USB Screensaver** in Android TV’s system screensaver settings and choose an idle delay if your TV offers one.

## Use the main menu

- **Turn on Screensaver Now** starts the full-screen slideshow using the saved source, interval, transition and brightness.
- **Slideshow Interval** sets the time per photo. Choose 10 or 30 seconds, or 1, 2, 3, 5, 10 or 30 minutes. The default is 10 seconds.
- **Slideshow effects → Brightness** adjusts the app menu and slideshow from 10% to 100% in 5-point steps. Focus the slider and press Left or Right. Each change saves immediately.
- **Slideshow effects → Image Transition** offers **Crossfade** (the default, about 0.8 seconds) and **No Fade** for an immediate change.
- **Image Source** switches between **USB** and **Internal Gallery**. A checkmark identifies the saved choice. Photos shuffle and loop during playback.
- **Advanced settings → Check for updates** checks for a newer app version. The installed version appears below “2026” at the bottom of the main menu.

Press OK on a menu button to open its submenu on the right. Press Left or Back to return one level. While the brightness slider has focus, Left lowers brightness. Use Back to leave it.

## Configure Internal Gallery

### For viewers

The publisher manages one shared collection for all TVs. To check for fresh photos now, select **Image Source → Internal Gallery** again. With a saved gallery, the menu returns immediately and the check runs in the background. Start the slideshow again after the download finishes to see the new collection.

Starting a slideshow with Internal Gallery selected also triggers a background check if at least 24 hours have passed since the last check. The TV sends the saved ETag so the server can skip an unchanged download. If a refresh fails, the TV can keep playing its last complete gallery.

### For the publisher

Replace one file in Firebase to change the photos on TVs running the gallery-enabled app. A gallery change requires no new APK or GitHub release.

1. **Prepare the photos.** Choose 1 to 1,000 still images readable by Android. Supported extensions are JPG, JPEG, PNG, BMP, WebP and GIF. Use TV-sized copies to keep downloads small.
2. **Create the archive.** On Windows, open the folder of prepared photos, select the image files themselves, then choose **Compress to ZIP file** or **Send to → Compressed (zipped) folder**. Name it exactly `gallery.zip`. Open the ZIP and confirm the photos are at its top level. Exclude folders, thumbnail databases, videos and other files. Keep both the ZIP and the combined extracted image data at or below 256 MiB.
3. **Upload as the owner.** In project `slideshow-864f5`, open **Storage → Files** and select the root of `gs://slideshow-864f5.firebasestorage.app`. Choose **Upload file**, select `gallery.zip`, and confirm replacement of the existing object. Keep a local copy of the previous ZIP if you want to restore it. [Open Slideshow Storage](https://console.firebase.google.com/project/slideshow-864f5/storage)
4. **Verify the upload.** Download the published ZIP from the same address used by the TV, open it and check the photos. Then reselect **Internal Gallery** on a connected TV and restart the slideshow after the refresh. [Download the live gallery](https://firebasestorage.googleapis.com/v0/b/slideshow-864f5.firebasestorage.app/o/gallery.zip?alt=media)

The bucket uses Standard storage in US-EAST1 on the Blaze billing plan. Anyone with the gallery URL can download the photos. The deployed Firebase rules allow a public get only for `gallery.zip` and grant clients no listing or write access. Manage uploads using an authorized publisher account. The app reads the bucket name from `GALLERY_BUCKET` in `app/build.gradle.kts`. [Repository storage rules](https://github.com/lukewallace/slideshow/blob/main/release/gallery-storage.rules)

## Update the TV over the internet

1. For the GitHub APK, open **Advanced settings → Check for updates** while online. The app checks the downloaded APK’s package, signing identity and Android version code.
2. Select **Install** if a newer verified build is available. If Android asks for permission to install apps, grant it to USB Screensaver, return to the menu and check again.
3. Confirm in Android’s installer. If the TV returns to its home screen, reopen USB Screensaver and check the version below “2026”. An ordinary update preserves your preferences.

Early test-signed builds require a one-time uninstall and fresh installation to switch to production signing. Uninstalling resets preferences and removes locally downloaded gallery photos. USB photos stay on the drive.

## Publish a new version as the owner

1. Review and commit the app changes. Keep the application ID `com.usbscreensaver`. Set the display version and increase `versionCode` in `app/build.gradle.kts`. Version 1.3.23 uses code 34.
2. Build the standalone release APK with `gradle :app:assembleRelease`. Sign the zip-aligned APK using `release/Sign-AzureApk.ps1` and verify it using `release/Verify-Apk.ps1`. The existing non-exportable signing key is held by Azure Key Vault. [Signing instructions](https://github.com/lukewallace/slideshow/blob/main/release/AZURE.md)
3. Push the matching source commit. Create a GitHub release targeting it with the version tag, such as `v1.3.23`. Attach the signed APK with the exact asset name **USB-Screensaver.apk** and publish as the latest stable release.
4. Download from the TV’s update address, compare its SHA-256 file hash with the signed local build and verify the production certificate. Then test **Check for updates** on a TV running the previous version. [TV update address](https://github.com/lukewallace/slideshow/releases/latest/download/USB-Screensaver.apk)

The production certificate SHA-256 fingerprint is `68250a8bbced2cf24f169857de58328bf70f4e4d9003a585b1471b30699af3dc`. For a future Google Play submission, build the Play bundle with `gradle -PplayDistribution=true :app:bundleRelease`. That variant also uses Firebase for the gallery and directs app update checks to Google Play.

## Troubleshooting

- **Gallery unavailable on the first download.** Check the TV’s internet connection and free internal storage, then select **Retry**. The publisher can test the live gallery link and check the ZIP name, contents, size and Firebase read rule.
- **Old gallery photos still appear.** Reselect **Internal Gallery** while online, give the download time to finish, then stop and start the slideshow. A running slideshow keeps its current photo list.
- **No images found on USB.** Select **USB**, confirm the file manager can open a JPG, check photo permission and place the photos near the drive’s root. Restart the slideshow to rescan.
- **Manual playback works but the idle screensaver never starts.** Check Android TV’s selected system screensaver and idle delay.
- **There is no published update available.** Confirm the latest GitHub release includes **USB-Screensaver.apk**. If the app reports that you already have the latest available version, check the version in the footer.
- **Signing-certificate warning.** Keep the installed app in place while checking the official download with the verification script. For an older 1.3.3 updater that reports a mismatch, try installing a verified current production APK over it by USB. If Android also reports a signature conflict, identify the installed build before uninstalling.

Designed by Lukasz Walaszczyk · 2026
