package com.ravendarque.deaftly.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
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
import com.ravendarque.deaftly.ui.chat.ChatOpening
import com.ravendarque.deaftly.ui.chat.ChatScreen
import com.ravendarque.deaftly.ui.chat.ChatViewModel
import com.ravendarque.deaftly.ui.chat.TemplatePickerScreen
import com.ravendarque.deaftly.ui.editor.EditorExit
import com.ravendarque.deaftly.ui.editor.EditorScreen
import com.ravendarque.deaftly.ui.editor.EditorViewModel
import com.ravendarque.deaftly.ui.facetoface.FaceToFaceScreen
import com.ravendarque.deaftly.ui.facetoface.FaceToFaceViewModel
import com.ravendarque.deaftly.ui.fill.FillScreen
import com.ravendarque.deaftly.ui.fill.FillViewModel
import com.ravendarque.deaftly.ui.firstrun.FirstRunScreen
import com.ravendarque.deaftly.ui.firstrun.FirstRunViewModel
import com.ravendarque.deaftly.ui.help.HelpPageScreen
import com.ravendarque.deaftly.ui.help.HelpTopicsScreen
import com.ravendarque.deaftly.ui.home.HomeViewModel
import com.ravendarque.deaftly.ui.home.TemplatesScreen
import com.ravendarque.deaftly.ui.hub.HubScreen
import com.ravendarque.deaftly.ui.ocr.OcrOutcome
import com.ravendarque.deaftly.ui.ocr.OcrScreen
import com.ravendarque.deaftly.ui.settings.SettingsScreen
import com.ravendarque.deaftly.ui.settings.SettingsViewModel
import com.ravendarque.deaftly.ui.show.ShowScreen
import com.ravendarque.deaftly.ui.show.ShowViewModel
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.typeshow.TypeShowScreen
import kotlinx.coroutines.flow.first

@Composable
fun AppNavigation(container: AppContainer) {
    val firstRunDone by produceState<Boolean?>(null) { value = container.settingsRepository.firstRunDone.first() }
    when (val done = firstRunDone) {
        null -> Box(Modifier.fillMaxSize().background(Deaftly.colors.ground))
        else -> AppNavigationHost(container, if (done) Route.Home else Route.FirstRun)
    }
}

@Composable
private fun AppNavigationHost(
    container: AppContainer,
    start: Route,
) {
    val backStack = rememberNavBackStack(start)
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
                entry<Route.FirstRun> {
                    val viewModel = viewModel<FirstRunViewModel>(factory = container.firstRunViewModelFactory)
                    FirstRunScreen(viewModel, onDone = { backStack[backStack.lastIndex] = Route.Home })
                }
                entry<Route.Home> { HubScreen(onNavigate = { backStack.add(it) }) }
                entry<Route.Templates> {
                    val viewModel = viewModel<HomeViewModel>(factory = container.homeViewModelFactory)
                    TemplatesScreen(viewModel, onNavigate = { backStack.add(it) }, onHome = { backStack.removeLastOrNull() })
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
                        highlights = route.highlights,
                        viewModel = viewModel,
                        onClose = { backStack.removeLastOrNull() },
                        onContinueAsChat = { backStack.add(Route.Chat(listOf(route.text))) },
                    )
                }
                conversationEntries(container, backStack)
                entry<Route.Ocr> { OcrEntry(it, container, backStack) }
                entry<Route.Settings> {
                    val viewModel = viewModel<SettingsViewModel>(factory = container.settingsViewModelFactory)
                    SettingsScreen(viewModel, onBack = { backStack.removeLastOrNull() })
                }
                helpEntries(container, backStack)
            },
    )
}

private fun EntryProviderScope<NavKey>.helpEntries(
    container: AppContainer,
    backStack: NavBackStack<NavKey>,
) {
    entry<Route.Help> {
        HelpTopicsScreen(
            container.helpRepository,
            onOpen = { backStack.add(Route.HelpPage(it)) },
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<Route.HelpPage> { route ->
        HelpPageScreen(
            route.slug,
            container.helpRepository,
            onOpenTopic = { backStack.add(Route.HelpPage(it)) },
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

private fun EntryProviderScope<NavKey>.conversationEntries(
    container: AppContainer,
    backStack: NavBackStack<NavKey>,
) {
    entry<Route.FaceToFace> {
        val viewModel = viewModel<FaceToFaceViewModel>(factory = container.faceToFaceViewModelFactory)
        FaceToFaceScreen(
            viewModel,
            onEnd = { backStack.removeLastOrNull() },
            onContinue = { backStack[backStack.lastIndex] = it },
        )
    }
    entry<Route.Chat> { route ->
        val viewModel = viewModel<ChatViewModel>(factory = container.chatViewModelFactory(ChatOpening(route.messages, route.draft)))
        ChatScreen(
            viewModel,
            onEnd = { backStack.removeLastOrNull() },
            onInsertTemplate = { backStack.add(Route.TemplatePicker) },
        )
    }
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
