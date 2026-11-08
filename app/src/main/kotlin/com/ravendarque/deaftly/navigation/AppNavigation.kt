package com.ravendarque.deaftly.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ravendarque.deaftly.AppContainer
import com.ravendarque.deaftly.ui.chat.ChatScreen
import com.ravendarque.deaftly.ui.chat.ChatViewModel
import com.ravendarque.deaftly.ui.chat.TemplatePickerScreen
import com.ravendarque.deaftly.ui.editor.EditorExit
import com.ravendarque.deaftly.ui.editor.EditorScreen
import com.ravendarque.deaftly.ui.editor.EditorViewModel
import com.ravendarque.deaftly.ui.fill.FillScreen
import com.ravendarque.deaftly.ui.fill.FillViewModel
import com.ravendarque.deaftly.ui.home.HomeScreen
import com.ravendarque.deaftly.ui.home.HomeViewModel
import com.ravendarque.deaftly.ui.ocr.OcrOutcome
import com.ravendarque.deaftly.ui.ocr.OcrScreen
import com.ravendarque.deaftly.ui.settings.SettingsScreen
import com.ravendarque.deaftly.ui.settings.SettingsViewModel
import com.ravendarque.deaftly.ui.show.ShowScreen
import com.ravendarque.deaftly.ui.show.ShowViewModel
import com.ravendarque.deaftly.ui.typeshow.TypeShowScreen

@Composable
fun AppNavigation(container: AppContainer) {
    val backStack = rememberNavBackStack(Route.Home)
    val insertIntoChat: (String) -> Unit = { text ->
        container.chatInsertions.offer(text)
        while (backStack.lastOrNull() !is Route.Chat && backStack.size > 1) backStack.removeLastOrNull()
    }

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
                templateEntries(container, backStack, insertIntoChat)
                entry<Route.TypeShow> {
                    TypeShowScreen(
                        onShow = { backStack.add(Route.Show(it)) },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<Route.Show> { route ->
                    val viewModel = viewModel<ShowViewModel>(factory = container.showViewModelFactory)
                    ShowScreen(
                        text = route.text,
                        viewModel = viewModel,
                        onClose = { backStack.removeLastOrNull() },
                        onContinueAsChat = { backStack.add(Route.Chat(route.text)) },
                    )
                }
                entry<Route.Chat> { route ->
                    val viewModel = viewModel<ChatViewModel>(factory = container.chatViewModelFactory(route.firstMessage))
                    ChatScreen(
                        viewModel,
                        onEnd = { backStack.removeLastOrNull() },
                        onInsertTemplate = { backStack.add(Route.TemplatePicker) },
                    )
                }
                entry<Route.Ocr> { OcrEntry(it, container, backStack) }
                entry<Route.Settings> {
                    val viewModel = viewModel<SettingsViewModel>(factory = container.settingsViewModelFactory)
                    SettingsScreen(viewModel, onBack = { backStack.removeLastOrNull() })
                }
            },
    )
}

private fun EntryProviderScope<NavKey>.templateEntries(
    container: AppContainer,
    backStack: NavBackStack<NavKey>,
    insertIntoChat: (String) -> Unit,
) {
    entry<Route.Fill> { route ->
        val viewModel = viewModel<FillViewModel>(factory = container.fillViewModelFactory(route.templateId))
        FillScreen(
            templateId = route.templateId,
            viewModel = viewModel,
            onNavigate = { backStack.add(it) },
            onBack = { backStack.removeLastOrNull() },
            onInsert = if (route.forChat) insertIntoChat else null,
        )
    }
    entry<Route.TemplatePicker> {
        val viewModel = viewModel<HomeViewModel>(factory = container.homeViewModelFactory)
        TemplatePickerScreen(
            viewModel = viewModel,
            onFill = { backStack.add(Route.Fill(it, forChat = true)) },
            onInsert = insertIntoChat,
            onBack = { backStack.removeLastOrNull() },
        )
    }
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
}

@Composable
private fun OcrEntry(
    route: Route.Ocr,
    container: AppContainer,
    backStack: NavBackStack<NavKey>,
) {
    OcrScreen(
        viewModelFactory = container.ocrViewModelFactory(route.templateId, route.placeholderKey),
        onClose = { backStack.removeLastOrNull() },
        onTypeInstead = {
            container.ocrOutcomes.post(OcrOutcome.TypeInstead(route.templateId, route.placeholderKey))
            backStack.removeLastOrNull()
        },
        onAdd = { text ->
            container.ocrOutcomes.post(OcrOutcome.Text(route.templateId, route.placeholderKey, text))
            backStack.removeLastOrNull()
        },
    )
}
