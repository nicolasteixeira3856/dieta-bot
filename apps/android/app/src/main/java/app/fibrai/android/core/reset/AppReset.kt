package app.fibrai.android.core.reset

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.photo.PhotoStore
import app.fibrai.android.core.push.SlotAlarmScheduler
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** The reset steps in the order [AppReset] runs them. Each one is idempotent. */
interface ResetSteps {
    /** onboardingDone = 0 first: a reset cut short still opens the onboarding. */
    suspend fun markOnboardingPending()

    /** Slot alarms, the resync, shown notifications and the push preferences. */
    suspend fun clearPush()

    /** memory.bin (+ .new, legacy memory.txt) and the Chat photos. */
    suspend fun deleteFiles()

    /** Every Room table. */
    suspend fun clearDatabase()

    /** Something a reset erases is still on the device. */
    suspend fun leftover(): Boolean
}

enum class ResetStep { MARK, PUSH, FILES, DATABASE }

/**
 * App reset (ADR-040): erases everything the app keeps for the user on this device, like a
 * reinstall, then the onboarding starts over. The installation id (noBackupFilesDir, ADR-025) stays.
 */
@Singleton
class AppReset @Inject constructor(
    private val steps: ResetSteps,
    private val telemetry: Telemetry,
) {
    private val mutex = Mutex()

    /** Config: true when every step ran. */
    suspend fun run(): Boolean = mutex.withLock { runSteps() }

    /** Cold start with onboardingDone = 0: finishes a reset cut short. A fresh install has nothing left. */
    suspend fun finishInterrupted(): Boolean = mutex.withLock { if (steps.leftover()) runSteps() else true }

    private suspend fun runSteps(): Boolean {
        var step = ResetStep.MARK
        return try {
            steps.markOnboardingPending()
            step = ResetStep.PUSH
            steps.clearPush()
            step = ResetStep.FILES
            steps.deleteFiles()
            step = ResetStep.DATABASE
            steps.clearDatabase()
            telemetry.event(TelemetryEvents.APP_RESET, mapOf("outcome" to "done"))
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            telemetry.event(TelemetryEvents.APP_RESET, mapOf("outcome" to "failed", "step" to step.name.lowercase()))
            telemetry.nonFatal(e)
            false
        }
    }
}

class DeviceResetSteps @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DayRepository,
    private val scheduler: SlotAlarmScheduler,
) : ResetSteps {
    private val files: List<File>
        get() = listOf(MEMORY, "$MEMORY.new", LEGACY_MEMORY).map { File(context.filesDir, it) }

    private val photos: File get() = File(context.filesDir, PhotoStore.DIR)

    override suspend fun markOnboardingPending() = repository.markOnboardingPending()

    override suspend fun clearPush() {
        scheduler.cancelAll()
        NotificationManagerCompat.from(context).cancelAll()
    }

    override suspend fun deleteFiles() = withContext(Dispatchers.IO) {
        files.forEach { check(!it.exists() || it.delete()) { "file not deleted" } }
        check(!photos.exists() || photos.deleteRecursively()) { "photos not deleted" }
    }

    override suspend fun clearDatabase() = repository.clearAll()

    /** A71 (ADR-057 decision 7): saved onboarding answers are an onboarding in progress, never a reset cut short. */
    override suspend fun leftover(): Boolean = !repository.hasOnboardingAnswers() &&
        (withContext(Dispatchers.IO) { files.any { it.exists() } || photos.list().orEmpty().isNotEmpty() } || repository.hasRows())

    private companion object {
        const val MEMORY = "memory.bin"
        const val LEGACY_MEMORY = "memory.txt"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ResetModule {
    @Binds
    abstract fun resetSteps(impl: DeviceResetSteps): ResetSteps
}
