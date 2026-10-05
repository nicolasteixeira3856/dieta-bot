package app.fibrai.android

import android.app.Application
import app.fibrai.android.core.push.PushSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FibraiApplication : Application() {
    @Inject lateinit var pushSync: PushSync

    override fun onCreate() {
        super.onCreate()
        // A7: slot alarms follow slots, logs and skips while the process lives.
        pushSync.start()
    }
}
