package com.nutri.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
        GrupoEscolha(
            opcoes = listOf(
                "Mesmo todos os dias" to (ui.modoTeto == "mesmo"),
                "Seg–sex / sáb–dom" to (ui.modoTeto == "util"),
                "Cada dia diferente" to (ui.modoTeto == "sete"),
            ),
            onEscolha = { indice ->
                vm.modoTeto(when (indice) { 1 -> "util"; 2 -> "sete"; else -> "mesmo" })
            },
            modifier = Modifier.testTag("o1-modos"),
        )
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
        GrupoEscolha(
            opcoes = listOf(
                "Não entra · 0%" to (ui.eat == "zero"),
                "Entra um pouco" to (ui.eat == "parcial"),
                "Entra tudo · 100%" to (ui.eat == "cem"),
            ),
            onEscolha = { indice ->
                vm.eat(when (indice) { 0 -> "zero"; 2 -> "cem"; else -> "parcial" })
            },
            modifier = Modifier.testTag("o2-eat"),
        )
        Text(
            when (ui.eat) {
                "zero" -> "Treino 1000 e o teto continua o mesmo."
                "cem" -> "Treino 480 → teto +480."
                else -> "Você escolhe a %."
            },
            color = Muted,
            fontSize = 13.sp,
        )
        if (ui.eat == "parcial") {
            CampoNumero("% que entra", ui.pct, suffix = "%", onChange = vm::pct)
            Text(
                "Preview: treino 480 → teto +$extra. Crédito real só com treino anotado.",
                color = Gold,
                fontSize = 13.sp,
            )
        }
        Text("estimativa, não consulta", color = Dim, fontSize = 12.sp)
        NutriCta("Entrar no app", Modifier.testTag("o2-entrar")) { vm.entrar() }
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
