package app.fibrai.android.core.push

import android.content.Context
import app.fibrai.android.core.database.slotsOfDay
import app.fibrai.android.core.database.DayRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * While the app runs: any change of slots (Config saveSlots, onboarding), logs or skips resyncs the
 * alarms. The first value resyncs too (app start). Midnight and boot come through [PushReceiver].
 */
@Singleton
class PushSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DayRepository,
    private val scheduler: SlotAlarmScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            repository.observeToday()
                .map { day ->
                    Triple(
                        (day.onboardingDone to day.date) to day.slotsOfDay,
                        day.logs.mapNotNull { it.slotId }.toSet(),
                        day.skippedSlotIds,
                    )
                }
                .distinctUntilChanged()
                .collect { (_, logged, skipped) ->
                    runCatching { scheduler.resync() }
                    // A slot logged or skipped in the app keeps no reminder on screen.
                    (logged + skipped).forEach { PushHandler.clear(context, it) }
                }
        }
    }
}
