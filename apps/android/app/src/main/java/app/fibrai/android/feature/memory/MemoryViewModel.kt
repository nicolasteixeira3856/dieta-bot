package app.fibrai.android.feature.memory

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.telemetry.NoopTelemetry
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.Fact
import app.fibrai.android.domain.MemoryRules
import app.fibrai.android.domain.SaoPaulo
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** memL row: the category chip, the text, a routine's numbers, the origin line. */
@Immutable
data class FactView(
    val id: String,
    val chip: String,
    val text: String,
    /** kcal, P, C, G of a routine or liked dish with all four; null otherwise. */
    val macros: List<Int>? = null,
    /** `Declarado · 25/09`, `Registrado 5 dias · último 07/10`, `Até 11/10 · criado 08/10`. */
    val origin: String,
)

/** A memL group: `FIXAS`, `ROTINAS` or `TEMPORÁRIAS`, its one-line explanation and its facts. */
@Immutable
data class FactGroup(val label: String, val detail: String, val facts: List<FactView>)

@Immutable
data class MemoryUiState(
    val loaded: Boolean = false,
    val groups: List<FactGroup> = emptyList(),
    /** The fact being corrected (its text in a field, Cancelar | Salvar). */
    val editing: String? = null,
    val draft: String = "",
    /** The fact whose delete asks first. */
    val confirmDelete: String? = null,
)

/**
 * A69 (ADR-053 § 1): "O que a Tali sabe" (memL). Every fact of the memory with its origin; delete (a tombstone keeps it from
 * coming back until the next compaction) and correct (same category and slot). No model call.
 */
@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memory: FactMemory,
    private val repository: DayRepository,
    private val clock: InstantClock,
    private val telemetry: Telemetry = NoopTelemetry,
) : ViewModel() {
    private val facts = MutableStateFlow<List<Fact>?>(null)
    private val local = MutableStateFlow(MemoryUiState())

    val uiState: StateFlow<MemoryUiState> = combine(facts, repository.observeToday(), local) { list, day, l ->
        val names = day.slots.associate { it.id.toString() to it.name }
        l.copy(loaded = list != null, groups = groups(list.orEmpty(), names))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoryUiState())

    init {
        viewModelScope.launch { reload() }
    }

    private fun today(): LocalDate = SaoPaulo.date(clock.now())

    private suspend fun reload() {
        facts.value = runCatching { memory.read(today()).facts }.getOrDefault(emptyList())
    }

    fun edit(id: String) {
        val fact = facts.value?.firstOrNull { it.id == id } ?: return
        local.update { it.copy(editing = id, draft = fact.text, confirmDelete = null) }
    }

    fun setDraft(text: String) = local.update { it.copy(draft = text) }

    fun cancelEdit() = local.update { it.copy(editing = null, draft = "") }

    fun saveEdit() {
        val id = local.value.editing ?: return
        val text = local.value.draft
        local.update { it.copy(editing = null, draft = "") }
        viewModelScope.launch {
            val before = facts.value?.firstOrNull { it.id == id }
            val after = runCatching { memory.correct(id, text, today()) }.getOrNull()?.facts?.firstOrNull { it.id == id }
            reload()
            if (before != null && after != null && after.text != before.text) event(TelemetryEvents.MEMORY_FACT_CORRECTED, after)
        }
    }

    fun askDelete(id: String) = local.update { it.copy(confirmDelete = id, editing = null) }

    fun cancelDelete() = local.update { it.copy(confirmDelete = null) }

    fun confirmDelete() {
        val id = local.value.confirmDelete ?: return
        local.update { it.copy(confirmDelete = null) }
        viewModelScope.launch {
            val fact = facts.value?.firstOrNull { it.id == id }
            runCatching { memory.delete(id, today()) }
            reload()
            fact?.let { event(TelemetryEvents.MEMORY_FACT_DELETED, it) }
        }
    }

    private fun event(name: String, fact: Fact) = telemetry.event(name, mapOf("kind" to fact.kind, "category" to fact.category))

    companion object {
        private val DAY = DateTimeFormatter.ofPattern("dd/MM")

        /** Fixas (permanent, not a routine), rotinas (routines, liked dishes, what was learned), temporárias; empty groups leave. */
        fun groups(facts: List<Fact>, slotNames: Map<String, String>): List<FactGroup> {
            val fixed = facts.filter { it.permanent && it.category != MemoryRules.ROUTINE }
            val temp = facts.filter { it.temp }
            val learned = facts - fixed.toSet() - temp.toSet()
            return listOf(
                FactGroup("Fixas", "Valem até você mudar ou apagar.", fixed.map { view(it, slotNames) }),
                FactGroup("Rotinas", "Aprendidas com o que você registra.", learned.map { view(it, slotNames) }),
                FactGroup("Temporárias", "Saem sozinhas na data indicada.", temp.map { view(it, slotNames) }),
            ).filter { it.facts.isNotEmpty() }
        }

        fun view(fact: Fact, slotNames: Map<String, String>): FactView {
            val slot = fact.slot?.let(slotNames::get)
            val chip = when (fact.category) {
                MemoryRules.ROUTINE -> listOfNotNull("Rotina", slot).joinToString(" · ")
                MemoryRules.LIKED -> listOfNotNull("Prato aprovado", slot).joinToString(" · ")
                MemoryRules.EQUIPMENT -> "Equipamento"
                "portion" -> "Porção"
                else -> "Preferência"
            }
            val macros = listOfNotNull(fact.kcal, fact.p, fact.c, fact.g).takeIf { it.size == 4 && fact.category in MemoryRules.MEAL_CATEGORIES }
            return FactView(fact.id, chip, fact.text, macros, origin(fact))
        }

        private fun day(iso: String): String = runCatching { LocalDate.parse(iso).format(DAY) }.getOrDefault(iso)

        /** `Até {created + 3}` (the day a temporary fact leaves), `Declarado`, or the recorded days and the last one. */
        fun origin(fact: Fact): String = when {
            fact.temp -> "Até ${day(LocalDate.parse(fact.created).plusDays(MemoryRules.TEMP_TTL_DAYS).toString())} · criado ${day(fact.created)}"
            fact.days.isEmpty() || fact.source == MemoryRules.EXPLICIT || fact.source == MemoryRules.DECLARED -> "Declarado · ${day(fact.created)}"
            else -> "Registrado ${fact.days.size} ${if (fact.days.size == 1) "dia" else "dias"} · último ${day(fact.lastSeen ?: fact.created)}"
        }
    }
}
