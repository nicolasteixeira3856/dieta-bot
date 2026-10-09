package app.fibrai.android.flavor

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

/** dev flavor (ADR-019): no tool routes since A69 removed `Memória da IA (dev)` (ADR-053 § 3). */
fun NavGraphBuilder.flavorDestinations(nav: NavController) = Unit

/** dev flavor (ADR-019): no tool rows; `debug.fibrai.hide_dev_tools` keeps its meaning for a future tool. */
@Composable
fun ColumnScope.FlavorConfigRows(nav: NavController) = Unit
