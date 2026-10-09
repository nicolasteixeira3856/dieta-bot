package app.fibrai.android.feature.chat

import app.fibrai.android.core.network.ChatAction
import app.fibrai.android.core.network.ChatOut
import app.fibrai.android.core.network.ChatEstimate
import app.fibrai.android.core.network.ChatWorkout
import app.fibrai.android.core.network.ItemOut
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull

/**
 * A66 (ADR-050): an answer with typed actions as the rows the Chat already knows. Each `log` and `plan` action becomes one
 * answer row in the server's order, shaped like a single-estimate answer; the first row also carries the reply, the memory,
 * every `skip` action (as `skip_slots`) and the raw actions. With no log or plan, the first row is the answer alone (a
 * question, skips, a workout). Workouts are applied after the rows.
 */
data class ActionParts(val rows: List<ChatOut>, val workouts: List<ChatWorkout>, val actions: String?, val rowActions: List<String?> = listOf(actions)) {
    /** A67: what each row stores: the whole list on the first row, its own action on every later row. */
    fun actionsOf(row: Int): String? = rowActions.getOrNull(row)
}

/** A68 (S38 `recipe`): the cooking recipe of a plan, as the app saves it. */
@Serializable
data class PlanRecipe(
    val name: String = "",
    val ingredients: List<app.fibrai.android.domain.RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
)

/** A67 (ADR-051): one option of an open request, its estimate as the server totalled it. */
data class PlanOption(
    val id: String,
    val name: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    val items: List<ItemOut>,
    val mealText: String?,
)

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
        val own = estimates.drop(1).map { json.encodeToString(ListSerializer(ChatAction.serializer()), listOf(it)) }
        return ActionParts(listOf(first) + rows.drop(1).map { it.copy(model = out.model, skipSlots = null) }, workouts, raw, listOf(raw) + own)
    }

    /**
     * A67: the options of the plan a row stores (its own action: the first log or plan of its list), each with an id, a name
     * and an estimate; fewer than two = a plain plan.
     */
    fun options(actions: String?): List<PlanOption> {
        val own = actions?.let { runCatching { json.decodeFromString(ListSerializer(ChatAction.serializer()), it) }.getOrNull() }
            ?.firstOrNull { it.type == LOG || it.type == PLAN }?.takeIf { it.type == PLAN } ?: return emptyList()
        val options = own.options?.let { runCatching { json.decodeFromJsonElement(ListSerializer(RawOption.serializer()), it) }.getOrNull() }.orEmpty()
        return options.mapNotNull { o ->
            val e = o.estimate ?: return@mapNotNull null
            if (o.id.isBlank() || o.name.isBlank() || e.kcal <= 0.0) return@mapNotNull null
            PlanOption(o.id, o.name.trim(), e.kcal.roundToInt(), e.p.roundToInt(), e.c.roundToInt(), e.g.roundToInt(), e.items, e.mealText?.trim()?.takeIf { it.isNotEmpty() })
        }.distinctBy { it.id }.takeIf { it.size >= 2 }.orEmpty()
    }

    /** An option item as a line: `30 g de molho de tomate`; a name that starts with a count keeps it, `1 pão sírio (60 g)`. */
    fun itemLine(item: ItemOut): String {
        val grams = if (item.g % 1.0 == 0.0) item.g.toInt().toString() else item.g.toString().replace('.', ',')
        val name = item.name.trim()
        if (item.g <= 0.0) return name
        return if (name.firstOrNull()?.isDigit() == true) "$name ($grams g)" else "$grams g de $name"
    }

    /** A68 (S38): the cooking recipe a plan row carries (its own action's `recipe`); null when none or without ingredients. */
    fun recipe(actions: String?): PlanRecipe? {
        val own = actions?.let { runCatching { json.decodeFromString(ListSerializer(ChatAction.serializer()), it) }.getOrNull() }
            ?.firstOrNull { it.type == LOG || it.type == PLAN }?.takeIf { it.type == PLAN } ?: return null
        val raw = own.recipe?.let { runCatching { json.decodeFromJsonElement(PlanRecipe.serializer(), it) }.getOrNull() } ?: return null
        val ingredients = raw.ingredients.filter { it.name.isNotBlank() }
        if (raw.name.isBlank() || ingredients.isEmpty()) return null
        return raw.copy(name = raw.name.trim(), ingredients = ingredients, steps = raw.steps.filter { it.isNotBlank() })
    }

    /** A68 (S38): the saved recipe a log or plan row refers to (its own action's `recipe_id`), as the app's id. */
    fun recipeId(actions: String?): Long? {
        val own = actions?.let { runCatching { json.decodeFromString(ListSerializer(ChatAction.serializer()), it) }.getOrNull() }
            ?.firstOrNull { it.type == LOG || it.type == PLAN } ?: return null
        return PromptBuilder.recipeIdOf(own.recipeId)
    }

    @Serializable
    private data class RawOption(val id: String = "", val name: String = "", val estimate: ChatEstimate? = null)

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
