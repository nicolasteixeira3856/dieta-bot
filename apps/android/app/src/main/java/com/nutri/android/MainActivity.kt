package com.nutri.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nutri.android.ui.Bg
import com.nutri.android.ui.DiaViewModel
import com.nutri.android.ui.Etapa
import com.nutri.android.ui.FolhaAtual
import com.nutri.android.ui.NutriTheme
import com.nutri.android.ui.TelaEat
import com.nutri.android.ui.TelaHome
import com.nutri.android.ui.TelaTeto
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable data object RotaTeto
@Serializable data object RotaEat
@Serializable data object RotaHome

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val tela = intent?.getStringExtra("nutri_tela")
        setContent {
            NutriTheme {
                App(tela)
            }
        }
    }
}

@Composable
private fun App(telaCaptura: String?) {
    val vm: DiaViewModel = hiltViewModel()
    val ui by vm.ui.collectAsState()
    val nav = rememberNavController()
    LaunchedEffect(telaCaptura) {
        if (!telaCaptura.isNullOrBlank()) vm.abrirCaptura(telaCaptura)
    }
    LaunchedEffect(ui.pronto, ui.etapa) {
        if (!ui.pronto) return@LaunchedEffect
        val rota = when (ui.etapa) {
            Etapa.O1 -> RotaTeto
            Etapa.O2 -> RotaEat
            Etapa.HOME -> RotaHome
        }
        nav.navigate(rota) {
            popUpTo(nav.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .semantics { testTagsAsResourceId = true },
    ) {
        NavHost(navController = nav, startDestination = RotaTeto, modifier = Modifier.fillMaxSize()) {
            composable<RotaTeto> { TelaTeto(ui, vm) }
            composable<RotaEat> { TelaEat(ui, vm) }
            composable<RotaHome> {
                TelaHome(ui, vm)
                FolhaAtual(ui, vm)
            }
        }
    }
}
