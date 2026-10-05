package app.fibrai.android.feature.workout

import com.google.common.truth.Truth.assertThat
import app.fibrai.android.domain.CreditPolicy
import org.junit.Test

class WorkoutEditorStateTest {
    @Test
    fun `credit line per policy`() {
        assertThat(WorkoutEditorState("350", CreditPolicy.PARTIAL, 50).creditLine)
            .isEqualTo("+175 kcal na meta de hoje (compensação 50%)")
        assertThat(WorkoutEditorState("350", CreditPolicy.FULL, 50).creditLine)
            .isEqualTo("+350 kcal na meta de hoje (compensação 100%)")
        assertThat(WorkoutEditorState("350", CreditPolicy.ZERO, 50).creditLine)
            .isEqualTo("Compensação desativada na Config")
    }

    @Test
    fun `empty field is no workout, credit 0`() {
        val empty = WorkoutEditorState("", CreditPolicy.FULL, 100)
        assertThat(empty.kcal).isNull()
        assertThat(empty.credit).isEqualTo(0)
        assertThat(empty.creditLine).isEqualTo("+0 kcal na meta de hoje (compensação 100%)")
    }

    @Test
    fun `clean keeps up to 5 digits`() {
        assertThat(WorkoutEditorState.clean("3a5-0")).isEqualTo("350")
        assertThat(WorkoutEditorState.clean("1234567")).isEqualTo("12345")
    }
}
