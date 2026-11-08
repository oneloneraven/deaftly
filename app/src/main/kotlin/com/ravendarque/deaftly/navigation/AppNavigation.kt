package com.ravendarque.deaftly.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ravendarque.deaftly.AppContainer
import com.ravendarque.deaftly.ui.home.HomeScreen
import com.ravendarque.deaftly.ui.home.HomeViewModel
import com.ravendarque.deaftly.ui.placeholder.PlaceholderScreen

private val backStackSaver =
    listSaver<SnapshotStateList<Route>, String>(
        save = { stack -> stack.map { it.name } },
        restore = { names -> names.map(Route::valueOf).toMutableStateList() },
    )

@Composable
fun AppNavigation(container: AppContainer) {
    val backStack = rememberSaveable(saver = backStackSaver) { mutableStateListOf(Route.Home) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider = { route ->
            NavEntry(route) {
                when (route) {
                    Route.Home -> {
                        val viewModel = viewModel<HomeViewModel>(factory = container.homeViewModelFactory)
                        HomeScreen(viewModel, onNavigate = { backStack.add(it) })
                    }

                    else -> {
                        PlaceholderScreen(stringResource(route.title), onBack = { backStack.removeLastOrNull() })
                    }
                }
            }
        },
    )
}
