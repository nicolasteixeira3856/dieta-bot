package com.nutri.android.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GoldInventoryTest {
    @Test
    fun parsesIds() {
        val readme = """
            ## Golds

            ```text
            splash.png · o1.png
            home0.png · home1.png
            ```

            ## Next
        """.trimIndent()
        assertThat(GoldInventory.parse(readme)).containsExactly("splash", "o1", "home0", "home1").inOrder()
    }

    @Test
    fun everyInventoryIdHasAFigmaGold() {
        for (id in listOf("splash", "home1", "chatE", "cfg")) {
            assertThat(GoldInventory.contains(id)).isTrue()
            assertThat(java.io.File("../../../docs/qa/figma/dark/$id.png").exists()).isTrue()
        }
    }
}
