package app.fibrai.android.core.push

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class PushHandlerTest : PushTestBase() {
    private val notifications get() = shadowOf(context.getSystemService(NotificationManager::class.java))
    private lateinit var handler: PushHandler

    @Before
    fun setUpHandler() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        handler = PushHandler(context, repo, SlotAlarmScheduler(context, repo, clock))
    }

    @Test
    fun emptySlot_notifies_withCopyAndBothActions() = runBlocking<Unit> {
        assertThat(handler.onSlot(slotIds[3])).isTrue()
        val n = notifications.allNotifications.single()
        assertThat(n.extras.getString(NotificationCompat.EXTRA_TITLE)).isEqualTo("Jantar. Ainda não registrou.")
        assertThat(n.actions.map { it.title.toString() }).containsExactly("Registrar", "Pular").inOrder()
        assertThat(n.channelId).isEqualTo(PushHandler.CHANNEL)
        val open = shadowOf(n.actions[0].actionIntent).savedIntent
        assertThat(open.component?.className).isEqualTo(MainActivity::class.java.name)
        assertThat(open.getStringExtra(MainActivity.EXTRA_OPEN)).isEqualTo(MainActivity.OPEN_CHAT)
        assertThat(open.getLongExtra(MainActivity.EXTRA_SLOT, -1)).isEqualTo(slotIds[3])
    }

    @Test
    fun logInTheApp_clearsThatSlotsReminder() = runBlocking<Unit> {
        handler.onSlot(slotIds[3])
        handler.onSlot(slotIds[2])
        PushSync(context, repo, SlotAlarmScheduler(context, repo, clock)).start()
        repo.addLog("", "sopa", 350, 15, true, slotId = slotIds[3])
        withTimeout(5_000) {
            while (notifications.allNotifications.size != 1) delay(20)
        }
        assertThat(notifications.allNotifications.single().extras.getString(NotificationCompat.EXTRA_TITLE)).isEqualTo("Lanche. Ainda não registrou.")
    }

    @Test
    fun loggedSlot_doesNotNotify() = runBlocking<Unit> {
        repo.addLog("", "sopa", 350, 15, true, slotId = slotIds[3])
        assertThat(handler.onSlot(slotIds[3])).isFalse()
        assertThat(notifications.allNotifications).isEmpty()
    }

    @Test
    fun skippedSlot_doesNotNotify() = runBlocking<Unit> {
        repo.addSkip(slotIds[3])
        assertThat(handler.onSlot(slotIds[3])).isFalse()
        assertThat(notifications.allNotifications).isEmpty()
    }

    @Test
    fun pular_storesSkip_andClearsTheNotification() = runBlocking<Unit> {
        handler.onSlot(slotIds[3])
        handler.onSkip(slotIds[3])
        val day = repo.observeToday().first()
        assertThat(day.skippedSlotIds).containsExactly(slotIds[3])
        assertThat(day.logs).isEmpty()
        assertThat(notifications.allNotifications).isEmpty()
    }

    @Test
    fun withoutNotificationPermission_nothingPosted() = runBlocking<Unit> {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertThat(handler.onSlot(slotIds[3])).isFalse()
        assertThat(notifications.allNotifications).isEmpty()
    }

    @Test
    fun unknownSlot_ignored() = runBlocking<Unit> {
        assertThat(handler.onSlot(9_999)).isFalse()
    }
}
