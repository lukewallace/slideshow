package com.usbscreensaver

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.widget.ImageView

internal object SlideBrightness {
    const val STEP = 5
    private const val KEY = "slide_brightness_percent"

    fun read(prefs: SharedPreferences) = prefs.getInt(KEY, 100).coerceIn(0, 100)

    fun save(prefs: SharedPreferences, percent: Int) {
        prefs.edit().putInt(KEY, percent).apply()
    }

    fun applyTo(image: ImageView, percent: Int) {
        val channel = (percent * 255 + 50) / 100
        image.colorFilter = if (percent == 100) null
            else LightingColorFilter(Color.rgb(channel, channel, channel), 0)
    }
}
