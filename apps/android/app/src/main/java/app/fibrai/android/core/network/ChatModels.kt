package app.fibrai.android.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Mirrors server/main.py ChatIn (POST /v1/chat). */
@Serializable
data class ChatIn(
    @SerialName("local_time") val localTime: String,
    val profile: ChatProfile,
    val memory: String = "",
    val day: ChatDay,
    val digests: List<String> = emptyList(),
    val messages: List<ChatTurn> = emptyList(),
    val text: String,
    @SerialName("image_b64") val imageB64: String? = null,
    val compact: Boolean = false,
    /** Meals of the 7 days before today (S11). */
    val recent: List<ChatRecentMeal> = emptyList(),
    /**
     * A67 (S37, ADR-051): the first Chat turns with an empty memory (the greeting and the answer to the questions). Default
     * false is not encoded: the server reads an absent field as false.
     */
    val discovery: Boolean = false,
    /** A68 (S38): the index of saved recipes, most recent first, at most 30; empty = not sent. */
    val recipes: List<ChatRecipe> = emptyList(),
    /** A68 (S38): the saved recipe the message names, complete, only on that turn; null = none. */
    @SerialName("recipe_full") val recipeFull: ChatRecipeFull? = null,
    /** A64 (S33): the app's totals of each of the 7 days before today, newest first; empty = not sent. */
    @SerialName("recent_days") val recentDays: List<ChatRecentDay> = emptyList(),
    /**
     * Memory v2 (A28). Present, even empty, = v2 client: the server reads these instead of [memory]
     * and answers memory_updates / memory_used. Null only to talk as a legacy client.
     */
    val facts: List<ChatFact>? = null,
    /**
     * Question rounds already shown for the pending meal, 0-3 (A30, ADR-026). Always sent: the server
     * may answer a question only and releases the estimate after 3. No default on purpose: Json does not
     * encode defaults, and an absent field makes the server treat the app as a legacy client.
     */
    @SerialName("clarify_rounds") val clarifyRounds: Int,
    /** Forçar estimativa: the server releases the estimate now. */
    @SerialName("force_estimate") val forceEstimate: Boolean = false,
    /**
     * The client records by itself (A34, ADR-028): the server answers [ChatOut.record]. No default, like
     * [clarifyRounds]: Json does not encode defaults and the field must always go.
     */
    @SerialName("auto_record") val autoRecord: Boolean,
    /**
     * The client stores temp facts (A38, ADR-029): v5 client, the server may propose `kind: temp` and answers
     * [ChatOut.questionSlot]. No default, like [autoRecord]: the field must always go.
     */
    @SerialName("temp_facts") val tempFacts: Boolean,
    /**
     * The client applies meal additions and revisions (A47, ADR-032): the server answers [ChatOut.mealChange].
     * No default, like [autoRecord]: the field must always go.
     */
    @SerialName("meal_changes") val mealChanges: Boolean,
    /**
     * The client applies skips listed next to any intent (A59, ADR-047): the server answers [ChatOut.skipSlots]. No
     * default, like [mealChanges]: the field must always go; a compact request sends false.
     */
    @SerialName("skip_slots") val skipSlots: Boolean,
    /**
     * The client shows the over-budget choice (A60 part A, ADR-039): the server answers [ChatOut.planBudget]. No default,
     * like [skipSlots]: the field must always go; a compact request sends false.
     */
    @SerialName("plan_budget") val planBudget: Boolean,
    /**
     * The client records a workout reported in the Chat (A65, ADR-049): the server answers [ChatOut.workout]. No default,
     * like [planBudget]: the field must always go; a compact request sends false.
     */
    val workout: Boolean,
    /**
     * The client applies typed actions (A66, ADR-050): the server answers [ChatOut.actions] and the legacy fields are
     * ignored. No default, like [workout]: always encoded; a compact request sends false.
     */
    val actions: Boolean,
    /** Ajustar para caber: the target of the adjusted plan, the `limit_kcal` the choice showed. Null = none. */
    @SerialName("fit_kcal") val fitKcal: Int? = null,
    /** The unrecorded addition the user is continuing (A47); null = none. Never on a compact request. */
    @SerialName("pending_addition") val pendingAddition: ChatPendingAddition? = null,
    /** A72 (S42, ADR-058): a log may target an extra (`slot: "extra"`, `time`). False is not encoded (compact). */
    val extras: Boolean = false,
    /** A72 (S42): a log of a named past day inside 30 days comes back with `day`. False is not encoded (compact). */
    @SerialName("other_day") val otherDay: Boolean = false,
    /** A72 (S42): the app's first day; a named day before it is too old. Null = not sent. */
    @SerialName("first_day") val firstDay: String? = null,
)

/** An unrecorded addition sent back as context (S18 `pending_addition`): base_slot is the eaten source slot or null. */
@Serializable
data class ChatPendingAddition(
    @SerialName("base_slot") val baseSlot: String?,
    val addition: ChatAddition,
)

/** Only the newly eaten food (S18 `addition`), rounded once by the server. */
@Serializable
data class ChatAddition(
    @SerialName("meal_text") val mealText: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    val items: List<ChatAdditionItem>,
)

@Serializable
data class ChatAdditionItem(val name: String, val g: Double, val kcal: Int)

/** A memory fact as the server sees it (api-contract). slot is a profile slot id or null. kind "temp" = a T id (A38). */
@Serializable
data class ChatFact(
    val id: String,
    val kind: String,
    val category: String,
    val key: String,
    val text: String,
    val slot: String?,
    @SerialName("days_seen") val daysSeen: Int,
    @SerialName("last_seen") val lastSeen: String?,
    /** A64 (S33): the numbers of a routine, all four or none; null = not encoded. */
    val kcal: Int? = null,
    val p: Int? = null,
    val c: Int? = null,
    val g: Int? = null,
)

/** A68 (S38 `recipes`): `id` R{n}, name ≤ 60, totals of the current version, ≤ 3 key foods. */
@Serializable
data class ChatRecipe(
    val id: String,
    val name: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    @SerialName("key_foods") val keyFoods: List<String>,
)

/** A68 (S38 `recipe_full`): a [ChatRecipe] with its ingredients (1–30, g > 0) and steps (≤ 10, ≤ 300 characters). */
@Serializable
data class ChatRecipeFull(
    val id: String,
    val name: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    @SerialName("key_foods") val keyFoods: List<String>,
    val ingredients: List<ChatAdditionItem>,
    val steps: List<String>,
)

/**
 * A64 (S33 `recent_days`): one past day from Room. [ceilingKcal] is that day's effective ceiling with its own workout
 * credit; [overSlot] the profile slot that went furthest over its share of the ceiling; [missingSlots] the profile slots
 * with nothing recorded and not skipped. Both use today's profile slot ids only (the server refuses any other).
 */
@Serializable
data class ChatRecentDay(
    val date: String,
    val recorded: Boolean,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    @SerialName("ceiling_kcal") val ceilingKcal: Int,
    @SerialName("over_slot") val overSlot: String?,
    @SerialName("missing_slots") val missingSlots: List<String>,
)

/**
 * op: "add" | "reinforce" | "replace" | "remove". id is null only on add. A67 (S33, S37): kcal/p/c/g of a routine or a liked
 * dish when the model estimated them; [declared] = a routine the user declared without a recorded day (discovery).
 */
@Serializable
data class ChatMemoryUpdate(
    val op: String,
    val id: String? = null,
    val kind: String,
    val category: String,
    val key: String = "",
    val text: String = "",
    val slot: String? = null,
    val kcal: Int? = null,
    val p: Int? = null,
    val c: Int? = null,
    val g: Int? = null,
    val declared: Boolean = false,
)

/** slotId null = "Outros" (orphan or deleted slot). */
@Serializable
data class ChatRecentMeal(
    val date: String,
    @SerialName("slot_id") val slotId: String?,
    @SerialName("slot_name") val slotName: String,
    val text: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    /** A72 (S42): the time of an extra (`slot_id: "extra"`); null otherwise (not encoded). */
    val time: String? = null,
)

@Serializable
data class ChatProfile(
    @SerialName("ceiling_kcal") val ceilingKcal: Int,
    @SerialName("p_target") val pTarget: Int,
    @SerialName("c_target") val cTarget: Int,
    @SerialName("g_target") val gTarget: Int,
    @SerialName("eat_back") val eatBack: String,
    val slots: List<ChatSlot> = emptyList(),
    /** A60 part B (ADR-044): "seco" | "duro", on every normal and compact request. No default: always encoded. */
    val tone: String,
    /** A71 (S41, ADR-057 decision 9): the accepted goal weight and date; null = none (not encoded). */
    val goal: ChatGoal? = null,
    /** A72 (S42): the slots of each profile group by ISO weekday, for a past day's log; empty = not sent. */
    @SerialName("slots_by_day") val slotsByDay: List<ChatSlotsByDay> = emptyList(),
)

/** A72 (S42): one profile group: its ISO weekdays (1 = Monday) and its slots. */
@Serializable
data class ChatSlotsByDay(val weekdays: List<Int>, val slots: List<ChatSlot>)

@Serializable
data class ChatSlot(val id: String, val name: String, val time: String)

@Serializable
data class ChatDay(
    val date: String,
    @SerialName("eaten_kcal") val eatenKcal: Int = 0,
    @SerialName("eaten_p") val eatenP: Int = 0,
    @SerialName("eaten_c") val eatenC: Int = 0,
    @SerialName("eaten_g") val eatenG: Int = 0,
    @SerialName("workout_kcal") val workoutKcal: Int? = null,
    /** Effective ceiling - eaten; may be negative (S11). */
    @SerialName("remaining_kcal") val remainingKcal: Int? = null,
    val slots: List<ChatDaySlot> = emptyList(),
)

/** status: "empty" | "eaten" | "skipped" | "planned" (A60 part D: the reserved plan's text and numbers). */
@Serializable
data class ChatDaySlot(
    val id: String,
    val status: String,
    val text: String? = null,
    val kcal: Int? = null,
    val p: Int? = null,
    val c: Int? = null,
    val g: Int? = null,
)

/** role: "user" | "assistant". */
@Serializable
data class ChatTurn(val role: String, val text: String)

@Serializable
data class ChatEstimate(
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val c: Double = 0.0,
    val g: Double = 0.0,
    val confidence: String = "low",
    val question: String? = null,
    val items: List<ItemOut> = emptyList(),
    @SerialName("suggested_slot") val suggestedSlot: String? = null,
    /** The whole meal as corrected by the conversation (S11). The app records this text. */
    @SerialName("meal_text") val mealText: String? = null,
)

@Serializable
data class ChatOut(
    val reply: String = "",
    /** "log" | "plan" | "question" | "skip" (S14); null = server before S11, handled as today. */
    val intent: String? = null,
    val estimate: ChatEstimate? = null,
    /** At most 5 proposals; the app applies them with MemoryRules (A28). */
    @SerialName("memory_updates") val memoryUpdates: List<ChatMemoryUpdate> = emptyList(),
    /** Fact ids the reply relied on. */
    @SerialName("memory_used") val memoryUsed: List<String> = emptyList(),
    val digest: String? = null,
    val model: String = "",
    /** Question before the estimate (A30): non-blank with estimate null = a question-only turn. */
    val question: String? = null,
    /** "auto" | "ask" | "none" (S14, A34). Null = server before S14. */
    val record: String? = null,
    /** Profile slot id of a skip by text (intent "skip"), else null. */
    @SerialName("skip_slot") val skipSlot: String? = null,
    /** Slot the server held with a question-only turn (S16, v5 client); null otherwise. */
    @SerialName("question_slot") val questionSlot: String? = null,
    /**
     * A47: kept raw and parsed by [app.fibrai.android.domain.MealChanges], so malformed metadata never fails the
     * whole answer. Kotlin null = absent (a server without the capability: legacy flow); [kotlinx.serialization.json.JsonNull]
     * = the capable server sent no change.
     */
    @SerialName("meal_change") val mealChange: JsonElement? = null,
    /**
     * A59: profile slot ids the message says did not happen today, next to any intent, in profile order. Null = a server
     * without the capability: the single [skipSlot] path.
     */
    @SerialName("skip_slots") val skipSlots: List<String>? = null,
    /**
     * A60 part A: kept raw and parsed by [app.fibrai.android.domain.PlanBudget.parse], so a malformed object never fails the
     * answer. Null or absent = no choice UI.
     */
    @SerialName("plan_budget") val planBudget: JsonElement? = null,
    /** A65 (S35): the energy of a workout done today, as the user stated it; null = none. */
    val workout: ChatWorkout? = null,
    /** A66 (S36): the ordered actions of the answer (1–6); null = a server without the capability (legacy fields). */
    val actions: List<ChatAction>? = null,
)

/**
 * One typed action of an answer (S36, ADR-050). [type]: log | plan | skip | workout | recipe_recall | question. Per action:
 * [estimate] (log and plan; null when held), [question] (the held question of that action), [record] (v4 rules per action),
 * [mealChange] (log only, kept raw like [ChatOut.mealChange]), [workout], [planBudget] (plan only), [slot]. [options]
 * (S37) and [recipe] (S38) stay raw for A67 and A68.
 */
@Serializable
data class ChatAction(
    val id: String = "",
    val type: String = "",
    val slot: String? = null,
    val estimate: ChatEstimate? = null,
    val question: String? = null,
    val record: String? = null,
    @SerialName("record_intent") val recordIntent: String? = null,
    @SerialName("meal_day") val mealDay: String? = null,
    @SerialName("meal_change") val mealChange: JsonElement? = null,
    val workout: ChatWorkout? = null,
    @SerialName("recipe_id") val recipeId: String? = null,
    val options: JsonElement? = null,
    @SerialName("plan_budget") val planBudget: JsonElement? = null,
    val recipe: JsonElement? = null,
    /** A72 (S42): the `HH:mm` of an extra log (null: now). */
    val time: String? = null,
    /** A72 (S42): the ISO date of a past-day log (`meal_day: other`); null otherwise. */
    val day: String? = null,
)

/** `mode`: "replace" (the day's workout becomes [kcal]) or "add" (it sums to the day's number). */
@Serializable
data class ChatWorkout(val kcal: Int = 0, val mode: String = "replace")

/** Mirrors server/main.py CloseIn (POST /v1/close, A60 part B): the app's numbers of a day or a week. */
@Serializable
data class CloseIn(
    /** "day" | "week". */
    val period: String,
    val tone: String,
    @SerialName("local_time") val localTime: String,
    val profile: CloseProfile,
    /** [app.fibrai.android.domain.ClosureDay] or [app.fibrai.android.domain.ClosureWeek], as the period says. */
    val numbers: JsonElement,
)

@Serializable
data class CloseProfile(
    @SerialName("ceiling_kcal") val ceilingKcal: Int,
    @SerialName("p_target") val pTarget: Int,
    @SerialName("c_target") val cTarget: Int,
    @SerialName("g_target") val gTarget: Int,
    val slots: List<ChatSlot>,
    /** A71 (S41): the accepted goal; null = none (not encoded). */
    val goal: ChatGoal? = null,
)

/** The closure text (pt-BR, at most 3 lines); the server's neutral line on its own failures. */
@Serializable
data class CloseOut(val text: String = "", val model: String = "")
