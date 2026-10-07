package app.fibrai.android.core.closure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.fibrai.android.domain.Closures
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * dev only (A60 part B, ADR-019): runs a closure now, as the 22:00 alarm would, so QA and the testers exercise the path
 * without waiting for the clock. `adb shell am broadcast -a app.fibrai.android.dev.RUN_CLOSURE --es period day|week
 * -n app.fibrai.android.dev/app.fibrai.android.core.closure.RunClosureReceiver`. Not a screen; it deletes nothing.
 */
@AndroidEntryPoint
class RunClosureReceiver : BroadcastReceiver() {
    @Inject lateinit var runner: ClosureRunner

    override fun onReceive(context: Context, intent: Intent) {
        val period = intent.getStringExtra(EXTRA_PERIOD).takeIf { it == Closures.WEEK } ?: Closures.DAY
        val pending = goAsync()
        scope.launch {
            try {
                runCatching { runner.run(period) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PERIOD = "period"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
