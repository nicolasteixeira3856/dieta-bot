package app.fibrai.android.feature.recipes

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.SavedRecipe
import app.fibrai.android.core.telemetry.NoopTelemetry
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.Recipes
import app.fibrai.android.domain.SaoPaulo
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** rcpL row: the recipe's name and its current totals. */
@Immutable
data class RecipeRow(val id: Long, val name: String, val kcal: Int, val p: Int, val c: Int, val g: Int)

/** rcpD: the current version of one recipe. */
@Immutable
data class RecipeDetail(
    val id: Long,
    val name: String,
    /** `Versão 1 · salva em 25 de setembro`. */
    val versionLine: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    /** Item and grams (`120 g`), as the portions table shows them. */
    val ingredients: List<Pair<String, String>>,
    val steps: List<String>,
)

@Immutable
data class RecipesUiState(
    val loaded: Boolean = false,
    val rows: List<RecipeRow> = emptyList(),
    val detail: RecipeDetail? = null,
    /** Excluir receita asks first (Dialog/Confirm Tone=Danger). */
    val confirmDelete: Boolean = false,
    /** The detail's recipe was deleted: the screen goes back. */
    val deleted: Boolean = false,
)

/** A68 (ADR-052 § 2): Config → Receitas (rcpL) and a recipe (rcpD), with delete. Saving happens in the Chat. */
@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val repository: DayRepository,
    private val telemetry: Telemetry = NoopTelemetry,
) : ViewModel() {
    private val open = MutableStateFlow<Long?>(null)
    private val local = MutableStateFlow(RecipesUiState())

    val uiState: StateFlow<RecipesUiState> = combine(repository.observeRecipes(), open, local) { recipes, id, l ->
        l.copy(
            loaded = true,
            rows = recipes.map(::row),
            detail = id?.let { recipes.firstOrNull { r -> r.recipe.id == it } }?.let(::detail),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState())

    /** The detail of [id] (rcpD). */
    fun open(id: Long) {
        open.value = id
        local.update { it.copy(confirmDelete = false, deleted = false) }
    }

    fun askDelete() = local.update { it.copy(confirmDelete = true) }

    fun cancelDelete() = local.update { it.copy(confirmDelete = false) }

    fun confirmDelete() {
        val id = open.value ?: return
        local.update { it.copy(confirmDelete = false) }
        viewModelScope.launch {
            repository.deleteRecipe(id)
            telemetry.event(TelemetryEvents.RECIPE_DELETED, emptyMap())
            local.update { it.copy(deleted = true) }
        }
    }

    private fun row(s: SavedRecipe) = RecipeRow(s.recipe.id, s.recipe.name, s.version.kcal, s.version.p, s.version.c, s.version.g)

    private fun detail(s: SavedRecipe): RecipeDetail {
        val saved = Instant.ofEpochMilli(s.version.createdAtEpochMs).atZone(SaoPaulo.zone).format(DAY)
        return RecipeDetail(
            id = s.recipe.id,
            name = s.recipe.name,
            versionLine = "Versão ${s.version.version} · salva em $saved",
            kcal = s.version.kcal,
            p = s.version.p,
            c = s.version.c,
            g = s.version.g,
            ingredients = Recipes.decodeIngredients(s.version.ingredients).map { it.name to grams(it.g) },
            steps = Recipes.decodeSteps(s.version.steps),
        )
    }

    companion object {
        private val DAY = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

        fun grams(g: Double): String = if (g % 1.0 == 0.0) "${g.toInt()} g" else "${g.toString().replace('.', ',')} g"
    }
}
