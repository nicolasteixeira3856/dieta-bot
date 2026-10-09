package app.fibrai.android.feature.chat

import app.fibrai.android.core.network.ChatAction
import app.fibrai.android.core.network.ChatEstimate
import app.fibrai.android.core.network.ChatMemoryUpdate
import app.fibrai.android.core.network.ChatOut
import app.fibrai.android.core.network.ChatWorkout
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonNull
import org.junit.Test

/** A66 (ADR-050): typed actions as the rows the Chat stores. */
class ChatActionsTest {
    private val memory = listOf(ChatMemoryUpdate("add", null, "permanent", "preference", "leite", "Usa leite"))

    @Test
    fun aLegacyAnswerIsOneRow() {
        val out = ChatOut(reply = "Ok", intent = "log", workout = ChatWorkout(300, "replace"))
        val parts = ChatActions.parts(out)
        assertThat(parts.rows).containsExactly(out)
        assertThat(parts.workouts).containsExactly(ChatWorkout(300, "replace"))
        assertThat(parts.actions).isNull()
    }

    @Test
    fun logsAndPlansAreRowsInOrder_theFirstCarriesReplyMemoryAndSkips() {
        val out = ChatOut(
            reply = "Tudo anotado.",
            memoryUpdates = memory,
            actions = listOf(
                ChatAction("a1", "log", "1", ChatEstimate(kcal = 300.0), record = "auto"),
                ChatAction("a2", "plan", "4", ChatEstimate(kcal = 500.0), record = "none"),
                ChatAction("a3", "workout", workout = ChatWorkout(200, "add")),
                ChatAction("a4", "skip", "3", record = "auto"),
            ),
        )
        val parts = ChatActions.parts(out)
        assertThat(parts.rows.map { it.intent }).containsExactly("log", "plan").inOrder()
        assertThat(parts.rows[0].reply).isEqualTo("Tudo anotado.")
        assertThat(parts.rows[0].memoryUpdates).isEqualTo(memory)
        assertThat(parts.rows[0].skipSlots).containsExactly("3")
        assertThat(parts.rows[0].mealChange).isEqualTo(JsonNull)
        assertThat(parts.rows[1].reply).isEmpty()
        assertThat(parts.rows[1].memoryUpdates).isEmpty()
        assertThat(parts.rows[1].mealChange).isNull()
        assertThat(parts.workouts).containsExactly(ChatWorkout(200, "add"))
        assertThat(parts.actions).contains("\"a4\"")
    }

    @Test
    fun aHeldLogIsAQuestionRowWithItsSlot_skipsAloneAreASkipRow_atMostSix() {
        val held = ChatActions.parts(ChatOut(reply = "E o almoço?", actions = listOf(ChatAction("a1", "log", "2", null, question = "Quanto de arroz?", record = "none"))))
        assertThat(held.rows.single().question).isEqualTo("Quanto de arroz?")
        assertThat(held.rows.single().questionSlot).isEqualTo("2")
        val skips = ChatActions.parts(ChatOut(reply = "Ok.", actions = listOf(ChatAction("a1", "skip", "1", record = "auto"), ChatAction("a2", "skip", "3", record = "auto"))))
        assertThat(skips.rows.single().intent).isEqualTo("skip")
        assertThat(skips.rows.single().skipSlots).containsExactly("1", "3").inOrder()
        val many = ChatActions.parts(ChatOut(reply = "x", actions = (1..8).map { ChatAction("a$it", "log", "1", ChatEstimate(kcal = 1.0), record = "auto") }))
        assertThat(many.rows).hasSize(6)
    }
}
