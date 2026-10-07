package app.fibrai.android

import android.app.Application
import app.fibrai.android.core.closure.ClosureRunner
import app.fibrai.android.core.push.PushSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class FibraiApplication : Application() {
    @Inject lateinit var pushSync: PushSync
    @Inject lateinit var closures: ClosureRunner

    override fun onCreate() {
        super.onCreate()
        // A7: slot alarms follow slots, logs and skips while the process lives.
        pushSync.start()
        // A60 part B: a closure left without its text for lack of network asks once more at the next start.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { runCatching { closures.retryOffline() } }
    }
}
