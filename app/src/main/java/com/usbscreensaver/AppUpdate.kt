package com.usbscreensaver

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

internal class AppUpdate(private val activity: MainActivity) {
    private val pm = activity.packageManager
    private var pendingFile: File? = null

    fun dismiss() {
        pendingFile?.delete()
        pendingFile = null
    }

    fun check() {
        Thread {
            val directory = File(activity.cacheDir, "updates")
            val file = File(directory, "${UUID.randomUUID()}.apk")
            try {
                directory.mkdirs()
                directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86400000L }
                    ?.forEach { it.delete() }
                if (!download(file)) {
                    throw UpdateMessage("There is no published update available.")
                }
                val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
                            else PackageManager.GET_SIGNATURES
                val installed = pm.getPackageInfo(activity.packageName, flags)
                val update = pm.getPackageArchiveInfo(file.path, flags)
                if (update == null || update.packageName != installed.packageName)
                    throw UpdateMessage("The release does not contain a valid update for this app.")
                // Identical APKs need no installation, even if the TV omits archive signing metadata.
                if (version(update) == version(installed) &&
                    digest(file).contentEquals(digest(File(activity.applicationInfo.sourceDir))))
                    throw UpdateMessage("You already have the latest available version.")
                val current = fingerprints(installed) {
                    pm.getPackageInfo(activity.packageName, PackageManager.GET_SIGNATURES)
                }
                val incoming = fingerprints(update) {
                    pm.getPackageArchiveInfo(file.path, PackageManager.GET_SIGNATURES)
                }
                if (current.isEmpty() || incoming.isEmpty())
                    throw UpdateMessage("This TV could not read the signing certificate. Installation was stopped.")
                if (current != incoming || current != setOf(PRODUCTION_SIGNER))
                    throw UpdateMessage("The signing certificate does not match the production release. Installation was stopped.")
                if (version(update) <= version(installed))
                    throw UpdateMessage("You already have the latest available version.")
                activity.runOnUiThread {
                    if (activity.isChecking(this)) {
                        pendingFile = file
                        activity.updatePrompt("Install USB Screensaver ${update.versionName} on this TV?", "Install") {
                            pendingFile = null
                            install(file)
                        }
                    } else file.delete()
                }
            } catch (e: Exception) {
                file.delete()
                show { message(if (e is UpdateMessage) e.message.orEmpty()
                               else "Could not check for updates. Check your internet connection and try again.") }
            }
        }.start()
    }

    private fun show(action: () -> Unit) = activity.runOnUiThread {
        if (activity.isChecking(this)) action()
    }

    private class UpdateMessage(message: String) : Exception(message)

    private fun message(text: String) {
        activity.updatePrompt(text)
    }

    private companion object {
        const val PRODUCTION_SIGNER = "68250a8bbced2cf24f169857de58328bf70f4e4d9003a585b1471b30699af3dc"
    }

    private fun fingerprints(info: PackageInfo, legacy: () -> PackageInfo?): Set<String> {
        val modern = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else null
        val signers = if (!modern.isNullOrEmpty()) modern
                      else if (Build.VERSION.SDK_INT >= 28) legacy()?.signatures else info.signatures
        return signers.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(java.util.Locale.ROOT, it) }
        }.toSet()
    }

    private fun digest(file: File): ByteArray {
        val sha = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(32768)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                sha.update(buffer, 0, count)
            }
        }
        return sha.digest()
    }

    private fun version(info: PackageInfo) =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    private fun download(file: File): Boolean {
        var url = URL("https://github.com/lukewallace/slideshow/releases/latest/download/USB-Screensaver.apk")
        repeat(6) {
            if (url.protocol != "https" || (url.host != "github.com" && !url.host.endsWith(".githubusercontent.com")))
                throw IOException("Unexpected download destination")
            val connection = url.openConnection() as HttpsURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 10000
                connection.readTimeout = 20000
                connection.setRequestProperty("User-Agent", "USB-Screensaver-Updater")
                when (connection.responseCode) {
                    301, 302, 303, 307, 308 -> url = URL(url, connection.getHeaderField("Location") ?: throw IOException())
                    404 -> return false
                    200 -> {
                        connection.inputStream.use { input ->
                            file.outputStream().use { output ->
                                val buffer = ByteArray(32768)
                                var total = 0L
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    if (total > 64L * 1024 * 1024) throw IOException("APK too large")
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        return true
                    }
                    else -> throw IOException("Download failed")
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }

    private fun install(file: File) {
        try {
            if (Build.VERSION.SDK_INT >= 26 && !pm.canRequestPackageInstalls()) {
                activity.updatePrompt("Allow USB Screensaver to install apps in TV settings. Then return and select Check for updates again.", "Open settings") {
                    open(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.packageName)))
                }
                file.delete()
                return
            }
            val uri = Uri.Builder().scheme("content").authority(activity.packageName + ".updates")
                .appendPath(file.name).build()
            open(Intent(Intent.ACTION_VIEW).setDataAndType(uri, UpdateProvider.APK_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (_: SecurityException) {
            message("This TV’s security settings prevent app installation.")
        }
    }

    private fun open(intent: Intent) {
        try {
            activity.startActivity(intent)
            activity.closePanel()
        } catch (_: ActivityNotFoundException) {
            message("This TV has no handler for this action. You can install the official APK by USB.")
        } catch (_: SecurityException) {
            message("This TV’s security settings prevent this action.")
        }
    }
}
