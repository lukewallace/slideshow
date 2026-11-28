package com.usbscreensaver

import android.content.Context
import android.graphics.BitmapFactory
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors
import java.util.zip.ZipInputStream
import javax.net.ssl.HttpsURLConnection

internal fun galleryPhotos(context: Context): List<() -> InputStream> =
    FirebaseGallery.photos(context).map { file -> { file.inputStream() } }

internal object FirebaseGallery {
    private val worker = Executors.newSingleThreadExecutor()
    private const val DAY = 24 * 60 * 60 * 1000L
    private const val MAX_BYTES = 256L * 1024 * 1024
    private fun prefs(context: Context) = context.getSharedPreferences("firebase_gallery", Context.MODE_PRIVATE)
    private fun root(context: Context) = File(context.noBackupFilesDir, "gallery")

    fun photos(context: Context): List<File> {
        val active = prefs(context).getString("active", null) ?: return emptyList()
        return File(root(context), active).listFiles().orEmpty().filter { it.isFile && isPhoto(it.name) }
    }

    fun refresh(context: Context, force: Boolean = false, done: (Boolean) -> Unit = {}) {
        val app = context.applicationContext
        worker.execute {
            val state = prefs(app)
            val now = System.currentTimeMillis()
            if (!force && now - state.getLong("checked", 0) in 0 until DAY) return@execute
            val success = try {
                sync(app)
                true
            } catch (_: Exception) { false }
            state.edit().putLong("checked", now).apply()
            done(success)
        }
    }

    private var cleaned = false
    private fun sync(context: Context) {
        val state = prefs(context)
        val root = root(context)
        check(root.isDirectory || root.mkdirs())
        if (!cleaned) {
            // Only reclaim old generations at process startup; a running slideshow may still use one.
            val active = state.getString("active", null)
            root.listFiles()?.filter { it.name != active }?.forEach { it.deleteRecursively() }
            cleaned = true
        }
        val url = URL("https://firebasestorage.googleapis.com/v0/b/" +
            BuildConfig.GALLERY_BUCKET + "/o/gallery.zip?alt=media")
        val connection = url.openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.useCaches = false
            if (photos(context).isNotEmpty()) state.getString("etag", null)?.let {
                connection.setRequestProperty("If-None-Match", it)
            }
            if (connection.responseCode == 304) return
            check(connection.responseCode == 200 && connection.contentLength.toLong() <= MAX_BYTES)
            val next = File(root, UUID.randomUUID().toString())
            check(next.mkdir())
            try {
                connection.inputStream.use { unpack(it, next) }
                check(state.edit().putString("active", next.name)
                    .putString("etag", connection.getHeaderField("ETag")).commit())
            } catch (e: Exception) {
                next.deleteRecursively()
                throw e
            }
        } finally { connection.disconnect() }
    }

    private fun unpack(input: InputStream, dir: File) {
        var count = 0
        var total = 0L
        val buffer = ByteArray(32768)
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && isPhoto(entry.name) && ++count <= 1000)
                val file = File(dir, "${count}." + entry.name.substringAfterLast('.').lowercase())
                file.outputStream().use { out ->
                    while (true) {
                        val n = zip.read(buffer)
                        if (n < 0) break
                        total += n
                        require(total <= MAX_BYTES)
                        out.write(buffer, 0, n)
                    }
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0)
            }
        }
        require(count > 0)
    }
}

internal class GalleryRequest(private val activity: MainActivity, private val ready: () -> Unit) {
    private var closed = false
    fun start() {
        if (FirebaseGallery.photos(activity).isNotEmpty()) {
            FirebaseGallery.refresh(activity, true)
            ready()
            return
        }
        activity.updatePrompt("Downloading Internal Gallery…")
        FirebaseGallery.refresh(activity, true) { success ->
            activity.runOnUiThread {
                if (!closed) {
                    if (success) ready()
                    else activity.updatePrompt("Gallery unavailable. Check the internet connection and try again.", "Retry") { start() }
                }
            }
        }
    }

    fun close() { closed = true }
}
