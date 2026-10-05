package app.fibrai.android.core.push

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Slot alarm, 00:05 resync, "Pular" action, boot/time/permission changes. */
@AndroidEntryPoint
class PushReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: PushHandler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_SLOT -> handler.onSlot(intent.getLongExtra(EXTRA_SLOT, -1))
                    ACTION_SKIP -> handler.onSkip(intent.getLongExtra(EXTRA_SLOT, -1))
                    ACTION_RESYNC,
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
                    -> handler.onResync()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SLOT = "app.fibrai.android.push.SLOT"
        const val ACTION_SKIP = "app.fibrai.android.push.SKIP"
        const val ACTION_RESYNC = "app.fibrai.android.push.RESYNC"
        const val EXTRA_SLOT = "slot_id"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
