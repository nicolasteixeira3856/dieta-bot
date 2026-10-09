package app.fibrai.android.feature.chat

import app.fibrai.android.core.network.ChatAction
import app.fibrai.android.core.network.ChatOut
import app.fibrai.android.core.network.ChatWorkout
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull

/**
 * A66 (ADR-050): an answer with typed actions as the rows the Chat already knows. Each `log` and `plan` action becomes one
 * answer row in the server's order, shaped like a single-estimate answer; the first row also carries the reply, the memory,
 * every `skip` action (as `skip_slots`) and the raw actions. With no log or plan, the first row is the answer alone (a
 * question, skips, a workout). Workouts are applied after the rows.
 */
data class ActionParts(val rows: List<ChatOut>, val workouts: List<ChatWorkout>, val actions: String?)

object ChatActions {
    const val LOG = "log"
    const val PLAN = "plan"
    const val SKIP = "skip"
    const val WORKOUT = "workout"
    const val QUESTION = "question"

    /** Server bound (S36). */
    const val MAX = 6

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun parts(out: ChatOut): ActionParts {
        val actions = out.actions?.take(MAX) ?: return ActionParts(listOf(out), listOfNotNull(out.workout), null)
        val estimates = actions.filter { it.type == LOG || it.type == PLAN }
        val skips = actions.filter { it.type == SKIP }.mapNotNull { it.slot }.distinct()
        val workouts = actions.filter { it.type == WORKOUT }.mapNotNull { it.workout }
        val rows = estimates.map(::row).ifEmpty {
            val question = actions.firstOrNull { it.type == QUESTION }
            listOf(
                ChatOut(
                    intent = if (question == null && skips.isNotEmpty()) SKIP else QUESTION,
                    record = question?.record ?: actions.firstOrNull()?.record,
                    skipSlot = skips.firstOrNull().takeIf { question == null },
                    mealChange = JsonNull,
                ),
            )
        }
        val first = rows.first().copy(
            reply = out.reply,
            memoryUpdates = out.memoryUpdates,
            memoryUsed = out.memoryUsed,
            digest = out.digest,
            model = out.model,
            skipSlots = skips,
        )
        val raw = json.encodeToString(ListSerializer(ChatAction.serializer()), actions)
        return ActionParts(listOf(first) + rows.drop(1).map { it.copy(model = out.model, skipSlots = null) }, workouts, raw)
    }

    /** One log or plan action as a single-estimate answer: a held log is a question-only row with its slot. */
    private fun row(a: ChatAction): ChatOut {
        val held = a.type == LOG && a.estimate == null && !a.question.isNullOrBlank()
        return ChatOut(
            intent = a.type,
            estimate = a.estimate,
            question = a.question?.takeIf { held },
            record = a.record,
            questionSlot = a.slot.takeIf { held },
            // A capable server: a log without a change is an explicit "no change" (MealChanges), never the legacy path.
            mealChange = if (a.type == LOG) a.mealChange ?: JsonNull else null,
            planBudget = a.planBudget.takeIf { a.type == PLAN },
        )
    }
}
