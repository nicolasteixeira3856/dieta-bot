package app.fibrai.android.feature.devtools

import com.google.common.truth.Truth.assertThat
import app.fibrai.android.core.network.ChatProfile
import app.fibrai.android.core.network.ChatSlot
import org.junit.Test

class ProfileTextTest {
    private val profile = ChatProfile(
        ceilingKcal = 2230,
        pTarget = 167,
        cTarget = 223,
        gTarget = 74,
        eatBack = "partial 50%",
        slots = listOf(ChatSlot("11", "Café da manhã", "07:30"), ChatSlot("12", "Lanche", "11:00")),
    )

    private fun ok(text: String) = (ProfileText.parse(text, profile) as ProfileText.Result.Ok).profile
    private fun error(text: String) = (ProfileText.parse(text, profile) as ProfileText.Result.Error).message

    @Test
    fun format_isTheKeyValueBlock() {
        assertThat(ProfileText.format(profile)).isEqualTo(
            """
            teto_kcal=2230
            proteina_g=167
            carbo_g=223
            gordura_g=74
            compensacao=partial 50%
            refeicao.1=Café da manhã 07:30
            refeicao.2=Lanche 11:00
            """.trimIndent(),
        )
    }

    @Test
    fun roundTrip_isLossless() {
        assertThat(ok(ProfileText.format(profile))).isEqualTo(profile)
        listOf("zero", "full", "partial 120%").forEach { eat ->
            val p = profile.copy(eatBack = eat)
            assertThat(ok(ProfileText.format(p).replace("partial 50%", eat))).isEqualTo(p)
        }
    }

    @Test
    fun edits_valuesNamesAndTimes_keepSlotIds() {
        val text = ProfileText.format(profile)
            .replace("proteina_g=167", "proteina_g = 180")
            .replace("compensacao=partial 50%", "compensacao=full")
            .replace("Lanche 11:00", "Lanche da manhã 9:45")
        val edited = ok(text)
        assertThat(edited.pTarget).isEqualTo(180)
        assertThat(edited.eatBack).isEqualTo("full")
        assertThat(edited.slots[1]).isEqualTo(ChatSlot("12", "Lanche da manhã", "09:45"))
    }

    @Test
    fun removedKey_isRefused() {
        assertThat(error(ProfileText.format(profile).replace("gordura_g=74\n", ""))).isEqualTo("Falta gordura_g: remover não é permitido.")
        assertThat(error(ProfileText.format(profile).replace("\nrefeicao.2=Lanche 11:00", ""))).isEqualTo("Falta refeicao.2: remover não é permitido.")
    }

    @Test
    fun addedSlot_isRefusedWithItsLine() {
        assertThat(error(ProfileText.format(profile) + "\nrefeicao.3=Jantar 20:00")).isEqualTo("Linha 8: adicionar refeição não é permitido.")
    }

    @Test
    fun invalidValues_reportTheLine() {
        val base = ProfileText.format(profile)
        assertThat(error(base.replace("teto_kcal=2230", "teto_kcal=0"))).startsWith("Linha 1: teto_kcal")
        assertThat(error(base.replace("carbo_g=223", "carbo_g=abc"))).startsWith("Linha 3: carbo_g")
        assertThat(error(base.replace("partial 50%", "partial 0%"))).startsWith("Linha 5: compensacao")
        assertThat(error(base.replace("Lanche 11:00", "Lanche 25:00"))).isEqualTo("Linha 7: horário inválido.")
        assertThat(error(base.replace("Lanche 11:00", "11:00"))).isEqualTo("Linha 7: use Nome HH:MM.")
        assertThat(error(base.replace("gordura_g=74", "gordura_g=74\nproteina_g=1"))).isEqualTo("Linha 5: proteina_g repetida.")
        assertThat(error(base + "\nsexo=m")).isEqualTo("Linha 8: chave desconhecida sexo.")
        assertThat(error(base + "\nsem igual")).isEqualTo("Linha 8: esperado chave=valor.")
    }
}
