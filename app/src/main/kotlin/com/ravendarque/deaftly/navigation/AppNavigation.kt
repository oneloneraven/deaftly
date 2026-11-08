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
import com.ravendarque.deaftly.ui.editor.EditorExit
import com.ravendarque.deaftly.ui.editor.EditorScreen
import com.ravendarque.deaftly.ui.editor.EditorViewModel
import com.ravendarque.deaftly.ui.fill.FillScreen
import com.ravendarque.deaftly.ui.fill.FillViewModel
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
                entry<Route.Fill> { route ->
                    val viewModel = viewModel<FillViewModel>(factory = container.fillViewModelFactory(route.templateId))
                    FillScreen(
                        templateId = route.templateId,
                        viewModel = viewModel,
                        onNavigate = { backStack.add(it) },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<Route.Show> { PlaceholderEntry(it, backStack, body = it.text) }
                entry<Route.Chat> { PlaceholderEntry(it, backStack) }
                entry<Route.Ocr> { PlaceholderEntry(it, backStack) }
                entry<Route.Editor> { route ->
                    val viewModel = viewModel<EditorViewModel>(factory = container.editorViewModelFactory(route.templateId))
                    EditorScreen(viewModel) { exit ->
                        if (exit is EditorExit.Copied) {
                            backStack[backStack.lastIndex] = Route.Editor(exit.templateId)
                        } else {
                            backStack.removeLastOrNull()
                        }
                    }
                }
                entry<Route.Settings> { PlaceholderEntry(it, backStack) }
            },
    )
}

@Composable
private fun PlaceholderEntry(
    route: Route,
    backStack: NavBackStack<NavKey>,
    body: String? = null,
) {
    PlaceholderScreen(stringResource(route.titleRes()), onBack = { backStack.removeLastOrNull() }, body = body)
}
