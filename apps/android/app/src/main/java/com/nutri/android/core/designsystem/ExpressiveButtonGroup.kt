package com.nutri.android.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveButtonGroup(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    val p = LocalPalette.current
    val colors = ToggleButtonDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = p.muted,
        checkedContainerColor = p.gold.copy(alpha = 0.16f),
        checkedContentColor = p.gold,
    )
    if (stacked) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(p.surf, RoundedCornerShape(20.dp))
                .border(1.dp, p.line, RoundedCornerShape(20.dp))
                .padding(3.dp)
                .testTag("expressive-button-group-stacked"),
        ) {
            options.forEachIndexed { i, label ->
                ButtonGroup(
                    overflowIndicator = { },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val itemMod = with(this) { Modifier.weight(1f) }
                    customItem(
                        buttonGroupContent = {
                            ToggleButton(
                                checked = selected == i,
                                onCheckedChange = { onSelect(i) },
                                modifier = itemMod.semantics {
                                    contentDescription = label
                                },
                                colors = colors,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight(560),
                                    color = if (selected == i) p.gold else p.muted,
                                )
                            }
                        },
                        menuContent = { },
                    )
                }
            }
        }
    } else {
        ButtonGroup(
            overflowIndicator = { },
            modifier = modifier
                .fillMaxWidth()
                .background(p.surf, RoundedCornerShape(18.dp))
                .border(1.dp, p.line, RoundedCornerShape(18.dp))
                .padding(3.dp)
                .testTag("expressive-button-group"),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            options.forEachIndexed { i, label ->
                val itemMod = with(this) { Modifier.weight(1f) }
                customItem(
                    buttonGroupContent = {
                        ToggleButton(
                            checked = selected == i,
                            onCheckedChange = { onSelect(i) },
                            modifier = itemMod.semantics {
                                contentDescription = label
                            },
                            colors = colors,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 11.dp),
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight(560),
                                color = if (selected == i) p.gold else p.muted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    },
                    menuContent = { },
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ExpressiveButtonGroupHorizontalPreview() {
    NutriTheme {
        ExpressiveButtonGroup(options = listOf("0%", "%", "100%"), selected = 0, onSelect = {})
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ExpressiveButtonGroupStackedPreview() {
    NutriTheme {
        ExpressiveButtonGroup(options = listOf("Mesmo", "Útil/fds", "7 dias"), selected = 1, onSelect = {}, stacked = true)
    }
}
