package com.usbscreensaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives USB mount/unmount events.
 * Since DreamService can't be directly notified, this sends a local broadcast
 * that the dream picks up on its next cycle, or the next time the dream starts.
 * (DreamService instances are short-lived; on next dream start it re-scans.)
 */
class UsbMountReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Nothing to do here immediately — the dream service re-scans on start.
        // A real implementation could use a local broadcast or SharedPreferences
        // flag to tell an already-running dream to refresh, but Android's dream
        // lifecycle makes this non-trivial. The simplest robust approach:
        // next time the dream starts, it re-enumerates storage.
    }
}