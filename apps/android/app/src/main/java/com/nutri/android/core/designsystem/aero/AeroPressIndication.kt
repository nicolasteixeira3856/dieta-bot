package com.nutri.android.core.designsystem.aero

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/** Press feedback of every Aero control (ADR-030, no Material ripple): a text/primary veil while pressed. */
@Composable
fun aeroPressIndication(): IndicationNodeFactory {
    val color = Aero.colors.textPrimary.copy(alpha = PRESS_ALPHA)
    return remember(color) { AeroPress(color) }
}

private const val PRESS_ALPHA = 0.12f

private class AeroPress(private val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressNode(interactionSource, color)

    override fun equals(other: Any?): Boolean = other is AeroPress && other.color == color

    override fun hashCode(): Int = color.hashCode()
}

private class PressNode(private val source: InteractionSource, private val color: Color) : Modifier.Node(), DrawModifierNode {
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                }
                val now = presses.isNotEmpty()
                if (now != pressed) {
                    pressed = now
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (pressed) drawRect(color)
    }
}
