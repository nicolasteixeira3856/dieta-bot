package com.nutri.android.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GoldInventoryTest {
    @Test
    fun parsesSourcePerId() {
        val readme = """
            ## Golds

            ```text
            stitch: splash.png · o1.png
            figma: home0.png · home1.png
            ```

            ## Next
        """.trimIndent()
        val map = GoldInventory.parse(readme)
        assertThat(map).containsExactly("splash", "stitch", "o1", "stitch", "home0", "figma", "home1", "figma")
    }

    @Test
    fun everyInventoryIdHasAGoldInItsSourceFolder() {
        for (id in listOf("splash", "home1", "chatE", "cfg")) {
            val source = GoldInventory.source(id)
            assertThat(java.io.File("../../../docs/qa/$source/dark/$id.png").exists()).isTrue()
        }
    }
}
