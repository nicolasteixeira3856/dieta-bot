package app.fibrai.android.flavor

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import app.fibrai.android.feature.devtools.DevMemoryConfigRow
import app.fibrai.android.feature.devtools.devMemoryDestination

/** dev flavor (ADR-019): tool routes. */
fun NavGraphBuilder.flavorDestinations(nav: NavController) = devMemoryDestination(nav)

/** dev flavor (ADR-019): tool rows at the end of Config. */
@Composable
fun ColumnScope.FlavorConfigRows(nav: NavController) = DevMemoryConfigRow(nav)
