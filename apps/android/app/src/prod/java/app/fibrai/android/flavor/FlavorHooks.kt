package app.fibrai.android.flavor

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

/** prod flavor (ADR-019): no tool routes. */
fun NavGraphBuilder.flavorDestinations(nav: NavController) = Unit

/** prod flavor (ADR-019): Config has no extra row. */
@Composable
fun ColumnScope.FlavorConfigRows(nav: NavController) = Unit
