package app.fibrai.android.feature.memory

/** The state drawn in the D24 gold memL: the portion being corrected. */
object MemoryFixtures {
    val memL = MemoryUiState(
        loaded = true,
        groups = listOf(
            FactGroup(
                "Fixas", "Valem até você mudar ou apagar.",
                listOf(
                    FactView("P1", "Preferência", "Usa leite semidesnatado", origin = "Declarado · 25/09"),
                    FactView("P2", "Preferência", "Não come carne de porco", origin = "Declarado · 26/09"),
                    FactView("P3", "Porção", "Arroz: 150 g por refeição", origin = "Declarado · 27/09"),
                ),
            ),
            FactGroup(
                "Rotinas", "Aprendidas com o que você registra.",
                listOf(
                    FactView("D1", "Rotina · Café da manhã", "Pão francês com ovo mexido e café com leite", listOf(430, 26, 36, 20), "Registrado 5 dias · último 07/10"),
                    FactView("D2", "Rotina · Almoço", "Arroz, feijão, frango grelhado e salada", listOf(610, 45, 70, 14), "Registrado 8 dias · último 08/10"),
                ),
            ),
            FactGroup(
                "Temporárias", "Saem sozinhas na data indicada.",
                listOf(FactView("T1", "Preferência", "Viagem até domingo: refeições fora de casa", origin = "Até 11/10 · criado 08/10")),
            ),
        ),
        editing = "P3",
        draft = "Arroz: 150 g por refeição",
    )
}
