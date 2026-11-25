package com.usbscreensaver

import android.app.Activity
import android.os.Bundle
import android.app.AlertDialog
import android.widget.Button
import android.content.SharedPreferences
import kotlin.math.abs

class MainActivity : Activity() {

    companion object {
        const val PREFS_NAME = "usb_screensaver_prefs"
        const val KEY_INTERVAL = "slide_interval"
        const val KEY_CROSSFADE = "crossfade_enabled"
        const val KEY_SHUFFLE = "shuffle_enabled"
        private val intervals = listOf(10, 30, 60, 120, 180, 300, 600)

        fun intervalSeconds(prefs: SharedPreferences): Int {
            val saved = prefs.getInt(KEY_INTERVAL, 10)
            return intervals.minBy { abs(it.toLong() - saved) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val updateButton = findViewById<Button>(R.id.checkUpdates)
        updateButton.setOnClickListener { AppUpdate(this, updateButton).check() }
        findViewById<Button>(R.id.startSlideshow).setOnClickListener {
            startActivity(android.content.Intent(this, SlideshowActivity::class.java))
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        bindChoice(R.id.intervalButton, R.string.interval_label, R.array.interval_options,
            intervals.indexOf(intervalSeconds(prefs))) { index ->
            prefs.edit().putInt(KEY_INTERVAL, intervals[index]).apply()
        }
        bindChoice(R.id.effectButton, R.string.effect_label, R.array.effect_options,
            if (prefs.getBoolean(KEY_CROSSFADE, false)) 1 else 0) { index ->
            prefs.edit().putBoolean(KEY_CROSSFADE, index == 1).apply()
        }
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            val permission = if (android.os.Build.VERSION.SDK_INT >= 33)
                android.Manifest.permission.READ_MEDIA_IMAGES else android.Manifest.permission.READ_EXTERNAL_STORAGE
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(permission), 1)
        }
    }

    private fun bindChoice(buttonId: Int, title: Int, options: Int, initial: Int, save: (Int) -> Unit) {
        val button = findViewById<Button>(buttonId)
        val labels = resources.getStringArray(options)
        var selected = initial
        button.text = labels[selected]
        button.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(labels, selected) { dialog, index ->
                    save(index)
                    selected = index
                    button.text = labels[index]
                    dialog.dismiss()
                }
                .show()
        }
    }
}

