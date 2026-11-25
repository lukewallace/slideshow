package com.usbscreensaver

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class SlideshowActivity : Activity() {
    private lateinit var slideshow: Slideshow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setContentView(R.layout.dream_layout)
        slideshow = Slideshow(findViewById(R.id.dreamRoot))
    }

    override fun onStart() {
        super.onStart()
        slideshow.start()
    }

    override fun onStop() {
        slideshow.stop()
        super.onStop()
    }
}


