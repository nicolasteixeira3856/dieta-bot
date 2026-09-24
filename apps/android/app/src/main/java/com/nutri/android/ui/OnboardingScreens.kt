package com.nutri.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TelaTeto(ui: DiaUi, vm: DiaViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Qual é o teto de kcal?", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text("Você manda no número.", color = Muted)
        ModoChip("Mesmo todos os dias", ui.modoTeto == "mesmo", Modifier.testTag("modo-mesmo")) { vm.modoTeto("mesmo") }
        ModoChip("Seg–sex / sáb–dom", ui.modoTeto == "util", Modifier.testTag("modo-util")) { vm.modoTeto("util") }
        ModoChip("Cada dia diferente", ui.modoTeto == "sete", Modifier.testTag("modo-sete")) { vm.modoTeto("sete") }
        when (ui.modoTeto) {
            "util" -> {
                CampoNumero("Seg–sex", ui.campoUtil, onChange = vm::campoUtil)
                CampoNumero("Sáb–dom", ui.campoFds, onChange = vm::campoFds)
            }
            "sete" -> {
                val nomes = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
                nomes.forEachIndexed { i, nome ->
                    CampoNumero(nome, ui.camposDia[i]) { vm.campoDia(i, it) }
                }
            }
            else -> CampoNumero("Teto", ui.campoUnico, onChange = vm::campoUnico)
        }
        Text("Dá pra mudar o número depois. Isso não é cálculo de nutricionista.", color = Dim, fontSize = 12.sp)
        NutriCta("Continuar", Modifier.testTag("o1-continuar")) { vm.continuarO1() }
    }
}

@Composable
fun TelaEat(ui: DiaUi, vm: DiaViewModel) {
    val pct = ui.pct.toIntOrNull() ?: 50
    val extra = 480 * pct / 100
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("PRIMEIRO OPEN", color = Gold, fontSize = 11.sp)
        Text("Quando você treina, o teto sobe?", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        EatCard("Não entra · 0%", "Treino 1000 e o teto continua o mesmo.", ui.eat == "zero", Modifier.testTag("eat-0")) { vm.eat("zero") }
        EatCard("Entra um pouco", "Você escolhe a %.", ui.eat == "parcial", Modifier.testTag("eat-parcial")) { vm.eat("parcial") }
        if (ui.eat == "parcial") {
            CampoNumero("% que entra", ui.pct, suffix = "%", onChange = vm::pct)
            Text(
                "Preview: treino 480 → teto +$extra. Crédito real só com treino anotado.",
                color = Gold,
                fontSize = 13.sp,
            )
        }
        EatCard("Entra tudo · 100%", "Treino 480 → teto +480.", ui.eat == "cem", Modifier.testTag("eat-100")) { vm.eat("cem") }
        Text("estimativa, não consulta", color = Dim, fontSize = 12.sp)
        NutriCta("Entrar no app", Modifier.testTag("o2-entrar")) { vm.entrar() }
    }
}

@Composable
private fun ModoChip(texto: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().cardBorda(),
    ) {
        Text(texto, color = if (on) Gold else TextMain)
    }
}

@Composable
private fun EatCard(titulo: String, corpo: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.fillMaxWidth().cardBorda().padding(14.dp)) {
        androidx.compose.material3.TextButton(onClick = onClick) {
            Column {
                Text(titulo, color = if (on) Gold else TextMain)
                Text(corpo, color = Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun CampoNumero(label: String, valor: String, suffix: String = "kcal", onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = { Text(suffix, color = Muted) },
        textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight(560)),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = Line,
            focusedTextColor = TextMain,
            unfocusedTextColor = TextMain,
            cursorColor = Gold,
        ),
        singleLine = true,
    )
}
