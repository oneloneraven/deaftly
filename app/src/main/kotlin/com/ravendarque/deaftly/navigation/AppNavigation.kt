package com.ravendarque.deaftly.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ravendarque.deaftly.AppContainer
import com.ravendarque.deaftly.ui.home.HomeScreen
import com.ravendarque.deaftly.ui.home.HomeViewModel
import com.ravendarque.deaftly.ui.placeholder.PlaceholderScreen

@Composable
fun AppNavigation(container: AppContainer) {
    val backStack = rememberNavBackStack(Route.Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Route.Home> {
                    val viewModel = viewModel<HomeViewModel>(factory = container.homeViewModelFactory)
                    HomeScreen(viewModel, onNavigate = { backStack.add(it) })
                }
                entry<Route.Fill> { PlaceholderEntry(it, backStack) }
                entry<Route.Show> { PlaceholderEntry(it, backStack) }
                entry<Route.Chat> { PlaceholderEntry(it, backStack) }
                entry<Route.Ocr> { PlaceholderEntry(it, backStack) }
                entry<Route.Editor> { PlaceholderEntry(it, backStack) }
                entry<Route.Settings> { PlaceholderEntry(it, backStack) }
            },
    )
}

@Composable
private fun PlaceholderEntry(
    route: Route,
    backStack: NavBackStack<NavKey>,
) {
    PlaceholderScreen(stringResource(route.titleRes()), onBack = { backStack.removeLastOrNull() })
}
