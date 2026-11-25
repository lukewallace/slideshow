# USB Screensaver for Android TV

User and publisher guide · Version 1.3.6 · 25 November 2026

The TV reads photos from your USB drive. Android controls when the screensaver starts automatically. GitHub delivers app updates when you select **Check for updates**. You can run the slideshow offline.

[Set up the TV](#set-up-the-tv) · [Use the menu](#use-the-main-menu) · [Update the TV](#update-the-tv-over-the-internet) · [Publish a version](#publish-a-new-version-as-the-owner) · [Troubleshooting](#troubleshooting)

## Set up the TV

1. Download **USB-Screensaver.apk** from the [latest official release](https://github.com/lukewallace/slideshow/releases/latest). An APK is the Android installation file.
2. Copy the APK to a USB drive. Put your pictures in a simple folder such as `Photos`. The APK and pictures can be in different folders. The app scans accessible USB storage up to five levels deep.
3. Plug the drive into the TV. Open the APK with the TV’s file manager and choose **Install**. If prompted, allow that file manager to install apps. Open USB Screensaver and grant the requested photo or storage permission.
4. Choose **Turn on Screensaver Now** to check your pictures. For automatic playback, select **USB Screensaver** in the TV’s system screensaver settings and choose an idle delay if available. The settings path varies by TV.

Keep the drive connected during playback. The app supports JPG, JPEG, PNG, BMP, WebP and GIF still images. Video playback is outside the scope of this photo slideshow. It shuffles the photos by default and loops when it reaches the end.

## Use the main menu

### Slideshow interval

Sets how long each photo stays on screen. Choose 10 seconds, 30 seconds, 1 minute, 2 minutes, 3 minutes, 5 minutes or 10 minutes. The default is 10 seconds. The TV’s idle delay is a separate system setting.

### Slideshow Effect

**Crossfade** blends into the next photo over 0.8 seconds. **No fade** switches immediately. From version 1.3.6, Crossfade is the default whenever the user has not selected an effect. An explicit selection survives an ordinary update.

### Turn on Screensaver Now

Opens the full-screen slideshow immediately with the same photos and preferences used for automatic playback.

### Check for updates

Checks the latest published release when selected. The footer below “2026” shows the version currently installed on this TV.

## Update the TV over the internet

1. Connect the TV to the internet and select **Check for updates**. The app downloads the APK from the [official update address](https://github.com/lukewallace/slideshow/releases/latest/download/USB-Screensaver.apk) and checks its package, signing identity and Android version code.
2. If a newer verified build is available, select **Install**. On the first update, Android may ask you to allow USB Screensaver to install apps. Grant that permission, return to the menu and select **Check for updates** again.
3. Confirm the update in Android’s installer. Reopen the app and check the version below “2026”. Your interval and effect carry over.

USB is only needed for your photos during normal use. For an app update, the TV can download the installation file directly. Users of the early test-signed builds need a one-time uninstall and fresh installation to switch to production signing. That migration resets app preferences and leaves the USB photos intact.

## Publish a new version as the owner

**Azure signs the APK. GitHub hosts it. Android verifies it on the TV.** Making a source change on GitHub alone does not deliver an update. The TV checks the APK attached to the latest public release.

1. **Prepare the source.** Make and review the change in the [slideshow repository](https://github.com/lukewallace/slideshow). In `app/build.gradle.kts`, set the new display version and increase `versionCode`. Version 1.3.6 uses code 17. Keep the application ID `com.usbscreensaver`.
2. **Build.** Build a zip-aligned unsigned release APK from that source. The repository’s Gradle release task is `:app:assembleRelease`.
3. **Sign and verify.** Sign in to Azure CLI as the authorized publisher. Use `release/Sign-AzureApk.ps1` with the existing vault `kv-slideshow-lw-sign` and certificate `slideshow-android-release`. The private key stays in Azure. The script checks the public fingerprint and verifies the resulting APK. Keep using the same certificate for future releases. [Signing instructions and prerequisites](https://github.com/lukewallace/slideshow/blob/main/release/AZURE.md).
4. **Publish.** On GitHub, create a release tagged with the version, such as `v1.3.6`, targeting the matching source commit. Attach the signed APK with the exact filename **USB-Screensaver.apk**. Publish it as the latest stable release.
5. **Verify delivery.** Download the file from the official update address above. Run `release/Verify-Apk.ps1` on that download and compare its SHA-256 file hash with the signed local APK. On a TV running the previous production version, select **Check for updates** and confirm the new footer version after installation.

The production certificate’s public SHA-256 fingerprint is `68250a8bbced2cf24f169857de58328bf70f4e4d9003a585b1471b30699af3dc`. It identifies the signer across releases. Each new APK has its own file hash. Protect both the GitHub publisher account and the Azure accounts that can grant signing access.

## Troubleshooting

### “No images found on USB” or new photos are missing

Use the TV’s file manager to confirm it can open a JPG from the drive. Check the app’s photo permission, try a folder close to the USB root, then stop and restart the slideshow. It rescans when it starts.

### The manual slideshow works, but the idle screensaver never starts

Check the TV’s selected system screensaver and idle delay. The slideshow interval only controls photo changes.

### “There is no published update available”

Check that the latest public GitHub release has an attachment named exactly `USB-Screensaver.apk`. The app shows this message when GitHub cannot find the download.

### “You already have the latest available version”

The downloaded release offers no newer version for this TV. Check the footer to see the installed version.

### A signing-certificate warning appears

Keep the installed app in place. Verify the official download with the repository’s verification script. Version 1.3.3 could report a mismatch when a TV omitted certificate information. For that older updater, install a verified current production APK over the existing app by USB. If Android’s installer also reports a signature conflict, stop and identify the installed build before uninstalling.

Designed by Lukasz Walaszczyk · 2026
