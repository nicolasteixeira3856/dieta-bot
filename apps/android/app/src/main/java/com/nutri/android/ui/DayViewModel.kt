package com.nutri.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutri.android.data.BudgetIn
import com.nutri.android.data.DayRepository
import com.nutri.android.data.DaySnapshot
import com.nutri.android.data.EstimateGate
import com.nutri.android.data.EstimateIn
import com.nutri.android.data.FitIn
import com.nutri.android.data.InstantClock
import com.nutri.android.data.PhotoCompressor
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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DayViewModel @Inject constructor(
    private val days: DayRepository,
    private val gate: EstimateGate,
    private val photos: PhotoCompressor,
    private val clock: InstantClock,
) : ViewModel() {
    private val calc = BudgetCalculator()
    private val _ui = MutableStateFlow(DayUi())
    val ui: StateFlow<DayUi> = _ui
    private var snapshot = DaySnapshot()
    private var captureMode: String? = null

    init {
        viewModelScope.launch {
            days.observeToday().collect { day ->
                snapshot = day
                if (captureMode == null) publish(day, _ui.value)
            }
        }
    }

    fun ceilingMode(mode: String) = _ui.update { it.copy(ceilingMode = mode) }
    fun sameField(v: String) = _ui.update { it.copy(sameField = v.filter { c -> c.isDigit() }) }
    fun weekdayField(v: String) = _ui.update { it.copy(weekdayField = v.filter { c -> c.isDigit() }) }
    fun weekendField(v: String) = _ui.update { it.copy(weekendField = v.filter { c -> c.isDigit() }) }
    fun dayField(i: Int, v: String) = _ui.update {
        val list = it.dayFields.toMutableList()
        list[i] = v.filter { c -> c.isDigit() }
        it.copy(dayFields = list)
    }

    fun continueO1() {
        _ui.update { it.copy(stage = Stage.O2) }
    }

    fun eat(mode: String) = _ui.update { it.copy(eat = mode) }
    fun pct(v: String) = _ui.update { it.copy(pct = v.filter { c -> c.isDigit() }.take(3).ifBlank { "50" }) }

    fun enter() {
        viewModelScope.launch {
            val today = today()
            val base = _ui.value
            days.saveProfile(
                ceilingMode = base.ceilingMode,
                kcalSame = base.sameField.toIntOrNull() ?: 2000,
                kcalWeekday = base.weekdayField.toIntOrNull() ?: 2000,
                kcalWeekend = base.weekendField.toIntOrNull() ?: 2300,
                kcalDays = base.dayFields.map { it.toIntOrNull() ?: 2000 },
                eat = base.eat,
                pct = base.pct.toIntOrNull() ?: 50,
                onboardingDone = true,
                firstDay = snapshot.firstDay.ifBlank { today.toString() },
            )
            _ui.update { it.copy(stage = Stage.HOME, sheet = null) }
        }
    }

    fun openLog() = _ui.update { it.copy(stage = Stage.HOME, sheet = SheetKind.T1, estimate = null, text = it.text) }
    fun text(v: String) = _ui.update { it.copy(text = v) }
    fun photo(uri: android.net.Uri) {
        val b64 = photos.jpegBase64(uri)
        _ui.update { it.copy(photoB64 = b64) }
    }

    fun submitLog() {
        val now = _ui.value
        if (now.text.isBlank() && now.photoB64 == null) return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            val hour = clock.now().atZone(SaoPaulo.zone)
            val window = windowAtHour(hour.hour)
            val out = gate.estimate(
                EstimateIn(
                    local_time = hour.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    window = window,
                    text = now.text,
                    image_b64 = now.photoB64,
                ),
            )
            val low = out.confidence != "high"
            _ui.update {
                it.copy(
                    loading = false,
                    estimate = out,
                    sheet = null,
                    stage = Stage.T2,
                    photoB64 = null,
                    window = window,
                    t2Title = if (low) "confiança baixa · 1 pergunta" else "confirmação",
                    t2Name = now.text.replace(",", " +").ifBlank { "refeição" },
                    t2Range = null,
                    t2Question = if (low) out.question else null,
                )
            }
        }
    }

    fun t2Answer(i: Int) = _ui.update { it.copy(t2Answer = i) }

    fun confirm() {
        val now = _ui.value
        val est = now.estimate ?: return
        viewModelScope.launch {
            days.addLog(
                window = now.window,
                text = now.text,
                kcal = est.kcal.toInt(),
                p = est.p.toInt(),
                stable = true,
            )
            _ui.update { it.copy(stage = Stage.HOME, sheet = null, text = "", estimate = null, t2Question = null) }
        }
    }

    fun undo() = _ui.update { it.copy(stage = Stage.HOME, sheet = SheetKind.T1, estimate = null) }

    fun openFit() = _ui.update {
        it.copy(
            stage = Stage.HOME,
            sheet = SheetKind.T3,
            fit = null,
            t3Headline = null,
            t3Line = null,
            t3Sub = null,
            t3Cta = "Encaixar",
        )
    }

    fun fitMode(mode: String) = _ui.update {
        it.copy(
            fitMode = mode,
            fit = null,
            t3Headline = null,
            t3Line = null,
            t3Sub = null,
            t3Cta = if (mode == "idea") "Já comi" else "Encaixar",
        )
    }

    fun fitText(v: String) = _ui.update { it.copy(fitText = v) }

    fun requestFit() {
        val now = _ui.value
        if (now.fitMode == "idea" && now.fit != null) return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            val mode = when (now.fitMode) {
                "have" -> "have"
                "idea" -> "surprise"
                else -> "want"
            }
            val items = if (mode == "have") {
                now.fitText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
            val out = gate.fit(
                FitIn(
                    mode = mode,
                    text = now.fitText,
                    availableItems = items,
                    budget = BudgetIn(now.windowBudget.toDouble(), (170 - now.protein).coerceAtLeast(0).toDouble()),
                ),
            )
            val dish = out.dish.takeIf { it.name.isNotBlank() } ?: out.options.firstOrNull()
            _ui.update {
                it.copy(
                    loading = false,
                    fit = out,
                    t3Headline = if (out.fits) "Cabe." else "Inteira não cabe.",
                    t3Line = dish?.let { d -> "${d.name} · ${d.kcal.toInt()} kcal · ${d.p.toInt()} g P" },
                    t3Cta = if (it.fitMode == "idea") "Já comi" else "Encaixar ${dish?.kcal?.toInt() ?: ""}".trim(),
                )
            }
        }
    }

    fun alreadyAte() = _ui.update { it.copy(sheet = SheetKind.T1, fit = null) }

    fun close() = _ui.update { it.copy(sheet = null) }

    fun workout(v: String) = _ui.update { it.copy(workout = v.filter { c -> c.isDigit() }) }

    fun saveWorkout() {
        viewModelScope.launch {
            days.setWorkout(_ui.value.workout.toIntOrNull())
        }
    }

    fun removeChip(window: String) {
        viewModelScope.launch {
            days.removeChip(window)
            if (captureMode != null) _ui.update { it.copy(chipLabel = null, chipNote = null) }
        }
    }

    fun leaveSplash() {
        if (captureMode != null) return
        _ui.update {
            it.copy(stage = if (snapshot.onboardingDone) Stage.HOME else Stage.O1)
        }
    }

    fun openCapture(screen: String) {
        captureMode = screen
        _ui.value = captureState(screen)
    }

    private fun publish(day: DaySnapshot, current: DayUi) {
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
        val stage = when {
            current.stage == Stage.SPLASH -> Stage.SPLASH
            current.stage == Stage.T2 -> Stage.T2
            day.onboardingDone -> Stage.HOME
            current.stage == Stage.O2 -> Stage.O2
            else -> Stage.O1
        }
        _ui.update {
            current.copy(
                ready = true,
                stage = stage,
                ceilingMode = day.ceilingMode,
                sameField = day.kcalSame.toString(),
                weekdayField = day.kcalWeekday.toString(),
                weekendField = day.kcalWeekend.toString(),
                dayFields = day.kcalDays.map { n -> n.toString() }.let { list ->
                    if (list.size == 7) list else List(7) { "2000" }
                },
                eat = day.eat,
                pct = day.pct.toString(),
                workout = day.workoutKcal?.toString() ?: current.workout,
                eaten = day.logs.sumOf { it.kcal },
                protein = day.logs.sumOf { it.p },
                effectiveCeiling = result.effectiveCeiling,
                windowBudget = result.windowBudget,
                window = window,
                shortDate = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM", java.util.Locale("pt", "BR"))),
                chips = if (appDay <= 1 || chip == null) emptyList() else listOf(chip),
                appDay = appDay,
                remaining = result.windowBudget,
                remainingLabel = if (weekend) "fds · almoço + janta + fecha" else "cabe na ${windowTitle(window).lowercase()}",
                nextTitle = if (weekend) windowTitle(window) else "${windowTitle(window)} · reserva 0",
                nextDetail = if (weekend && day.logs.isEmpty()) "café não cobrado" else "comido ${day.logs.sumOf { it.kcal }} · P ${day.logs.sumOf { it.p }} g",
                bar = if (result.effectiveCeiling == 0) 0f else (day.logs.sumOf { it.kcal }.toFloat() / result.effectiveCeiling).coerceIn(0f, 1f),
                chipLabel = if (appDay > 1 && chip != null) windowTitle(chip.window).lowercase() else null,
                chipNote = if (appDay > 1 && chip != null) {
                    if (chip.question) "Quer um atalho desta janela?" else "perguntado 1x"
                } else {
                    null
                },
                weekend = weekend,
            )
        }
    }

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
