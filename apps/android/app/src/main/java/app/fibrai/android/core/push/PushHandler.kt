package app.fibrai.android.core.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import app.fibrai.android.MainActivity
import app.fibrai.android.R
import app.fibrai.android.core.database.slotsOfDay
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.designsystem.aero.AeroDarkColors
import app.fibrai.android.core.designsystem.aero.AeroLightColors
import app.fibrai.android.core.telemetry.NoopTelemetry
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.PushPlan
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** What the receiver does. Kept apart so it runs in JVM tests. */
@Singleton
class PushHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DayRepository,
    private val scheduler: SlotAlarmScheduler,
    private val closures: app.fibrai.android.core.closure.ClosureRunner,
    private val telemetry: Telemetry = NoopTelemetry,
) {
    /** A60 part B: the closure alarm fired: what is due now is closed, then the next alarm is set. */
    suspend fun onClosure() {
        runCatching { closures.runDue() }
        scheduler.resync()
    }

    /** Slot alarm fired: notify only when the slot is still empty (spec rule 2). */
    suspend fun onSlot(slotId: Long): Boolean {
        val day = repository.observeToday().first()
        val slot = day.slotsOfDay.firstOrNull { it.id == slotId } ?: return false
        val logged = day.logs.mapNotNull { it.slotId }.toSet()
        if (!day.onboardingDone || !day.notificationsEnabled || !PushPlan.shouldNotify(slotId, logged, day.skippedSlotIds)) return false
        val shown = notify(slotId, slot.name)
        if (shown) telemetry.event(TelemetryEvents.PUSH_ACTION, mapOf("action" to "shown"))
        return shown
    }

    /** "Pular" on the notification: skip status, notification and alarm gone (spec rule 4). */
    suspend fun onSkip(slotId: Long) {
        if (repository.observeToday().first().slotsOfDay.none { it.id == slotId }) return
        repository.addSkip(slotId)
        NotificationManagerCompat.from(context).cancel(notificationId(slotId))
        scheduler.cancel(slotId)
        telemetry.event(TelemetryEvents.PUSH_ACTION, mapOf("action" to "skip"))
        telemetry.event(TelemetryEvents.MEAL_SKIPPED, mapOf("from" to "push"))
    }

    suspend fun onResync() {
        scheduler.resync()
    }

    private fun notify(slotId: Long, name: String): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            slotId.toInt(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_CHAT)
                .putExtra(MainActivity.EXTRA_SLOT, slotId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val skip = PendingIntent.getBroadcast(
            context,
            slotId.toInt(),
            Intent(context, PushReceiver::class.java).setAction(PushReceiver.ACTION_SKIP).putExtra(PushReceiver.EXTRA_SLOT, slotId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent())
            .setContentTitle(PushPlan.copy(name))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Registrar", open)
            .addAction(0, "Pular", skip)
            .build()
        @Suppress("MissingPermission") // checked above
        manager.notify(notificationId(slotId), notification)
        return true
    }

    /** Notification/Push accent: accent/default of the system theme (the template tints the small icon and actions). */
    private fun accent(): Int {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return (if (night) AeroDarkColors else AeroLightColors).accentDefault.toArgb()
    }

    companion object {
        const val CHANNEL = "slot_reminders"

        /** Registrar opens the app, a log or a skip closes the slot: its reminder goes away. */
        fun clear(context: Context, slotId: Long) {
            NotificationManagerCompat.from(context).cancel(notificationId(slotId))
        }

        fun notificationId(slotId: Long): Int = NOTIFICATION_BASE + slotId.toInt()

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Lembretes de refeição", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "No horário de cada refeição ainda sem registro."
                },
            )
        }

        private const val NOTIFICATION_BASE = 1000
    }
}
