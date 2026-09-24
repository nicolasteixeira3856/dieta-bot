package com.nutri.android.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RegrasExtraTest {
    @Test
    fun `dia 1 nao mostra chip mesmo com dois logs estaveis`() {
        val chip = chipDaJanela(
            diaApp = 1,
            janelaAtual = "cafe",
            logs = listOf(LogEstavel("cafe", true), LogEstavel("cafe", true)),
            removidas = emptySet(),
            perguntadas = emptySet(),
        )
        assertThat(chip).isNull()
    }

    @Test
    fun `segundo log estavel da mesma janela pergunta uma vez e pode ser removido`() {
        val logs = listOf(LogEstavel("cafe", true), LogEstavel("cafe", true), LogEstavel("almoco", true))
        val pergunta = chipDaJanela(2, "cafe", logs, emptySet(), emptySet())
        assertThat(pergunta).isEqualTo(Chip("cafe", pergunta = true))

        val jaPerguntou = chipDaJanela(2, "cafe", logs, emptySet(), setOf("cafe"))
        assertThat(jaPerguntou).isEqualTo(Chip("cafe", pergunta = false))

        val removido = chipDaJanela(2, "cafe", logs, setOf("cafe"), setOf("cafe"))
        assertThat(removido).isNull()
    }

    @Test
    fun `lado maior da foto fica em 1280 e jpeg e 70`() {
        assertThat(FotoEscala.QUALIDADE_JPEG).isEqualTo(70)
        assertThat(FotoEscala.dimensoes(2000, 1000)).isEqualTo(1280 to 640)
        assertThat(FotoEscala.dimensoes(800, 600)).isEqualTo(800 to 600)
        assertThat(FotoEscala.dimensoes(1000, 2000)).isEqualTo(640 to 1280)
    }

    @Test
    fun `nao oferece prato que explode o teto`() {
        val ofertas = ofertasQueCabem(
            listOf(
                PratoOferta("hamburguer", 900.0),
                PratoOferta("ovos", 380.0),
                PratoOferta("", 100.0),
            ),
            orcamentoKcal = 455.0,
        )
        assertThat(ofertas.map { it.nome }).containsExactly("ovos")
    }
}
