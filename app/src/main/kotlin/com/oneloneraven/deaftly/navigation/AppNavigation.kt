package com.oneloneraven.deaftly.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.oneloneraven.deaftly.AppContainer
import com.oneloneraven.deaftly.feedback.FeedbackScreen
import com.oneloneraven.deaftly.feedback.FeedbackViewModel
import com.oneloneraven.deaftly.ui.chat.ChatOpening
import com.oneloneraven.deaftly.ui.chat.ChatScreen
import com.oneloneraven.deaftly.ui.chat.ChatViewModel
import com.oneloneraven.deaftly.ui.chat.TemplatePickerScreen
import com.oneloneraven.deaftly.ui.editor.EditorExit
import com.oneloneraven.deaftly.ui.editor.EditorScreen
import com.oneloneraven.deaftly.ui.editor.EditorViewModel
import com.oneloneraven.deaftly.ui.facetoface.FaceToFaceScreen
import com.oneloneraven.deaftly.ui.facetoface.FaceToFaceViewModel
import com.oneloneraven.deaftly.ui.fill.FillScreen
import com.oneloneraven.deaftly.ui.fill.FillViewModel
import com.oneloneraven.deaftly.ui.firstrun.FirstRunScreen
import com.oneloneraven.deaftly.ui.firstrun.FirstRunViewModel
import com.oneloneraven.deaftly.ui.help.HelpPageScreen
import com.oneloneraven.deaftly.ui.help.HelpTopicsScreen
import com.oneloneraven.deaftly.ui.home.HomeViewModel
import com.oneloneraven.deaftly.ui.home.TemplatesScreen
import com.oneloneraven.deaftly.ui.hub.HubScreen
import com.oneloneraven.deaftly.ui.listen.ListenScreen
import com.oneloneraven.deaftly.ui.listen.ListenViewModel
import com.oneloneraven.deaftly.ui.ocr.OcrOutcome
import com.oneloneraven.deaftly.ui.ocr.OcrScreen
import com.oneloneraven.deaftly.ui.savedchats.SavedChatsScreen
import com.oneloneraven.deaftly.ui.savedchats.SavedChatsViewModel
import com.oneloneraven.deaftly.ui.settings.SettingsScreen
import com.oneloneraven.deaftly.ui.settings.SettingsViewModel
import com.oneloneraven.deaftly.ui.share.ShareScreen
import com.oneloneraven.deaftly.ui.share.shareChooser
import com.oneloneraven.deaftly.ui.show.ShowScreen
import com.oneloneraven.deaftly.ui.show.ShowViewModel
import com.oneloneraven.deaftly.ui.theme.Deaftly
import com.oneloneraven.deaftly.ui.typeshow.TypeShowScreen
import kotlinx.coroutines.flow.first

@Composable
fun AppNavigation(
    container: AppContainer,
    shortcut: Route? = null,
    onShortcutHandled: () -> Unit = {},
) {
    val firstRunDone by produceState<Boolean?>(null) { value = container.settingsRepository.firstRunDone.first() }
    when (val done = firstRunDone) {
        null -> Box(Modifier.fillMaxSize().background(Deaftly.colors.ground))
        else -> AppNavigationHost(container, if (done) Route.Home else Route.FirstRun, shortcut, onShortcutHandled)
    }
}

@Composable
private fun OpenShortcut(
    backStack: NavBackStack<NavKey>,
    shortcut: Route?,
    onHandled: () -> Unit,
) {
    LaunchedEffect(shortcut) {
        if (shortcut == null) return@LaunchedEffect
        if (backStack.firstOrNull() == Route.Home && backStack.lastOrNull() != shortcut) backStack.add(shortcut)
        onHandled()
    }
}

@Composable
private fun AppNavigationHost(
    container: AppContainer,
    start: Route,
    shortcut: Route?,
    onShortcutHandled: () -> Unit,
) {
    val backStack = rememberNavBackStack(start)
    OpenShortcut(backStack, shortcut, onShortcutHandled)
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
                homeEntry(container, backStack)
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
                shareEntry(backStack)
            },
    )
}

private fun EntryProviderScope<NavKey>.homeEntry(
    container: AppContainer,
    backStack: NavBackStack<NavKey>,
) {
    entry<Route.Home> {
        val savedChats by container.savedChatStore.summaries.collectAsStateWithLifecycle()
        val savedChatsLoaded by container.savedChatStore.loaded.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val canPin = remember(context) { ShortcutManagerCompat.isRequestPinShortcutSupported(context) }
        HubScreen(
            onNavigate = { backStack.add(it) },
            hasSavedChats = savedChats.isNotEmpty(),
            savedChatsLoaded = savedChatsLoaded,
            addToHomeScreen = if (canPin) { route -> AppShortcut.forRoute(route)?.requestPin(context) } else null,
        )
    }
}

private fun EntryProviderScope<NavKey>.shareEntry(backStack: NavBackStack<NavKey>) {
    entry<Route.Share> {
        val context = LocalContext.current
        ShareScreen(
            onShare = { context.startActivity(shareChooser(it)) },
            onBack = { backStack.removeLastOrNull() },
        )
    }
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
            onFeedback = { backStack.add(Route.Feedback(it)) },
        )
    }
    entry<Route.Feedback> { route ->
        val viewModel = viewModel<FeedbackViewModel>(factory = container.feedbackViewModelFactory(route.kind))
        FeedbackScreen(viewModel, container.appVersion, onDone = { backStack.removeLastOrNull() })
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
    entry<Route.SavedChats> {
        val viewModel = viewModel<SavedChatsViewModel>(factory = container.savedChatsViewModelFactory)
        SavedChatsScreen(
            viewModel,
            onOpen = { backStack.add(Route.Chat(savedChatId = it)) },
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<Route.FaceToFace> {
        val viewModel = viewModel<FaceToFaceViewModel>(factory = container.faceToFaceViewModelFactory)
        FaceToFaceScreen(
            viewModel,
            onEnd = { backStack.removeLastOrNull() },
            onContinue = { backStack[backStack.lastIndex] = it },
        )
    }
    entry<Route.Listen> {
        val viewModel = viewModel<ListenViewModel>(factory = container.listenViewModelFactory)
        ListenScreen(
            viewModel,
            onEnd = { backStack.removeLastOrNull() },
            onContinue = { backStack[backStack.lastIndex] = it },
        )
    }
    entry<Route.Chat> { route ->
        val viewModel =
            viewModel<ChatViewModel>(
                factory = container.chatViewModelFactory(ChatOpening(route.messages, route.draft, route.savedChatId, route.heard)),
            )
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
