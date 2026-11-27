package com.usbscreensaver

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Button
import android.widget.SeekBar
import android.view.View
import android.widget.TextView
import android.content.SharedPreferences
import kotlin.math.abs

class MainActivity : Activity() {
    private lateinit var panel: ScrollView
    private lateinit var content: LinearLayout
    private var opener: Button? = null
    private var updater: AppUpdate? = null
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

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
        panel = findViewById(R.id.submenuPanel)
        content = findViewById(R.id.submenuContent)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        findViewById<TextView>(R.id.designerCredit).text = getString(R.string.designer_credit, versionName)
        val updateButton = findViewById<Button>(R.id.checkUpdates)
        updateButton.setOnClickListener {
            openPanel(updateButton, R.string.check_updates)
            val check = AppUpdate(this)
            updater = check
            updatePrompt(getString(R.string.checking_updates))
            check.check()
        }
        findViewById<Button>(R.id.startSlideshow).setOnClickListener {
            closePanel()
            startActivity(android.content.Intent(this, SlideshowActivity::class.java))
        }

        applyMenuBrightness(SlideBrightness.read(prefs))
        findViewById<Button>(R.id.advancedSettings).setOnClickListener {
            openPanel(it as Button, R.string.advanced_settings)
            showAdvancedSettings(prefs)
        }

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

    private fun applyMenuBrightness(percent: Int) {
        SlideBrightness.applyTo(findViewById(R.id.menuPhoto), percent)
        // Keep the remote focus indicator and controls visible at low brightness.
        findViewById<View>(R.id.menuPanel).alpha = percent.coerceAtLeast(40) / 100f
        panel.alpha = findViewById<View>(R.id.menuPanel).alpha
    }

    private fun clearOptions() {
        content.removeViews(1, content.childCount - 1)
        panel.scrollTo(0, 0)
    }

    private fun openPanel(button: Button, title: Int) {
        closePanel()
        opener = button
        findViewById<ViewGroup>(R.id.menuPanel).descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        panel.visibility = View.VISIBLE
        findViewById<TextView>(R.id.submenuHeading).setText(title)
    }

    internal fun closePanel() {
        val button = opener ?: return
        opener = null
        updater?.dismiss()
        updater = null
        panel.visibility = View.GONE
        clearOptions()
        applyMenuBrightness(SlideBrightness.read(prefs))
        findViewById<ViewGroup>(R.id.menuPanel).descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        button.requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (opener != null && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (event.action == KeyEvent.ACTION_UP) closePanel()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        if (opener != null) closePanel() else super.onBackPressed()
    }

    override fun onDestroy() {
        updater?.dismiss()
        super.onDestroy()
    }

    private fun option(label: String, action: () -> Unit): Button {
        val button = layoutInflater.inflate(R.layout.submenu_button, content, false) as Button
        button.text = label
        button.setOnClickListener { action() }
        content.addView(button)
        return button
    }

    internal fun isChecking(check: AppUpdate) = updater === check && !isFinishing && !isDestroyed

    internal fun updatePrompt(text: String, actionLabel: String? = null, action: () -> Unit = {}) {
        clearOptions()
        content.addView(TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
        })
        val primary = actionLabel?.let { option(it, action) }
        val back = option(getString(android.R.string.cancel)) { closePanel() }
        (primary ?: back).requestFocus()
    }

    private fun showAdvancedSettings(prefs: SharedPreferences) {
        val view = layoutInflater.inflate(R.layout.advanced_settings, content, false)
        content.addView(view)
        val label = view.findViewById<TextView>(R.id.brightnessLabel)
        val slider = view.findViewById<SeekBar>(R.id.brightnessSlider)
        fun selectedPercent() = SlideBrightness.MIN + slider.progress * SlideBrightness.STEP
        fun updatePreview() {
            val percent = selectedPercent()
            label.text = getString(R.string.brightness_value, percent)
            applyMenuBrightness(percent)
        }
        slider.max = (100 - SlideBrightness.MIN) / SlideBrightness.STEP
        slider.keyProgressIncrement = 1
        slider.progress = (SlideBrightness.read(prefs) - SlideBrightness.MIN) / SlideBrightness.STEP
        updatePreview()
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) = updatePreview()
            override fun onStartTrackingTouch(bar: SeekBar) {}
            override fun onStopTrackingTouch(bar: SeekBar) {}
        })
        slider.isFocusable = false
        option("Decrease brightness") { slider.progress -= 1 }.requestFocus()
        option("Increase brightness") { slider.progress += 1 }
        option(getString(android.R.string.ok)) {
            SlideBrightness.save(prefs, selectedPercent())
            closePanel()
        }
        option(getString(android.R.string.cancel)) { closePanel() }
    }

    private fun bindChoice(buttonId: Int, title: Int, options: Int, initial: Int, save: (Int) -> Unit) {
        val button = findViewById<Button>(buttonId)
        val labels = resources.getStringArray(options)
        var selected = initial
        button.text = labels[selected]
        button.setOnClickListener {
            openPanel(button, title)
            labels.forEachIndexed { index, label ->
                val choice = option(if (index == selected) "✓  $label" else label) {
                    save(index)
                    selected = index
                    button.text = labels[index]
                    closePanel()
                }
                if (index == selected) choice.requestFocus()
            }
        }
    }
}

