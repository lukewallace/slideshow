package com.usbscreensaver

import android.service.dreams.DreamService
import android.view.WindowManager

class UsbDreamService : DreamService() {
    private var slideshow: Slideshow? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isFullscreen = true
        window?.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        setContentView(R.layout.dream_layout)
        slideshow = Slideshow(requireNotNull(findViewById(R.id.dreamRoot)))
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        slideshow?.start()
    }

    override fun onDreamingStopped() {
        slideshow?.stop()
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        slideshow?.stop()
        slideshow = null
        super.onDetachedFromWindow()
    }
}
