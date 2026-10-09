package app.fibrai.android.domain

import java.text.Normalizer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** A68 (ADR-052): one ingredient of a saved recipe version, as the server sent it in the plan's `recipe`. */
@Serializable
data class RecipeIngredient(val name: String = "", val g: Double = 0.0, val kcal: Double = 0.0)

/** A68: what the matcher and the prompt need of a saved recipe: its id, name and current version's ingredients. */
data class RecipeRef(val id: Long, val name: String, val ingredients: List<RecipeIngredient>)

/**
 * A68 (ADR-052 § 3): the saved recipe a message names, by its normalized name, or by a key food together with the word
 * "receita". Pure: no Room, no network.
 */
object Recipes {
    const val INDEX_MAX = 30
    const val NAME_MAX = 60
    const val KEY_FOODS = 3
    const val KEY_FOOD_MAX = 40
    const val INGREDIENTS_MAX = 30
    const val STEPS_MAX = 10
    const val STEP_MAX = 300

    private val json = Json { ignoreUnknownKeys = true }
    private val INGREDIENTS = ListSerializer(RecipeIngredient.serializer())
    private val STEPS = ListSerializer(String.serializer())
    private val STOP = setOf("receita", "com", "sem", "para", "pra", "das", "dos", "uma", "de", "do", "da", "e", "o", "a")

    fun encodeIngredients(items: List<RecipeIngredient>) = json.encodeToString(INGREDIENTS, items)
    fun decodeIngredients(text: String?): List<RecipeIngredient> = text?.let { runCatching { json.decodeFromString(INGREDIENTS, it) }.getOrNull() }.orEmpty()
    fun encodeSteps(steps: List<String>) = json.encodeToString(STEPS, steps)
    fun decodeSteps(text: String?): List<String> = text?.let { runCatching { json.decodeFromString(STEPS, it) }.getOrNull() }.orEmpty()

    /** The three ingredients with the most energy, as the index's `key_foods`. */
    fun keyFoods(ingredients: List<RecipeIngredient>): List<String> = ingredients
        .filter { it.name.isNotBlank() }
        .sortedByDescending { it.kcal }
        .take(KEY_FOODS)
        .map { clip(it.name.trim(), KEY_FOOD_MAX) }

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9 ]+"), " ").replace(Regex("\\s+"), " ").trim()

    /**
     * The recipe [text] names: its whole normalized name; else every meaningful word of the name (two or more); else, with
     * the word "receita", one of its key foods. Several: the longest name. Null when none.
     */
    fun named(text: String, recipes: List<RecipeRef>): RecipeRef? {
        val message = " ${normalize(text)} "
        val words = message.trim().split(' ').toSet()
        fun meaningful(name: String) = normalize(name).split(' ').filter { it.length >= 3 && it !in STOP }
        val byName = recipes.filter { r ->
            val name = normalize(r.name)
            name.isNotEmpty() && (" $name " in message || meaningful(r.name).let { m -> m.size >= 2 && m.all { it in words } })
        }
        byName.maxByOrNull { it.name.length }?.let { return it }
        if ("receita" !in words) return null
        return recipes.firstOrNull { r -> keyFoods(r.ingredients).any { food -> meaningful(food).let { m -> m.isNotEmpty() && m.all { it in words } } } }
    }

    fun clip(text: String, max: Int): String = if (text.codePointCount(0, text.length) <= max) text else text.substring(0, text.offsetByCodePoints(0, max))
}
