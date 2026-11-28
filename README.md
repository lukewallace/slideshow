# USB Screensaver for Android TV

Displays photos from USB storage. The settings screen controls timing and crossfade and can start the slideshow immediately.

[Read the user and publisher guide](docs/USB-Screensaver-Guide.md) for TV setup, updates, and publishing instructions.

## Builds and distribution

Use Android Studio with Android SDK 34 and Gradle 8.5. Development builds use the application ID com.usbscreensaver.debug. The unsigned release build uses com.usbscreensaver. Gradle 8.5 can run :app:assembleRelease or :app:bundleRelease from the project root.

The repository contains no production signing credentials. The owner authorizes signing with the enrolled service and publishes approved artifacts. Keep the public certificate fingerprint in release/app-signing-sha256.txt after checking it directly in the signing service. Before distributing a signed APK, run release/Verify-Apk.ps1 with -Apk and -BuildTools pointing to the signed package and Android SDK build-tools directory.

Keep owner access protected with a passkey or security key. Give other GitHub users read access only. Review installed GitHub Apps, deploy keys and automation tokens for release or contents-write access. A draft or tag alone does not sign an APK. Android checks the signing identity when installing updates.

The original personal builds used a local test signing identity. Moving to a new production certificate ordinarily requires a one-time reinstall for those users. Later production updates must use the same signing identity and increasing version codes.

## Image Source and gallery delivery

USB is the default. Both the standalone APK and the Play bundle download Internal Gallery from Cloud Storage for Firebase. Select Internal Gallery for the initial download or to check for changes. The TV keeps a complete local copy for offline use and checks again when the slideshow starts, at most once a day. A refresh becomes visible the next time the slideshow opens.

Upload a ZIP of photos to the root of the Slideshow Firebase Storage bucket as `gallery.zip`. Put photos directly inside the ZIP. An update replaces this one object and needs no new app release. The TV uses the HTTP ETag to skip an unchanged download. For a new bucket, the rules in `release/gallery-storage.rules` allow anyone to download only the published gallery. Manage uploads in the Firebase console with the owner account.

Build the standalone APK with `gradle :app:assembleRelease` and the Play bundle with `gradle -PplayDistribution=true :app:bundleRelease`.
