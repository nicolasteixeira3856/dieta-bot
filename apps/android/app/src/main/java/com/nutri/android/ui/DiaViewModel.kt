package com.nutri.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutri.android.data.DiaSalvo
import com.nutri.android.data.DiaStore
import com.nutri.android.data.EstimateGate
import com.nutri.android.data.EstimateIn
import com.nutri.android.data.EstimateOut
import com.nutri.android.data.FitIn
import com.nutri.android.data.FitOut
import com.nutri.android.data.FotoCompressor
import com.nutri.android.data.LogSalvo
import com.nutri.android.data.OrcamentoIn
import com.nutri.android.domain.BudgetCalculator
import com.nutri.android.domain.Chip
import com.nutri.android.domain.EntradaOrcamento
import com.nutri.android.domain.LogEstavel
import com.nutri.android.domain.PoliticaCredito
import com.nutri.android.domain.SaoPaulo
import com.nutri.android.domain.TetoMesmoTodosOsDias
import com.nutri.android.domain.TetoSeteDias
import com.nutri.android.domain.TetoUtilFds
import com.nutri.android.domain.chipDaJanela
import com.nutri.android.domain.janelaNaHora
import com.nutri.android.domain.tituloJanela
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Etapa { O1, O2, HOME }
enum class Folha { T1, T2, T3 }

data class DiaUi(
    val pronto: Boolean = false,
    val etapa: Etapa = Etapa.O1,
    val sheet: Folha? = null,
    val modoTeto: String = "mesmo",
    val campoUnico: String = "2000",
    val campoUtil: String = "2000",
    val campoFds: String = "2300",
    val camposDia: List<String> = List(7) { "2000" },
    val eat: String = "parcial",
    val pct: String = "50",
    val texto: String = "",
    val fotoB64: String? = null,
    val treino: String = "",
    val comido: Int = 0,
    val proteina: Int = 0,
    val tetoEfetivo: Int = 2000,
    val orcamento: Int = 2000,
    val janela: String = "cafe",
    val dataCurta: String = "",
    val chips: List<Chip> = emptyList(),
    val diaApp: Int = 1,
    val estimativa: EstimateOut? = null,
    val encaixeTexto: String = "",
    val encaixe: FitOut? = null,
    val modoEncaixe: String = "quero",
    val carregando: Boolean = false,
)

@HiltViewModel
class DiaViewModel @Inject constructor(
    private val store: DiaStore,
    private val gate: EstimateGate,
    private val fotos: FotoCompressor,
) : ViewModel() {
    private val calc = BudgetCalculator()
    private val _ui = MutableStateFlow(DiaUi())
    val ui: StateFlow<DiaUi> = _ui
    private var salvo = DiaSalvo()

    init {
        viewModelScope.launch {
            store.fluxo.collect { dia ->
                salvo = dia
                publicar(dia, _ui.value)
            }
        }
    }

    fun modoTeto(modo: String) = _ui.update { it.copy(modoTeto = modo) }
    fun campoUnico(v: String) = _ui.update { it.copy(campoUnico = v.filter { c -> c.isDigit() }) }
    fun campoUtil(v: String) = _ui.update { it.copy(campoUtil = v.filter { c -> c.isDigit() }) }
    fun campoFds(v: String) = _ui.update { it.copy(campoFds = v.filter { c -> c.isDigit() }) }
    fun campoDia(i: Int, v: String) = _ui.update {
        val lista = it.camposDia.toMutableList()
        lista[i] = v.filter { c -> c.isDigit() }
        it.copy(camposDia = lista)
    }

    fun continuarO1() {
        _ui.update { it.copy(etapa = Etapa.O2) }
    }

    fun eat(modo: String) = _ui.update { it.copy(eat = modo) }
    fun pct(v: String) = _ui.update { it.copy(pct = v.filter { c -> c.isDigit() }.take(3).ifBlank { "50" }) }

    fun entrar() {
        viewModelScope.launch {
            val hoje = hoje()
            val base = _ui.value
            val novo = rascunho(base).copy(
                onboardingFeito = true,
                primeiroDia = salvo.primeiroDia.ifBlank { hoje.toString() },
            )
            store.gravar(novo)
            _ui.update { it.copy(etapa = Etapa.HOME, sheet = null) }
        }
    }

    fun abrirRegistro() = _ui.update { it.copy(sheet = Folha.T1, estimativa = null, texto = "") }
    fun texto(v: String) = _ui.update { it.copy(texto = v) }
    fun foto(uri: android.net.Uri) {
        val b64 = fotos.jpegBase64(uri)
        _ui.update { it.copy(fotoB64 = b64) }
    }

    fun registrar() {
        val agora = _ui.value
        if (agora.texto.isBlank() && agora.fotoB64 == null) return
        viewModelScope.launch {
            _ui.update { it.copy(carregando = true) }
            val hora = java.time.ZonedDateTime.now(SaoPaulo.zona)
            val janela = janelaNaHora(hora.hour)
            val out = gate.estimar(
                EstimateIn(
                    local_time = hora.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    janela = janela,
                    text = agora.texto,
                    image_b64 = agora.fotoB64,
                ),
            )
            _ui.update { it.copy(carregando = false, estimativa = out, sheet = Folha.T2, fotoB64 = null, janela = janela) }
        }
    }

    fun confirmar() {
        val agora = _ui.value
        val est = agora.estimativa ?: return
        viewModelScope.launch {
            val log = LogSalvo(
                janela = agora.janela,
                texto = agora.texto,
                kcal = est.kcal.toInt(),
                p = est.p.toInt(),
                estavel = true,
            )
            store.gravar(rascunho(agora).copy(logs = salvo.logs + log))
            _ui.update { it.copy(sheet = null, texto = "", estimativa = null) }
        }
    }

    fun abrirEncaixe() = _ui.update { it.copy(sheet = Folha.T3, encaixe = null, encaixeTexto = "") }
    fun modoEncaixe(modo: String) = _ui.update { it.copy(modoEncaixe = modo, encaixe = null) }
    fun encaixeTexto(v: String) = _ui.update { it.copy(encaixeTexto = v) }

    fun pedirEncaixe() {
        val agora = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(carregando = true) }
            val mode = when (agora.modoEncaixe) {
                "tenho" -> "have"
                "ideia" -> "surprise"
                else -> "want"
            }
            val itens = if (mode == "have") {
                agora.encaixeTexto.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
            val out = gate.encaixar(
                FitIn(
                    mode = mode,
                    text = agora.encaixeTexto,
                    itens_disponiveis = itens,
                    orcamento = OrcamentoIn(agora.orcamento.toDouble(), (170 - agora.proteina).coerceAtLeast(0).toDouble()),
                ),
            )
            _ui.update { it.copy(carregando = false, encaixe = out) }
        }
    }

    fun fechar() = _ui.update { it.copy(sheet = null) }

    fun treino(v: String) = _ui.update { it.copy(treino = v.filter { c -> c.isDigit() }) }

    fun gravarTreino() {
        viewModelScope.launch {
            val n = _ui.value.treino.toIntOrNull()
            store.gravar(rascunho(_ui.value).copy(treinoKcal = n))
        }
    }

    fun removerChip(janela: String) {
        viewModelScope.launch {
            store.gravar(rascunho(_ui.value).copy(removidas = (salvo.removidas + janela).distinct()))
        }
    }

    fun responderChip(janela: String, criar: Boolean) {
        viewModelScope.launch {
            val removidas = if (criar) salvo.removidas else (salvo.removidas + janela).distinct()
            store.gravar(
                rascunho(_ui.value).copy(
                    perguntadas = (salvo.perguntadas + janela).distinct(),
                    removidas = removidas,
                ),
            )
        }
    }

    fun abrirCaptura(tela: String) {
        _ui.update {
            when (tela) {
                "o2" -> it.copy(etapa = Etapa.O2, sheet = null, pronto = true)
                "t0" -> it.copy(etapa = Etapa.HOME, sheet = null, diaApp = 1, chips = emptyList(), pronto = true)
                "t1" -> it.copy(etapa = Etapa.HOME, sheet = Folha.T1, pronto = true)
                "t3" -> it.copy(etapa = Etapa.HOME, sheet = Folha.T3, pronto = true)
                else -> it.copy(etapa = Etapa.O1, sheet = null, pronto = true)
            }
        }
    }

    private fun publicar(dia: DiaSalvo, atual: DiaUi) {
        val hoje = hoje()
        val primeiro = dia.primeiroDia.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } ?: hoje
        val diaApp = ChronoUnit.DAYS.between(primeiro, hoje).toInt() + 1
        val hora = java.time.ZonedDateTime.now(SaoPaulo.zona).hour
        val janela = janelaNaHora(hora)
        val perfil = perfilDe(dia)
        val politica = when (dia.eat) {
            "zero" -> PoliticaCredito.ZERO
            "cem" -> PoliticaCredito.CEM
            else -> PoliticaCredito.PARCIAL
        }
        val resultado = calc.calcular(
            EntradaOrcamento(
                data = hoje,
                perfil = perfil,
                politica = politica,
                percentual = dia.pct,
                treinoKcal = dia.treinoKcal,
                consumido = dia.logs.sumOf { it.kcal },
                reservaProximas = 0,
            ),
        )
        val chip = chipDaJanela(
            diaApp = diaApp,
            janelaAtual = janela,
            logs = dia.logs.map { LogEstavel(it.janela, it.estavel) },
            removidas = dia.removidas.toSet(),
            perguntadas = dia.perguntadas.toSet(),
        )
        _ui.update {
            atual.copy(
                pronto = true,
                etapa = if (dia.onboardingFeito) Etapa.HOME else atual.etapa,
                modoTeto = dia.modoTeto,
                campoUnico = dia.kcalUnico.toString(),
                campoUtil = dia.kcalUtil.toString(),
                campoFds = dia.kcalFds.toString(),
                camposDia = dia.kcalDias.map { n -> n.toString() }.let { lista ->
                    if (lista.size == 7) lista else List(7) { "2000" }
                },
                eat = dia.eat,
                pct = dia.pct.toString(),
                treino = dia.treinoKcal?.toString() ?: atual.treino,
                comido = dia.logs.sumOf { it.kcal },
                proteina = dia.logs.sumOf { it.p },
                tetoEfetivo = resultado.tetoEfetivo,
                orcamento = resultado.orcamentoJanela,
                janela = janela,
                dataCurta = hoje.format(DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale("pt", "BR"))),
                chips = if (diaApp <= 1 || chip == null) emptyList() else listOf(chip),
                diaApp = diaApp,
            )
        }
    }

    private fun rascunho(ui: DiaUi): DiaSalvo {
        return salvo.copy(
            modoTeto = ui.modoTeto,
            kcalUnico = ui.campoUnico.toIntOrNull() ?: 2000,
            kcalUtil = ui.campoUtil.toIntOrNull() ?: 2000,
            kcalFds = ui.campoFds.toIntOrNull() ?: 2300,
            kcalDias = ui.camposDia.map { it.toIntOrNull() ?: 2000 },
            eat = ui.eat,
            pct = ui.pct.toIntOrNull() ?: 50,
            treinoKcal = ui.treino.toIntOrNull() ?: salvo.treinoKcal,
            primeiroDia = salvo.primeiroDia,
            onboardingFeito = salvo.onboardingFeito,
        )
    }

    private fun perfilDe(dia: DiaSalvo) = when (dia.modoTeto) {
        "util" -> TetoUtilFds(dia.kcalUtil, dia.kcalFds)
        "sete" -> {
            val d = dia.kcalDias.let { if (it.size == 7) it else List(7) { 2000 } }
            TetoSeteDias(d[0], d[1], d[2], d[3], d[4], d[5], d[6])
        }
        else -> TetoMesmoTodosOsDias(dia.kcalUnico)
    }

    private fun hoje(): LocalDate = SaoPaulo.data(java.time.Instant.now())
}
