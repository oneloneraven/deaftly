package com.ravendarque.deaftly.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.AppContainer
import com.ravendarque.deaftly.DeaftlyApplication
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.ui.chat.isMessageField
import com.ravendarque.deaftly.ui.ocr.OcrOutcome
import com.ravendarque.deaftly.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val LOAD_TIMEOUT_MILLIS = 10_000L
private const val EXTRA_TEMPLATES = 12

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1600dp")
class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun appContainer(): AppContainer = (composeRule.activity.application as DeaftlyApplication).container

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun text(
        id: Int,
        vararg args: Any,
    ) = composeRule.activity.getString(id, *args)

    private fun showApp(
        container: AppContainer = appContainer(),
        firstLaunch: Boolean = false,
    ) {
        if (!firstLaunch) runBlocking { container.settingsRepository.setFirstRunDone() }
        composeRule.setContent { AppTheme { AppNavigation(container) } }
        val landing = if (firstLaunch) R.string.first_run_start else R.string.hub_start_typing
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(landing)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openTemplates() {
        composeRule.onNodeWithText(text(R.string.hub_use_template)).performClick()
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.home_category_all)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertOnHub() {
        composeRule.onNodeWithText(text(R.string.hub_show_heading)).assertExists()
        composeRule.onNodeWithText(text(R.string.hub_chat_heading)).assertExists()
    }

    private fun pressSystemBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun theFirstLaunchShowsTheWelcomeAndGetStartedGoesHomeWithoutReturning() {
        composeRule.enableAccessibilityChecks()
        showApp(firstLaunch = true)

        composeRule.onNodeWithText(text(R.string.first_run_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.first_run_privacy)).assertExists()
        composeRule.onNodeWithText(text(R.string.hub_start_typing)).assertDoesNotExist()
        composeRule.onRoot().tryPerformAccessibilityChecks()

        composeRule.onNodeWithText(text(R.string.first_run_start)).performClick()
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.hub_start_typing)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text(R.string.first_run_title)).assertDoesNotExist()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.first_run_title)).assertDoesNotExist()
        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun theWelcomeSavesTheChoicesAndIsNotShownAgain() {
        val container = appContainer()
        showApp(container, firstLaunch = true)

        composeRule.onNode(hasSetTextAction()).performTextInput("Sam")
        composeRule.onNodeWithText(text(R.string.first_run_start)).performClick()
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.hub_start_typing)).fetchSemanticsNodes().isNotEmpty()
        }

        assertEquals("Sam", runBlocking { container.settingsRepository.chatProfile.first() }.name)
        assertTrue(runBlocking { container.settingsRepository.firstRunDone.first() })
    }

    @Test
    fun laterLaunchesGoStraightToHome() {
        showApp()

        composeRule.onNodeWithText(text(R.string.first_run_title)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.first_run_start)).assertDoesNotExist()
    }

    @Test
    fun startChatOpensTheChatAndEndingReturnsHome() {
        composeRule.enableAccessibilityChecks()
        showApp()

        composeRule.onNodeWithText(text(R.string.hub_side_by_side)).performClick()
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.chat_empty)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(text(R.string.chat_setup)).assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()

        composeRule.onNodeWithContentDescription(text(R.string.chat_end)).performClick()
        composeRule.onNodeWithText(text(R.string.hub_side_by_side)).assertExists()
        composeRule.onNodeWithText(text(R.string.chat_empty)).assertDoesNotExist()
    }

    @Test
    fun endingAChatDiscardsItsMessages() {
        showApp()

        fun openChat() {
            composeRule.onNodeWithText(text(R.string.hub_side_by_side)).performClick()
            composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
                composeRule.onAllNodesWithText(text(R.string.chat_composer_hint)).fetchSemanticsNodes().isNotEmpty()
            }
        }

        openChat()
        composeRule.onNode(isMessageField).performTextInput("Hello there")
        composeRule.onNodeWithContentDescription(text(R.string.chat_send)).performClick()
        composeRule.onNodeWithContentDescription("Me: Hello there").assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.chat_end)).performClick()
        composeRule.onNodeWithText(text(R.string.chat_end_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.chat_end_confirm)).performClick()
        openChat()

        composeRule.onNodeWithText(text(R.string.chat_empty)).assertExists()
        composeRule.onNodeWithContentDescription("Me: Hello there").assertDoesNotExist()
    }

    private fun openChatWithAMessage() {
        composeRule.onNodeWithText(text(R.string.hub_side_by_side)).performClick()
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.chat_composer_hint)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(isMessageField).performTextInput("Hello there")
        composeRule.onNodeWithContentDescription(text(R.string.chat_send)).performClick()
        composeRule.onNodeWithContentDescription("Me: Hello there").assertExists()
    }

    @Test
    fun aFilledTemplateIsInsertedIntoTheComposerAndTheChatIsIntact() {
        composeRule.enableAccessibilityChecks()
        showApp()
        openChatWithAMessage()
        composeRule.onNode(isMessageField).performTextInput("Please wait. ")

        composeRule.onNodeWithContentDescription(text(R.string.chat_insert_template)).performClick()
        composeRule.onNodeWithText(text(R.string.chat_insert_template)).assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        composeRule.onAllNodesWithText(text(R.string.template_explain_deaf_title))[0].performClick()
        composeRule.onNodeWithText(text(R.string.fill_insert)).assertIsNotEnabled()
        composeRule.onNode(hasText("how to reply") and hasClickAction()).performClick()
        val option =
            composeRule.activity.resources
                .getStringArray(R.array.placeholder_how_to_reply_options)
                .first()
        composeRule.onNode(hasText(option) and isSelectable()).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.fill_insert)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Me: Hello there").assertExists()
        composeRule.onNode(isMessageField).assert(hasText("Please wait. I'm deaf. I can't hear you. Please $option."))
        composeRule.onNodeWithText(text(R.string.fill_insert)).assertDoesNotExist()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun templatesOpensAtTheTopOfALongList() {
        val container = appContainer()
        runBlocking {
            repeat(EXTRA_TEMPLATES) { container.templateRepository.add(Template("extra-$it", "Extra $it", "Everyday", "Extra $it.")) }
        }
        showApp(container)

        openTemplates()

        composeRule.onNodeWithText(text(R.string.home_category_all)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.templates_add_new)).assertDoesNotExist()
    }

    @Test
    fun aTemplateWithoutPlaceholdersIsInsertedStraightFromThePicker() {
        val container = appContainer()
        runBlocking { container.templateRepository.add(Template("thanks", "Say thanks", "Everyday", "Thank you very much.")) }
        showApp(container)
        openChatWithAMessage()

        composeRule.onNodeWithContentDescription(text(R.string.chat_insert_template)).performClick()
        composeRule
            .onAllNodes(
                hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
            ).onLast()
            .performScrollToNode(hasText("Say thanks"))
        composeRule.onAllNodesWithText("Say thanks")[0].performClick()
        composeRule.waitForIdle()

        composeRule.onNode(isMessageField).assert(hasText("Thank you very much."))
        composeRule.onNodeWithContentDescription("Me: Hello there").assertExists()
        composeRule.onNodeWithContentDescription("Me: Thank you very much.").assertDoesNotExist()
    }

    @Test
    fun backFromThePickerReturnsToTheChatUntouched() {
        showApp()
        openChatWithAMessage()

        composeRule.onNodeWithContentDescription(text(R.string.chat_insert_template)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Me: Hello there").assertExists()
    }

    @Test
    fun newTemplateOpensTheEditorAndCancelReturnsToTemplates() {
        showApp()
        openTemplates()

        composeRule.onNodeWithContentDescription(text(R.string.home_new_template)).performClick()
        composeRule.onNodeWithText(text(R.string.editor_title_new)).assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.editor_cancel)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.editor_title_new)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.home_category_all)).assertExists()
    }

    @Test
    fun typeAndShowKeepsTheTextAfterReturningFromShowUntilCleared() {
        composeRule.enableAccessibilityChecks()
        showApp()
        composeRule.onNodeWithText(text(R.string.hub_start_typing)).performClick()

        composeRule.onNodeWithContentDescription(text(R.string.type_show_label)).performTextInput("Table for two")
        composeRule.onNodeWithText(text(R.string.fill_show)).performClick()
        composeRule.onNodeWithText("Table for two").assertExists()
        composeRule.onNodeWithText(text(R.string.show_hint)).assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        composeRule.onNodeWithContentDescription(text(R.string.show_close)).performClick()

        composeRule.onNodeWithText("Table for two").assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.type_show_clear)).performClick()
        composeRule.onNodeWithText(text(R.string.type_show_placeholder)).assertExists()
        composeRule.onNodeWithText(text(R.string.fill_show)).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()
        composeRule.onNodeWithText(text(R.string.hub_side_by_side)).assertExists()
    }

    @Test
    fun continueAsChatFromShowOpensTheChatWithTheShownMessageAsTheDeafUsersFirstTurn() {
        composeRule.enableAccessibilityChecks()
        showApp()
        composeRule.onNodeWithText(text(R.string.hub_start_typing)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.type_show_label)).performTextInput("Table for two")
        composeRule.onNodeWithText(text(R.string.fill_show)).performClick()

        composeRule.onNodeWithText(text(R.string.show_continue_chat)).performClick()
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.chat_turn_prompt_other)).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Me: Table for two").assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.chat_turn_label)).assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        composeRule.onNodeWithContentDescription(text(R.string.chat_end)).performClick()
        composeRule.onNodeWithText(text(R.string.chat_end_confirm)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.show_close)).assertExists()
    }

    @Test
    fun scanTheMenuExplainsTheCameraAndTypeItInsteadReturnsToTheTypingField() {
        composeRule.enableAccessibilityChecks()
        showApp()
        openTemplates()
        composeRule.onAllNodesWithText(text(R.string.template_order_food_title))[0].performClick()
        composeRule.onNode(hasText("menu item") and hasClickAction()).performClick()

        composeRule.onNodeWithText(text(R.string.fill_scan_menu)).performClick()

        composeRule.onNodeWithText(text(R.string.ocr_permission_body)).assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        composeRule.onNodeWithText(text(R.string.ocr_type_instead)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.ocr_permission_body)).assertDoesNotExist()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun textAddedFromTheScannerFillsTheCameraPlaceholderOnReturning() {
        val container = appContainer()
        showApp(container)
        openTemplates()
        composeRule.onAllNodesWithText(text(R.string.template_order_food_title))[0].performClick()
        composeRule.onNode(hasText("menu item") and hasClickAction()).performClick()
        composeRule.onNodeWithText(text(R.string.fill_scan_menu)).performClick()
        composeRule.onNodeWithText(text(R.string.ocr_permission_body)).assertExists()

        container.ocrOutcomes.post(OcrOutcome.Text("builtin.order-food", "menu item", "Lasagne"))
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.ocr_permission_body)).assertDoesNotExist()
        composeRule.onNode(hasText("Lasagne") and hasClickAction()).assertExists()
    }

    @Test
    fun tappingATemplateOpensFill() {
        showApp()
        openTemplates()

        composeRule.onAllNodesWithText(text(R.string.template_explain_deaf_title))[0].performClick()

        composeRule.onNodeWithContentDescription(text(R.string.back)).assertExists()
        composeRule.onNodeWithText(text(R.string.fill_hint)).assertExists()
        composeRule.onNodeWithText(text(R.string.fill_show)).assertExists()
    }

    @Test
    fun systemBackPopsTheStack() {
        showApp()

        composeRule.onNodeWithContentDescription(text(R.string.route_settings)).performClick()
        composeRule.onNodeWithText(text(R.string.settings_me_heading)).assertExists()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.settings_me_heading)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.hub_side_by_side)).assertExists()
    }

    @Test
    fun settingsOpensFromHomeAndBackReturnsHome() {
        composeRule.enableAccessibilityChecks()
        showApp()
        composeRule.onNodeWithContentDescription(text(R.string.route_settings)).performClick()
        composeRule.onRoot().tryPerformAccessibilityChecks()

        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()

        composeRule.onNodeWithText(text(R.string.hub_side_by_side)).assertExists()
    }

    @Test
    fun faceToFaceContinuesSideBySideWithEachSentMessageFromTheDeafUserAndTheTurnPassedOn() {
        composeRule.enableAccessibilityChecks()
        showApp()
        composeRule.onNodeWithText(text(R.string.hub_face_to_face)).performClick()
        composeRule.waitForIdle()
        val field = composeRule.onNode(hasSetTextAction())

        field.performTextInput("Hello, I am deaf")
        composeRule.onNodeWithContentDescription(text(R.string.chat_send)).performClick()
        field.performTextInput("Could I have a coffee please")
        composeRule.onNodeWithContentDescription(text(R.string.chat_send)).performClick()
        field.performTextInput("And a")
        composeRule.onNodeWithText(text(R.string.f2f_continue)).performClick()
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.chat_turn_prompt_other)).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Me: Hello, I am deaf").assertExists()
        composeRule.onNodeWithContentDescription("Me: Could I have a coffee please").assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.chat_turn_label)).assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        composeRule.onNodeWithContentDescription(text(R.string.chat_end)).performClick()
        composeRule.onNodeWithText(text(R.string.chat_end_confirm)).performClick()
        composeRule.onNodeWithText(text(R.string.hub_face_to_face)).assertExists()
    }

    @Test
    fun faceToFaceWithNothingSentClosesStraightAwayAndWithMessagesAsksFirst() {
        showApp()
        composeRule.onNodeWithText(text(R.string.hub_face_to_face)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(text(R.string.f2f_close)).performClick()
        composeRule.onNodeWithText(text(R.string.hub_face_to_face)).assertExists()

        composeRule.onNodeWithText(text(R.string.hub_face_to_face)).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).performTextInput("Hello")
        composeRule.onNodeWithContentDescription(text(R.string.chat_send)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.f2f_close)).performClick()
        composeRule.onNodeWithText(text(R.string.chat_end_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.chat_end_keep)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.f2f_close)).assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.f2f_close)).performClick()
        composeRule.onNodeWithText(text(R.string.chat_end_confirm)).performClick()
        composeRule.onNodeWithText(text(R.string.hub_face_to_face)).assertExists()
    }

    @Test
    fun theHubOffersTheFourOptionsAndEachOpensItsScreenAndReturnsToTheHub() {
        composeRule.enableAccessibilityChecks()
        showApp()
        assertOnHub()
        composeRule.onRoot().tryPerformAccessibilityChecks()

        composeRule.onNodeWithText(text(R.string.hub_start_typing)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.type_show_label)).assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()
        assertOnHub()

        openTemplates()
        composeRule.onNodeWithText(text(R.string.templates_home)).performClick()
        assertOnHub()

        composeRule.onNodeWithText(text(R.string.hub_face_to_face)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.f2f_close)).performClick()
        assertOnHub()

        composeRule.onNodeWithText(text(R.string.hub_side_by_side)).performClick()
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.chat_empty)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(text(R.string.chat_end)).performClick()
        assertOnHub()
    }

    @Test
    fun theTemplatesScreenHasHomeAndNoSettingsAndSystemBackReturnsToTheHub() {
        composeRule.enableAccessibilityChecks()
        showApp()
        openTemplates()

        composeRule.onNodeWithText(text(R.string.templates_home)).assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.route_settings)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(text(R.string.home_new_template)).assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()

        pressSystemBack()

        assertOnHub()
        assertTrue(!composeRule.activity.isFinishing)
    }

    @Test
    fun systemBackFromTheHubLeavesTheApp() {
        showApp()

        pressSystemBack()

        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun showingATemplateReturnsToItsFillThenTemplatesThenTheHub() {
        val container = appContainer()
        runBlocking { container.templateRepository.add(Template("thanks", "Say thanks", "Everyday", "Thank you very much.")) }
        showApp(container)
        openTemplates()
        composeRule.onNode(hasSetTextAction()).performTextInput("thanks")

        composeRule.onAllNodesWithText("Say thanks")[0].performClick()
        composeRule.onNodeWithText(text(R.string.fill_show)).performClick()
        composeRule.onNodeWithText(text(R.string.show_hint)).assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.show_close)).performClick()
        composeRule.onNodeWithText(text(R.string.fill_show)).assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()
        composeRule.onNodeWithText(text(R.string.home_category_all)).assertExists()
        composeRule.onNodeWithText(text(R.string.templates_home)).performClick()

        assertOnHub()
    }

    @Test
    fun routesRoundTripThroughSerialisation() {
        val routes: List<Route> =
            listOf(
                Route.Home,
                Route.Templates,
                Route.Fill("template-1"),
                Route.Fill("template-1", forChat = true),
                Route.TemplatePicker,
                Route.TypeShow,
                Route.Show("Hello"),
                Route.Show("Hello Sam", listOf(HighlightRange(6, 9))),
                Route.Chat(),
                Route.Chat(listOf("Hello", "Could I have a coffee"), "And a"),
                Route.FaceToFace,
                Route.Editor("template-1"),
                Route.Editor(),
                Route.Ocr("order-food", "menu item"),
                Route.Settings,
                Route.FirstRun,
            )

        routes.forEach { route ->
            val json = Json.encodeToString(Route.serializer(), route)
            assertEquals(route, Json.decodeFromString(Route.serializer(), json))
        }
    }
}
