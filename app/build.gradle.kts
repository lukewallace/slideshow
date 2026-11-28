plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val playDistribution = providers.gradleProperty("playDistribution").orNull == "true"

android {
    buildFeatures { buildConfig = true }
    sourceSets["release"].manifest.srcFile(if (playDistribution) "src/play/AndroidManifest.xml" else "src/direct/AndroidManifest.xml")
    sourceSets["debug"].manifest.srcFile(if (playDistribution) "src/play/AndroidManifest.xml" else "src/direct/AndroidManifest.xml")
    namespace = "com.usbscreensaver"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.usbscreensaver"
        minSdk = 21
        targetSdk = 34
        versionCode = 34
        versionName = "1.3.23"
        buildConfigField("String", "GALLERY_BUCKET", "\"slideshow-864f5.firebasestorage.app\"")
        buildConfigField("boolean", "PLAY_DISTRIBUTION", playDistribution.toString())

    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            signingConfig = null
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
}





