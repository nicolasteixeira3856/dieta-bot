package com.nutri.android.feature.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.DaySnapshot
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.network.BudgetIn
import com.nutri.android.core.network.DishOut
import com.nutri.android.core.network.EstimateGate
import com.nutri.android.core.network.EstimateIn
import com.nutri.android.core.network.EstimateOut
import com.nutri.android.core.network.FitIn
import com.nutri.android.core.network.FitOut
import com.nutri.android.core.network.PhotoCompressor
import com.nutri.android.domain.BudgetCalculator
import com.nutri.android.domain.BudgetInput
import com.nutri.android.domain.CreditPolicy
import com.nutri.android.domain.SameEveryDayCeiling
import com.nutri.android.domain.SaoPaulo
import com.nutri.android.domain.SevenDayCeiling
import com.nutri.android.domain.StableLog
import com.nutri.android.domain.WeekdayWeekendCeiling
import com.nutri.android.domain.chipForWindow
import com.nutri.android.domain.windowAtHour
import com.nutri.android.domain.windowTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HomeNavEvent {
    data class NavigateToT2(
        val estimate: EstimateOut,
        val mealText: String,
        val window: String,
        val windowBudget: Int,
    ) : HomeNavEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DayRepository,
    private val gate: EstimateGate,
    private val photos: PhotoCompressor,
    private val clock: InstantClock,
) : ViewModel() {
    private val calc = BudgetCalculator()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<HomeNavEvent>(extraBufferCapacity = 1)
    val navEvents: SharedFlow<HomeNavEvent> = _navEvents.asSharedFlow()

    private var snapshot = DaySnapshot()

    init {
        viewModelScope.launch {
            repository.observeToday().collect { day ->
                snapshot = day
                updateStateWithDay(day)
            }
        }
    }

    private fun updateStateWithDay(day: DaySnapshot) {
        val today = day.date.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } ?: today()
        val first = day.firstDay.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } ?: today
        val appDay = ChronoUnit.DAYS.between(first, today).toInt() + 1
        val hour = clock.now().atZone(SaoPaulo.zone).hour
        val window = windowAtHour(hour)
        val profile = profileOf(day)
        val policy = when (day.eat) {
            "zero" -> CreditPolicy.ZERO
            "full" -> CreditPolicy.FULL
            else -> CreditPolicy.PARTIAL
        }
        val result = calc.calculate(
            BudgetInput(
                date = today,
                profile = profile,
                policy = policy,
                percent = day.pct,
                workoutKcal = day.workoutKcal,
                eaten = day.logs.sumOf { it.kcal },
                reservedUpcoming = 0,
            ),
        )
        val chip = chipForWindow(
            appDay = appDay,
            currentWindow = window,
            logs = day.logs.map { StableLog(it.window, it.stable) },
            removed = day.removedWindows.toSet(),
            asked = day.askedWindows.toSet(),
        )
        val weekend = today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY
        _uiState.update { current ->
            current.copy(
                ready = true,
                shortDate = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale("pt", "BR"))),
                remaining = result.windowBudget,
                remainingLabel = if (weekend) "fds · almoço + janta + fecha" else "cabe na ${windowTitle(window).lowercase()}",
                nextTitle = if (weekend) windowTitle(window) else "${windowTitle(window)} · reserva 0",
                nextDetail = if (weekend && day.logs.isEmpty()) "café não cobrado" else "comido ${day.logs.sumOf { it.kcal }} · P ${day.logs.sumOf { it.p }} g",
                bar = if (result.effectiveCeiling == 0) 0f else (day.logs.sumOf { it.kcal }.toFloat() / result.effectiveCeiling).coerceIn(0f, 1f),
                appDay = appDay,
                chips = if (appDay <= 1 || chip == null) emptyList() else listOf(chip),
                chipLabel = if (appDay > 1 && chip != null) windowTitle(chip.window).lowercase() else null,
                chipNote = if (appDay > 1 && chip != null) {
                    if (chip.question) "Quer um atalho desta janela?" else "perguntado 1x"
                } else null,
                window = window,
                eaten = day.logs.sumOf { it.kcal },
                protein = day.logs.sumOf { it.p },
                effectiveCeiling = result.effectiveCeiling,
                windowBudget = result.windowBudget,
                weekend = weekend,
                logs = day.logs.map { log ->
                    HomeLogLine(
                        window = log.window,
                        title = windowTitle(log.window),
                        kcal = log.kcal,
                        p = log.p,
                        text = log.text,
                    )
                },
            )
        }
    }

    fun openLog() = _uiState.update { it.copy(sheet = HomeSheetKind.T1) }
    fun closeSheet() = _uiState.update { it.copy(sheet = null) }
    fun setLogText(text: String) = _uiState.update { it.copy(logText = text) }
    fun setPhoto(uri: Uri) {
        val b64 = photos.jpegBase64(uri)
        _uiState.update { it.copy(photoB64 = b64) }
    }

    fun submitLog() {
        val now = _uiState.value
        if (now.logText.isBlank() && now.photoB64 == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val hour = clock.now().atZone(SaoPaulo.zone)
            val window = windowAtHour(hour.hour)
            val out = gate.estimate(
                EstimateIn(
                    local_time = hour.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    window = window,
                    text = now.logText,
                    image_b64 = now.photoB64,
                ),
            )
            _uiState.update { it.copy(loading = false, sheet = null, photoB64 = null) }
            _navEvents.emit(
                HomeNavEvent.NavigateToT2(
                    estimate = out,
                    mealText = now.logText,
                    window = window,
                    windowBudget = now.windowBudget,
                ),
            )
        }
    }

    fun openFit() = _uiState.update {
        it.copy(
            sheet = HomeSheetKind.T3,
            fit = null,
            fitDishes = emptyList(),
            selectedFitIndex = null,
            t3Headline = null,
            t3Line = null,
            t3Sub = null,
            t3Cta = "Encaixar",
        )
    }

    fun setFitMode(mode: String) = _uiState.update {
        it.copy(
            fitMode = mode,
            fit = null,
            fitDishes = emptyList(),
            selectedFitIndex = null,
            t3Headline = null,
            t3Line = null,
            t3Sub = null,
            t3Cta = "Encaixar",
        )
    }

    fun setFitText(text: String) = _uiState.update { it.copy(fitText = text) }

    fun requestFit() {
        val now = _uiState.value
        if (now.fit != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val mode = when (now.fitMode) {
                "have" -> "have"
                "idea" -> "surprise"
                else -> "want"
            }
            val items = if (mode == "have") {
                now.fitText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else emptyList()

            val raw = gate.fit(
                FitIn(
                    mode = mode,
                    text = now.fitText,
                    availableItems = items,
                    budget = BudgetIn(now.windowBudget.toDouble(), (170 - now.protein).coerceAtLeast(0).toDouble()),
                ),
            )
            val out = raw.copy(
                dish = if (raw.dish.name.isNotBlank()) raw.dish.copy(fits = true) else raw.dish,
                options = raw.options.map { it.copy(fits = true) },
            )
            val dishes = fitCards(out)
            val firstFit = dishes.indexOfFirst { it.fits && it.kcal.toInt() > 0 }
            _uiState.update {
                it.copy(
                    loading = false,
                    fit = out,
                    fitDishes = dishes,
                    selectedFitIndex = firstFit.takeIf { idx -> idx >= 0 },
                    t3Headline = if (out.fits) "Cabe." else "Inteira não cabe.",
                    t3Line = dishes.firstOrNull()?.let { d -> "${d.name} · ${d.kcal.toInt()} kcal · ${d.p.toInt()} g P" },
                    t3Cta = "Vou nesse",
                )
            }
        }
    }

    fun primaryFitAction() {
        if (_uiState.value.fit == null) requestFit() else commitFit()
    }

    fun selectFitDish(index: Int) {
        val dish = _uiState.value.fitDishes.getOrNull(index) ?: return
        if (!dish.fits || dish.kcal.toInt() <= 0) return
        _uiState.update { it.copy(selectedFitIndex = index) }
    }

    fun commitFit() {
        val now = _uiState.value
        if (now.fit == null) return
        val index = now.selectedFitIndex ?: return
        val dish = now.fitDishes.getOrNull(index) ?: return
        val kcal = dish.kcal.toInt()
        if (!dish.fits || kcal <= 0) return
        viewModelScope.launch {
            repository.addLog(
                window = now.window,
                text = dish.name,
                kcal = kcal,
                p = dish.p.toInt(),
                stable = true,
            )
            _uiState.update { clearFit(it).copy(sheet = null) }
        }
    }

    fun alreadyAte() {
        _uiState.update {
            clearFit(it).copy(sheet = HomeSheetKind.T1, logText = "", photoB64 = null)
        }
    }

    fun removeChip(window: String) {
        viewModelScope.launch {
            repository.removeChip(window)
        }
    }

    private fun clearFit(ui: HomeUiState) = ui.copy(
        fit = null,
        fitDishes = emptyList(),
        selectedFitIndex = null,
        t3Headline = null,
        t3Line = null,
        t3Sub = null,
        t3Cta = "Encaixar",
        fitText = "",
    )

    private fun profileOf(day: DaySnapshot) = when (day.ceilingMode) {
        "weekdayWeekend" -> WeekdayWeekendCeiling(day.kcalWeekday, day.kcalWeekend)
        "seven" -> {
            val d = day.kcalDays.let { if (it.size == 7) it else List(7) { 2000 } }
            SevenDayCeiling(d[0], d[1], d[2], d[3], d[4], d[5], d[6])
        }
        else -> SameEveryDayCeiling(day.kcalSame)
    }

    private fun today(): LocalDate = SaoPaulo.date(clock.now())
}

internal fun fitCards(out: FitOut): List<DishOut> {
    val seen = linkedSetOf<String>()
    val cards = mutableListOf<DishOut>()
    fun add(dish: DishOut, selectable: Boolean) {
        if (dish.name.isBlank()) return
        if (!seen.add(dish.name)) return
        cards += if (selectable && !dish.fits) dish.copy(fits = true) else dish
    }
    if (out.dish.name.isNotBlank()) add(out.dish, selectable = out.dish.fits)
    out.options.filter { it.fits }.forEach { add(it, selectable = true) }
    return cards
}
