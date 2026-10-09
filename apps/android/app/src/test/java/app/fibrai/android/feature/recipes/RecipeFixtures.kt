package app.fibrai.android.feature.recipes

/** States drawn in the D23 golds rcpL and rcpD. */
object RecipeFixtures {
    val rcpL = RecipesUiState(
        loaded = true,
        rows = listOf(
            RecipeRow(1, "Frango com brócolis e arroz", 520, 46, 41, 17),
            RecipeRow(2, "Macarrão com atum ao sugo", 620, 42, 70, 18),
            RecipeRow(3, "Pizza de pão sírio", 480, 30, 52, 16),
            RecipeRow(4, "Omelete de claras com aveia", 340, 32, 30, 9),
            RecipeRow(5, "Iogurte com granola e banana", 310, 18, 46, 6),
        ),
    )

    val rcpD = RecipesUiState(
        loaded = true,
        detail = RecipeDetail(
            id = 1,
            name = "Frango com brócolis e arroz",
            versionLine = "Versão 1 · salva em 25 de setembro",
            kcal = 520, p = 46, c = 41, g = 17,
            ingredients = listOf(
                "Peito de frango" to "120 g", "Arroz cozido" to "120 g", "Brócolis" to "100 g",
                "Azeite" to "5 g", "Alho" to "5 g", "Queijo ralado (opcional)" to "15 g",
            ),
            steps = listOf(
                "Corte o frango em cubos e grelhe por 8 min.",
                "Refogue o alho no azeite e junte o brócolis por 3 min.",
                "Misture o arroz e o frango e finalize com o queijo.",
            ),
        ),
    )
}
