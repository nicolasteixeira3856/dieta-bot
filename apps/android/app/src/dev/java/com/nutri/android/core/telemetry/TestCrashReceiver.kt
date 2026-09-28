package com.nutri.android.core.telemetry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** dev only (A11): forces a crash so Crashlytics can be checked end to end. Sent from adb. */
class TestCrashReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        throw RuntimeException("crashlytics test")
    }
}
