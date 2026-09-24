package com.nutri.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutri.android.domain.tituloJanela

@Composable
fun TelaHome(ui: DiaUi, vm: DiaViewModel) {
    val frac = if (ui.tetoEfetivo == 0) 0f else (ui.comido.toFloat() / ui.tetoEfetivo).coerceIn(0.02f, 1f)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ui.dataCurta, color = Muted, fontSize = 13.sp)
            Text("Teto ${ui.tetoEfetivo}", color = TextMain, fontWeight = FontWeight.W600, fontSize = 14.sp)
        }
        Text(
            "${ui.comido}  / ${ui.tetoEfetivo} kcal",
            modifier = Modifier.testTag("saldo"),
            color = TextMain,
            fontSize = 34.sp,
            fontWeight = FontWeight(590),
        )
        Text("P ${ui.proteina} / 170 g", color = Muted, fontSize = 13.sp)
        Box(
            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)).background(Line),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Gold))
        }
        Column(Modifier.fillMaxWidth().cardBorda().padding(16.dp).testTag("proxima-janela")) {
            Text("PRÓXIMA", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.W600)
            Text("${tituloJanela(ui.janela)}", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.W600)
            Text("Orçamento ${ui.orcamento} kcal", color = TextMain, fontSize = 16.sp)
            Text("Ainda sem atalho. Escreve ou manda foto.", color = Muted, fontSize = 13.sp)
            NutriCta("O que cabe agora", Modifier.testTag("home-cta").padding(top = 8.dp)) { vm.abrirEncaixe() }
        }
        Row(
            Modifier.fillMaxWidth().cardBorda().clickable { vm.abrirRegistro() }.padding(14.dp).testTag("home-composer"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("O que você comeu, ou uma foto", color = Muted)
            Text("◉", color = Gold)
        }
        if (ui.diaApp > 1) {
            ui.chips.forEach { chip ->
                Column(Modifier.fillMaxWidth().cardBorda().padding(12.dp).testTag("chip-${chip.janela}")) {
                    Text(tituloJanela(chip.janela), color = Gold)
                    if (chip.pergunta) {
                        Text("Quer um atalho desta janela?", color = TextMain)
                        Row {
                            TextButton(onClick = { vm.responderChip(chip.janela, true) }) { Text("Criar", color = Gold) }
                            TextButton(onClick = { vm.responderChip(chip.janela, false) }) { Text("Não", color = Muted) }
                        }
                    }
                    TextButton(onClick = { vm.removerChip(chip.janela) }, Modifier.testTag("chip-remover")) {
                        Text("Remover", color = Bad)
                    }
                }
            }
        }
        listOf("cafe", "lanche_manha", "almoco", "lanche_tarde", "janta", "ceia").forEach { id ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tituloJanela(id), color = if (id == ui.janela) TextMain else Muted)
                Text(if (id == ui.janela) "agora" else "pendente", color = if (id == ui.janela) Gold else Dim, fontSize = 12.sp)
            }
        }
        Text(if (ui.treino.isBlank()) "Treino hoje" else "Treino ${ui.treino} kcal", color = Muted, fontSize = 13.sp)
        OutlinedTreino(ui.treino, vm::treino)
        TextButton(onClick = { vm.gravarTreino() }, Modifier.testTag("gravar-treino")) { Text("anotar kcal", color = Gold) }
        Text("estimativa, não consulta", color = Dim, fontSize = 12.sp, modifier = Modifier.testTag("disclaimer"))
    }
}

@Composable
private fun OutlinedTreino(valor: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().testTag("treino"),
        label = { Text("treino kcal") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = Line,
            focusedTextColor = TextMain,
            unfocusedTextColor = TextMain,
            cursorColor = Gold,
        ),
    )
}
