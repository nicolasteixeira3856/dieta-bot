package app.fibrai.android.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** A60 part C (ADR-045): the subset parser, its literal fallbacks and plain(). Pure. */
class ReplyMarkupTest {
    private fun p(vararg spans: ReplySpan) = ReplyBlock.Paragraph(spans.toList())
    private fun t(text: String) = ReplySpan(text)
    private fun b(text: String) = ReplySpan(text, bold = true)

    @Test fun everyMarker() {
        val reply = listOf(
            "Duas opções para o jantar:",
            "- **Pizza de pão sírio**: 1 pão sírio (60 g) · **420 kcal**",
            "- Omelete de forno",
            "| Item | Gramas |",
            "| --- | --- |",
            "| Peito de frango | 120 g |",
            "| Arroz cozido | 120 g |",
            "1. Corte o frango em cubos.",
            "2. Grelhe por 8 min.",
            "Total: ~**520 kcal** · 46P · 41C · 17G",
        ).joinToString("\n")
        assertThat(ReplyMarkup.parse(reply)).containsExactly(
            p(t("Duas opções para o jantar:")),
            ReplyBlock.Bullet(listOf(b("Pizza de pão sírio"), t(": 1 pão sírio (60 g) · "), b("420 kcal"))),
            ReplyBlock.Bullet(listOf(t("Omelete de forno"))),
            ReplyBlock.Table("Item" to "Gramas", listOf("Peito de frango" to "120 g", "Arroz cozido" to "120 g")),
            ReplyBlock.Step(1, listOf(t("Corte o frango em cubos."))),
            ReplyBlock.Step(2, listOf(t("Grelhe por 8 min."))),
            p(t("Total: ~"), b("520 kcal"), t(" · 46P · 41C · 17G")),
        ).inOrder()
        assertThat(ReplyMarkup.formatted(reply)).isTrue()
        assertThat(ReplyMarkup.formatted("Para caber nas 560 kcal:\n• 1 pão sírio")).isFalse()
    }

    @Test fun fallbacksAreLiteral() {
        assertThat(ReplyMarkup.parse("**Arroz com feijão")).containsExactly(p(t("**Arroz com feijão")))
        val seven = (1..7).joinToString("\n") { "| item$it | ${it}0 g |" }
        val long = ReplyMarkup.parse("| Item | Gramas |\n| --- | --- |\n$seven")
        assertThat(long.all { it is ReplyBlock.Paragraph }).isTrue()
        assertThat(long).hasSize(9)
        val table = "| Item | Gramas |\n| --- | --- |\n| ovo | 100 g |"
        val two = ReplyMarkup.parse("$table\n\n$table")
        assertThat(two.filterIsInstance<ReplyBlock.Table>()).hasSize(1)
        assertThat(two.last()).isEqualTo(p(t("| ovo | 100 g |")))
        assertThat(ReplyMarkup.parse("| Item | Gramas |\n| --- | --- |\n| arroz |").none { it is ReplyBlock.Table }).isTrue()
        assertThat(ReplyMarkup.parse("  - aninhado")).containsExactly(p(t("  - aninhado")))
        assertThat(ReplyMarkup.parse("## Título")).containsExactly(p(t("## Título")))
        assertThat(ReplyMarkup.parse("[link](http://x.y)")).containsExactly(p(t("[link](http://x.y)")))
        assertThat(ReplyMarkup.parse("1) passo")).containsExactly(p(t("1) passo")))
    }

    @Test fun plainDropsTheMarkers() {
        val reply = "Duas opções:\n- **Pizza**: · **420 kcal**\n| Item | Gramas |\n| --- | --- |\n| ovo | 100 g |\n1. Bata.\nTotal: **520 kcal**"
        assertThat(ReplyMarkup.plain(reply)).isEqualTo("Duas opções:\nPizza: · 420 kcal\novo: 100 g\n1. Bata.\nTotal: 520 kcal")
        assertThat(ReplyMarkup.plain("Sem marcadores.")).isEqualTo("Sem marcadores.")
        assertThat(ReplyMarkup.plain(ReplyMarkup.plain(reply))).isEqualTo(ReplyMarkup.plain(reply))
    }

    @Test fun aLongReplyParsesInUnderAMillisecond() {
        val reply = ("- **Frango** 120 g, arroz **100 g** e salada.\n").repeat(60).take(2000)
        repeat(200) { ReplyMarkup.parse(reply) }
        val n = 200
        val started = System.nanoTime()
        repeat(n) { ReplyMarkup.parse(reply) }
        val perCall = (System.nanoTime() - started) / n
        assertThat(perCall).isLessThan(1_000_000L)
    }
}
