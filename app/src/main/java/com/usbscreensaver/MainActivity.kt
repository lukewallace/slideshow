package com.usbscreensaver

import android.app.Activity
import android.os.Bundle
import android.app.AlertDialog
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.content.SharedPreferences
import kotlin.math.abs

class MainActivity : Activity() {

    companion object {
        const val PREFS_NAME = "usb_screensaver_prefs"
        const val KEY_INTERVAL = "slide_interval"
        const val KEY_CROSSFADE = "crossfade_enabled"
        fun crossfadeEnabled(prefs: SharedPreferences) = prefs.getBoolean(KEY_CROSSFADE, true)
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
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        findViewById<TextView>(R.id.designerCredit).text = getString(R.string.designer_credit, versionName)
        val updateButton = findViewById<Button>(R.id.checkUpdates)
        updateButton.setOnClickListener { AppUpdate(this, updateButton).check() }
        findViewById<Button>(R.id.startSlideshow).setOnClickListener {
            startActivity(android.content.Intent(this, SlideshowActivity::class.java))
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        findViewById<Button>(R.id.advancedSettings).setOnClickListener { showAdvancedSettings(prefs) }

        bindChoice(R.id.intervalButton, R.string.interval_label, R.array.interval_options,
            intervals.indexOf(intervalSeconds(prefs))) { index ->
            prefs.edit().putInt(KEY_INTERVAL, intervals[index]).apply()
        }
        bindChoice(R.id.effectButton, R.string.effect_label, R.array.effect_options,
            if (crossfadeEnabled(prefs)) 1 else 0) { index ->
            prefs.edit().putBoolean(KEY_CROSSFADE, index == 1).apply()
        }
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            val permission = if (android.os.Build.VERSION.SDK_INT >= 33)
                android.Manifest.permission.READ_MEDIA_IMAGES else android.Manifest.permission.READ_EXTERNAL_STORAGE
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(permission), 1)
        }
    }

    private fun showAdvancedSettings(prefs: SharedPreferences) {
        val view = layoutInflater.inflate(R.layout.advanced_settings, null)
        val preview = view.findViewById<ImageView>(R.id.brightnessPreview)
        val label = view.findViewById<TextView>(R.id.brightnessLabel)
        val slider = view.findViewById<SeekBar>(R.id.brightnessSlider)
        fun updatePreview() {
            val percent = slider.progress * SlideBrightness.STEP
            label.text = getString(R.string.brightness_value, percent)
            SlideBrightness.applyTo(preview, percent)
        }
        slider.max = 100 / SlideBrightness.STEP
        slider.keyProgressIncrement = 1
        slider.progress = SlideBrightness.read(prefs) / SlideBrightness.STEP
        updatePreview()
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) = updatePreview()
            override fun onStartTrackingTouch(bar: SeekBar) {}
            override fun onStopTrackingTouch(bar: SeekBar) {}
        })
        AlertDialog.Builder(this)
            .setTitle(R.string.advanced_settings)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                SlideBrightness.save(prefs, slider.progress * SlideBrightness.STEP)
            }.show()
        slider.requestFocus()
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

