package com.usbscreensaver

import android.content.Context
import android.widget.FrameLayout
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import java.io.File

internal class Slideshow(root: FrameLayout) {
    private val prefs = root.context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
    private val imageView = root.findViewById<ImageView>(R.id.dreamImage)
    private val incoming = root.findViewById<ImageView>(R.id.incomingImage)
    private val noImagesLabel = root.findViewById<TextView>(R.id.noImagesLabel)
    private val handler = Handler(Looper.getMainLooper())
    private var images: List<File> = emptyList()
    private var currentIndex = 0

    private val slideRunnable = object : Runnable {
        override fun run() {
            showNextImage()
        }
    }

    fun start() {
        val brightness = SlideBrightness.read(prefs)
        SlideBrightness.applyTo(imageView, brightness)
        SlideBrightness.applyTo(incoming, brightness)
        loadImages()
        startSlideshow()
    }

    fun stop() {
        handler.removeCallbacks(slideRunnable)
        clearTransition()
        imageView.setImageDrawable(null)
    }
    private fun loadImages() {
        val exts = setOf("jpg", "jpeg", "png", "bmp", "webp", "gif")
        val result = mutableListOf<File>()

        val paths = getUSBStoragePaths()
        for (dir in paths) {
            dir.walkTopDown().maxDepth(5).forEach { file ->
                if (file.isFile && file.extension.lowercase() in exts) {
                    result.add(file)
                }
            }
        }

        images = result
        if (prefs.getBoolean(MainActivity.KEY_SHUFFLE, true)) {
            images = images.shuffled()
        }

        if (images.isEmpty()) {
            noImagesLabel.visibility = View.VISIBLE
            imageView.visibility = View.GONE
        } else {
            noImagesLabel.visibility = View.GONE
            imageView.visibility = View.VISIBLE
        }
    }

    private fun startSlideshow() {
        handler.removeCallbacks(slideRunnable)

        if (images.isEmpty()) return

        // Show first image immediately
        currentIndex = 0
        showImageBitmap(images[0])

        // Schedule subsequent slides
        scheduleNext()
    }

    private fun showNextImage() {
        if (images.isEmpty()) return

        currentIndex++
        if (currentIndex >= images.size) {
            // Loop: re-shuffle if enabled
            if (prefs.getBoolean(MainActivity.KEY_SHUFFLE, true)) {
                images = images.shuffled()
            }
            currentIndex = 0
        }

        showImageBitmap(images[currentIndex])
        scheduleNext()
    }

    private fun showImageBitmap(file: File) {
        try {
            // Decode with target size to avoid OOM on 4K TVs
            val opts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)

            val targetWidth = 1920
            val targetHeight = 1080
            var scaleFactor = 1
            while (opts.outWidth / scaleFactor > targetWidth * 1.5 ||
                   opts.outHeight / scaleFactor > targetHeight * 1.5) {
                scaleFactor *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = scaleFactor
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
            if (bitmap != null) {
                displayBitmap(bitmap)
            }
        } catch (e: Exception) {
            // Keep the current picture until the next scheduled slide.
        }
    }

    private fun displayBitmap(bitmap: Bitmap) {
        if (imageView.drawable == null || !MainActivity.crossfadeEnabled(prefs)) {
            imageView.setImageBitmap(bitmap)
            return
        }

        incoming.setImageBitmap(bitmap)
        incoming.alpha = 0f
        incoming.visibility = View.VISIBLE
        incoming.animate().alpha(1f).setDuration(800L).withEndAction {
            imageView.setImageBitmap(bitmap)
            incoming.visibility = View.GONE
            incoming.setImageDrawable(null)
        }.start()
    }

    private fun clearTransition() {
        incoming.animate().cancel()
        incoming.visibility = View.GONE
        incoming.setImageDrawable(null)
    }

    private fun scheduleNext() {
        val interval = MainActivity.intervalSeconds(prefs)
        handler.postDelayed(slideRunnable, interval * 1000L)
    }

    private fun getUSBStoragePaths(): List<File> {
        val candidates = mutableListOf<File>()

        arrayOf(
            "/storage/USB", "/storage/usb", "/storage/usb0", "/storage/usb1",
            "/storage/UsbDrive", "/storage/usbdrive",
            "/storage/0000-0000",
            "/mnt/usb", "/mnt/media_rw"
        ).forEach { path ->
            val file = File(path)
            if (file.isDirectory) candidates.add(file)
        }

        val storageDir = File("/storage")
        if (storageDir.isDirectory) {
            storageDir.listFiles()?.forEach { dir ->
                val name = dir.name
                if (!name.startsWith("emulated") && !name.equals("self", ignoreCase = true)) {
                    if (dir.isDirectory) candidates.add(dir)
                }
            }
        }

        return candidates.distinct()
    }
}




