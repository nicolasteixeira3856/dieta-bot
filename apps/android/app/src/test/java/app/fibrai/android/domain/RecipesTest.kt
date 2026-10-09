package app.fibrai.android.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** A68 (ADR-052 § 3): which saved recipe a message names. */
class RecipesTest {
    private val omelete = RecipeRef(1, "Omelete de forno", listOf(RecipeIngredient("ovos", 150.0, 210.0), RecipeIngredient("ricota", 50.0, 80.0)))
    private val pizza = RecipeRef(2, "Pizza de pão sírio", listOf(RecipeIngredient("pão sírio", 60.0, 160.0), RecipeIngredient("muçarela", 30.0, 90.0)))
    private val all = listOf(omelete, pizza)

    @Test
    fun byTheWholeNameWithoutAccents_byItsWords_orByAKeyFoodWithReceita() {
        assertThat(Recipes.named("lembra a receita da omelete de forno?", all)).isEqualTo(omelete)
        assertThat(Recipes.named("jantei a pizza de pao sirio", all)).isEqualTo(pizza)
        assertThat(Recipes.named("fiz de novo aquela pizza com pão sírio", all)).isEqualTo(pizza)
        assertThat(Recipes.named("qual era a receita com ricota?", all)).isEqualTo(omelete)
        assertThat(Recipes.named("comi ricota no café", all)).isNull()
        assertThat(Recipes.named("jantei arroz e feijão", all)).isNull()
    }

    @Test
    fun keyFoodsAreTheThreeWithMostEnergy() {
        val items = listOf(RecipeIngredient("a", 10.0, 10.0), RecipeIngredient("b", 10.0, 300.0), RecipeIngredient("c", 10.0, 50.0), RecipeIngredient("d", 10.0, 100.0))
        assertThat(Recipes.keyFoods(items)).containsExactly("b", "d", "c").inOrder()
    }
}
