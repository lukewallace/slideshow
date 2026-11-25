package com.usbscreensaver

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Button
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

internal class AppUpdate(private val activity: Activity, private val button: Button) {
    private val pm = activity.packageManager

    fun check() {
        button.isEnabled = false
        button.setText(R.string.checking_updates)
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
                val signers = signatures(installed)
                if (signers.isEmpty() || signers != signatures(update))
                    throw UpdateMessage("The release has a different signing certificate. For your security, installation was stopped.")
                if (version(update) <= version(installed))
                    throw UpdateMessage("You already have the latest available version.")
                show {
                    AlertDialog.Builder(activity).setTitle("Update available")
                        .setMessage("Install USB Screensaver ${update.versionName} on this TV?")
                        .setNegativeButton("Cancel") { _, _ -> file.delete() }
                        .setOnCancelListener { file.delete() }
                        .setPositiveButton("Install") { _, _ -> install(file) }.show()
                }
            } catch (e: Exception) {
                file.delete()
                show { message(if (e is UpdateMessage) e.message.orEmpty()
                               else "Could not check for updates. Check your internet connection and try again.") }
            }
        }.start()
    }

    private fun show(action: () -> Unit) = activity.runOnUiThread {
        if (!activity.isFinishing && !activity.isDestroyed) {
            button.isEnabled = true
            button.setText(R.string.check_updates)
            action()
        }
    }

    private class UpdateMessage(message: String) : Exception(message)

    private fun message(text: String) {
        AlertDialog.Builder(activity).setTitle(R.string.check_updates)
            .setMessage(text).setPositiveButton(android.R.string.ok, null).show()
    }

    private fun signatures(info: PackageInfo) =
        (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners
         else info.signatures)?.toSet().orEmpty()

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
                AlertDialog.Builder(activity).setTitle("Allow app updates")
                    .setMessage("Allow USB Screensaver to install apps in TV settings. Then return and select Check for updates again.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Open settings") { _, _ ->
                        open(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.packageName)))
                    }.show()
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
        } catch (_: ActivityNotFoundException) {
            message("This TV has no handler for this action. You can install the official APK by USB.")
        } catch (_: SecurityException) {
            message("This TV’s security settings prevent this action.")
        }
    }
}
