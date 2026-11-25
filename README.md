# USB Screensaver for Android TV

Displays photos from USB storage. The settings screen controls timing and crossfade and can start the slideshow immediately.

[Download the user and publisher guide](https://github.com/lukewallace/slideshow/raw/refs/heads/main/docs/USB-Screensaver-Guide.html) and open it in your browser for TV setup, updates, and publishing instructions.

## Builds and distribution

Use Android Studio with Android SDK 34 and Gradle 8.5. Development builds use the application ID com.usbscreensaver.debug. The unsigned release build uses com.usbscreensaver. Gradle 8.5 can run :app:assembleRelease or :app:bundleRelease from the project root.

The repository contains no production signing credentials. The owner authorizes signing with the enrolled service and publishes approved artifacts. Keep the public certificate fingerprint in release/app-signing-sha256.txt after checking it directly in the signing service. Before distributing a signed APK, run release/Verify-Apk.ps1 with -Apk and -BuildTools pointing to the signed package and Android SDK build-tools directory.

Keep owner access protected with a passkey or security key. Give other GitHub users read access only. Review installed GitHub Apps, deploy keys and automation tokens for release or contents-write access. A draft or tag alone does not sign an APK. Android checks the signing identity when installing updates.

The original personal builds used a local test signing identity. Moving to a new production certificate ordinarily requires a one-time reinstall for those users. Later production updates must use the same signing identity and increasing version codes.
