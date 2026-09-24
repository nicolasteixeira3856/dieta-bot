package com.nutri.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.diaStore by preferencesDataStore("nutri_dia")

@Serializable
data class LogSalvo(
    val janela: String,
    val texto: String,
    val kcal: Int,
    val p: Int,
    val estavel: Boolean = true,
)

@Serializable
data class DiaSalvo(
    val modoTeto: String = "mesmo",
    val kcalUnico: Int = 2000,
    val kcalUtil: Int = 2000,
    val kcalFds: Int = 2300,
    val kcalDias: List<Int> = List(7) { 2000 },
    val eat: String = "parcial",
    val pct: Int = 50,
    val treinoKcal: Int? = null,
    val logs: List<LogSalvo> = emptyList(),
    val removidas: List<String> = emptyList(),
    val perguntadas: List<String> = emptyList(),
    val primeiroDia: String = "",
    val onboardingFeito: Boolean = false,
)

@Singleton
class DiaStore @Inject constructor(@ApplicationContext context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val data = context.diaStore
    private val chave = stringPreferencesKey("dia")

    val fluxo: Flow<DiaSalvo> = data.data.map { prefs ->
        prefs[chave]?.let { runCatching { json.decodeFromString<DiaSalvo>(it) }.getOrNull() } ?: DiaSalvo()
    }

    suspend fun gravar(dia: DiaSalvo) {
        data.edit { it[chave] = json.encodeToString(dia) }
    }
}
