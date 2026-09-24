package com.nutri.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutri.android.domain.tituloJanela

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaAtual(ui: DiaUi, vm: DiaViewModel) {
    val sheet = ui.sheet ?: return
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { vm.fechar() },
        sheetState = state,
        containerColor = Surf,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Handle) },
    ) {
        when (sheet) {
            Folha.T1 -> FolhaRegistro(ui, vm)
            Folha.T2 -> FolhaConfirma(ui, vm)
            Folha.T3 -> FolhaEncaixe(ui, vm)
        }
    }
}

@Composable
private fun FolhaRegistro(ui: DiaUi, vm: DiaViewModel) {
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) vm.foto(uri)
    }
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("REGISTRAR", color = Gold, fontSize = 11.sp)
        OutlinedTextField(
            value = ui.texto,
            onValueChange = vm::texto,
            modifier = Modifier.fillMaxWidth().testTag("t1-texto"),
            placeholder = { Text("2 paes, ovo, cafe com leite", color = Dim) },
            textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 18.sp),
        )
        TextButton(
            onClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
            modifier = Modifier.testTag("t1-foto"),
        ) { Text(if (ui.fotoB64 == null) "Foto" else "Foto pronta", color = Gold) }
        NutriCta(if (ui.carregando) "…" else "Registrar", Modifier.testTag("t1-registrar")) { vm.registrar() }
    }
}

@Composable
private fun FolhaConfirma(ui: DiaUi, vm: DiaViewModel) {
    val est = ui.estimativa
    Column(Modifier.padding(20.dp).testTag("t2-card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(tituloJanela(ui.janela), color = Gold, fontSize = 11.sp)
        Text("≈ ${est?.kcal?.toInt() ?: 0} kcal", color = TextMain, fontSize = 32.sp, fontWeight = FontWeight(590))
        Text("P ${est?.p?.toInt() ?: 0} · C ${est?.c?.toInt() ?: 0} · G ${est?.g?.toInt() ?: 0}", color = Muted)
        Text("confiança ${est?.confianca ?: "baixa"}", color = if (est?.confianca == "alto") Good else Gold)
        val kcal = est?.kcal?.toInt() ?: 0
        val saldo = (ui.tetoEfetivo - ui.comido - kcal).coerceAtLeast(0)
        val semResposta = est == null || (est.confianca == "baixa" && kcal == 0)
        val cabe = !semResposta && kcal <= ui.orcamento
        Text(
            when {
                semResposta -> "sem resposta"
                cabe -> "cabe"
                else -> "não cabe"
            },
            color = if (cabe) Good else Bad,
            modifier = Modifier.testTag("t2-cabe"),
        )
        if (est?.confianca != "alto") {
            Text(est?.pergunta ?: "descreve em 1 linha", color = TextMain, modifier = Modifier.testTag("t2-pergunta"))
        }
        Text("Saldo $saldo / ${ui.tetoEfetivo}", color = Muted)
        Text("estimativa, não consulta", color = Dim, fontSize = 12.sp)
        NutriCta("Confirmar", Modifier.testTag("t2-confirmar")) { vm.confirmar() }
    }
}

@Composable
private fun FolhaEncaixe(ui: DiaUi, vm: DiaViewModel) {
    Column(Modifier.padding(20.dp).testTag("t3-encaixe"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("O QUE CABE AGORA", color = Gold, fontSize = 11.sp)
        Text("${tituloJanela(ui.janela)} · teto ${ui.orcamento} kcal", color = TextMain)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Modo("Quero", ui.modoEncaixe == "quero", Modifier.testTag("modo-quero")) { vm.modoEncaixe("quero") }
            Modo("Tenho", ui.modoEncaixe == "tenho", Modifier.testTag("modo-tenho")) { vm.modoEncaixe("tenho") }
            Modo("Sem ideia", ui.modoEncaixe == "ideia", Modifier.testTag("modo-ideia")) { vm.modoEncaixe("ideia") }
        }
        if (ui.modoEncaixe != "ideia") {
            OutlinedTextField(
                value = ui.encaixeTexto,
                onValueChange = vm::encaixeTexto,
                modifier = Modifier.fillMaxWidth().testTag("t3-texto"),
                placeholder = { Text(if (ui.modoEncaixe == "quero") "quero hambúrguer caseiro" else "pão, frango, queijo", color = Dim) },
            )
        }
        NutriCta(if (ui.carregando) "…" else "Encaixar", Modifier.testTag("t3-enviar")) { vm.pedirEncaixe() }
        val fit = ui.encaixe
        if (fit != null) {
            val pratos = if (fit.opcoes.isNotEmpty()) fit.opcoes else listOf(fit.prato).filter { it.nome.isNotBlank() }
            if (pratos.isEmpty()) {
                Text("não cabe", color = Bad)
            }
            pratos.forEach { prato ->
                Column(Modifier.fillMaxWidth().cardBorda().padding(12.dp)) {
                    Text(prato.nome.ifBlank { "prato" }, color = TextMain, fontWeight = FontWeight.W600)
                    Text("${prato.kcal.toInt()} kcal · P ${prato.p.toInt()} · cabe", color = Good)
                    prato.porcoes.forEach { p -> Text("${p.quantidade} ${p.nome}", color = Muted, fontSize = 13.sp) }
                }
            }
            if (fit.pergunta.isNotBlank()) Text(fit.pergunta, color = TextMain)
            Text("estimativa, não consulta", color = Dim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Modo(texto: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(texto, color = if (on) Gold else Muted)
    }
}
