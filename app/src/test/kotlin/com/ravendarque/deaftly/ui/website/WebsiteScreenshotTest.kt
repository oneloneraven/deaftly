package com.ravendarque.deaftly.ui.website

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.RichText
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.OcrSelection
import com.ravendarque.deaftly.core.ocr.Phrase
import com.ravendarque.deaftly.core.ocr.PhraseWord
import com.ravendarque.deaftly.core.ocr.Point
import com.ravendarque.deaftly.core.ocr.Quad
import com.ravendarque.deaftly.core.ocr.ViewportMapping
import com.ravendarque.deaftly.core.settings.PersonalNames
import com.ravendarque.deaftly.ui.chat.ChatCodec
import com.ravendarque.deaftly.ui.chat.ChatScreen
import com.ravendarque.deaftly.ui.chat.ChatViewModel
import com.ravendarque.deaftly.ui.chat.FakeSavedChatStore
import com.ravendarque.deaftly.ui.chat.FakeSettingsRepository
import com.ravendarque.deaftly.ui.chat.silentAudio
import com.ravendarque.deaftly.ui.chat.silentRecogniser
import com.ravendarque.deaftly.ui.chat.testWriteScope
import com.ravendarque.deaftly.ui.facetoface.FaceToFaceContent
import com.ravendarque.deaftly.ui.fill.FillScreen
import com.ravendarque.deaftly.ui.fill.FillViewModel
import com.ravendarque.deaftly.ui.fill.fillTemplates
import com.ravendarque.deaftly.ui.home.FakeTemplateRepository
import com.ravendarque.deaftly.ui.hub.HubScreen
import com.ravendarque.deaftly.ui.ocr.OcrActions
import com.ravendarque.deaftly.ui.ocr.OcrCameraContent
import com.ravendarque.deaftly.ui.ocr.OcrContent
import com.ravendarque.deaftly.ui.settings.SettingsScreen
import com.ravendarque.deaftly.ui.settings.SettingsViewModel
import com.ravendarque.deaftly.ui.show.ShowContent
import com.ravendarque.deaftly.ui.theme.AppTheme
import com.ravendarque.deaftly.ui.theme.defaultShowScheme
import com.ravendarque.deaftly.ui.typeshow.TypeShowContent
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

private const val SCREENSHOT_DIR = "src/test/screenshots"
private const val PACKAGE = "com.ravendarque.deaftly.ui.website"
private const val FIXTURES = "src/androidTest/assets/ocr-fixtures"
private const val MESSAGE = "Could I have a table for two by the window, please?"
private const val ORDER = "Hi! I'm deaf. Could I have Pasta arrabbiata please? If you have a question, please type it on my phone."
private const val VIEWPORT_WIDTH = 390f
private const val VIEWPORT_HEIGHT = 600f
internal val EXAMPLE_NAMES = PersonalNames(firstName = "Keisha", lastName = "Grant", useFirstNameInChats = true)
private val PRICE = Regex("^£\\d+(\\.\\d{2})?$")

private class Fixture(
    val image: File,
    val lines: List<JSONObject>,
)

private fun fixture(id: String): Fixture {
    val json = JSONObject(File("$FIXTURES/$id.json").readText())
    val lines = json.getJSONArray("lines")
    return Fixture(File("$FIXTURES/$id.jpg"), (0 until lines.length()).map { lines.getJSONObject(it) })
}

private fun JSONArray.points() =
    (0 until length()).map { index ->
        getJSONArray(index).let { point -> Point(point.getDouble(0).toFloat(), point.getDouble(1).toFloat()) }
    }

private fun JSONObject.isPrice() = PRICE.matches(getString("text"))

private fun JSONObject.toPhrase(
    id: Long,
    mapping: ViewportMapping,
): Phrase {
    val words =
        getJSONArray("words").let { array ->
            (0 until array.length()).map { array.getJSONObject(it) }
        }
    val quads = words.map { mapping.toViewport(Quad(it.getJSONArray("polygon").points())) }
    val first = quads.first().points
    val last = quads.last().points
    return Phrase(
        id,
        getString("text"),
        Quad(listOf(first[0], last[1], last[2], first[3])),
        words.mapIndexed { index, word -> PhraseWord(word.getString("text"), quads[index]) },
    )
}

private fun Fixture.itemPhrases(
    mapping: ViewportMapping,
    isItem: (index: Int) -> Boolean,
): List<Phrase> = lines.indices.filter(isItem).mapIndexed { order, index -> lines[index].toPhrase(order + 1L, mapping) }

private fun viewportOf(
    left: Float,
    top: Float,
    width: Float,
): BoundingBox = BoundingBox(left, top, left + width, top + width * VIEWPORT_HEIGHT / VIEWPORT_WIDTH)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h600dp")
open class WebsiteScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    protected fun shot(content: @Composable () -> Unit) {
        composeRule.setContent { AppTheme(darkTheme = false) { content() } }
        composeRule.waitForIdle()
        capture()
    }

    protected fun capture() {
        composeRule.onRoot().captureRoboImage("$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png")
    }

    private fun camera(
        id: String,
        visible: BoundingBox,
        isItem: (Fixture, Int) -> Boolean,
        selected: String,
    ) {
        val source = fixture(id)
        val mapping = ViewportMapping(visible)
        val phrases = source.itemPhrases(mapping) { isItem(source, it) }
        val chosen = phrases.first { it.text == selected }
        val selection = OcrSelection().toggled(chosen)
        val frame = BitmapFactory.decodeFile(source.image.path).asImageBitmap()
        shot {
            OcrCameraContent(
                content = OcrContent(status = "Live", phrases = phrases, selection = selection, fieldText = selection.tappedText),
                actions = OcrActions({}, {}, {}, {}, {}, {}, {}, {}),
                viewfinder = { modifier ->
                    Canvas(modifier) {
                        drawImage(
                            frame,
                            srcOffset = IntOffset(visible.left.toInt(), visible.top.toInt()),
                            srcSize = IntSize(visible.width.toInt(), visible.height.toInt()),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        )
                    }
                },
            )
        }
    }

    private fun followedByPrice(
        source: Fixture,
        index: Int,
    ) = !source.lines[index].isPrice() && source.lines.getOrNull(index + 1)?.isPrice() == true

    @Test
    fun hub() = shot { HubScreen(onNavigate = {}) }

    @Test
    fun typeAndShow() = shot { TypeShowContent(TextFieldState(MESSAGE), onShow = {}, onBack = {}) }

    @Test
    fun show() =
        shot {
            ShowContent(
                MESSAGE,
                defaultShowScheme(false),
                onCycleScheme = {},
                onClose = {},
                onContinueAsChat = {},
                highlights = emptyList(),
            )
        }

    @Test
    fun faceToFace() {
        val typing = "Hi! I'm deaf, so I'll type. My name is Ellie Thompson. Do you need me to sign"
        val field = TextFieldState(typing, TextRange(typing.length))
        shot { FaceToFaceContent(emptyList(), field, onSend = {}, onEnd = {}, onContinue = {}) }
    }

    @Test
    fun fill() {
        val viewModel = FillViewModel("order-food", FakeTemplateRepository(fillTemplates), FakeSettingsRepository(), SavedStateHandle())
        composeRule.setContent {
            AppTheme(darkTheme = false) { FillScreen("order-food", viewModel, onNavigate = {}, onBack = {}) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("menu item").performClick()
        composeRule.onNodeWithText("Type it").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("Pasta arrabbiata")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()
        capture()
    }

    @Test
    fun cameraMenu() = camera("menu-trattoria", viewportOf(45f, 340f, 450f), ::followedByPrice, "Pasta arrabbiata")

    @Test
    fun sideBySide() {
        val conversation =
            Conversation(
                Participant(ParticipantId.A, ColourRole.Blue, "Me"),
                Participant(ParticipantId.B, ColourRole.Gold, "Waiter"),
            ).withTurn(ParticipantId.A)
                .append(RichText.spans(ORDER))
                .withTurn(ParticipantId.B)
                .append(RichText.spans("Sure! Do you have any allergies?"))
                .withTurn(ParticipantId.A)
                .append(RichText.spans("Yes, I'm allergic to peanuts."))
                .withTurn(ParticipantId.B)
                .append(RichText.spans("Got it, thanks. I'll make sure the kitchen is aware."))
        val viewModel =
            ChatViewModel(
                FakeSettingsRepository(),
                SavedStateHandle(mapOf("conversation" to ChatCodec.encode(conversation))),
                store = FakeSavedChatStore(),
                writeScope = testWriteScope,
                createSpeechRecogniser = ::silentRecogniser,
                audio = silentAudio(),
            )
        shot { ChatScreen(viewModel, onEnd = {}, onInsertTemplate = {}) }
    }

    @Test
    fun settings() {
        val settings = FakeSettingsRepository(ChatProfile("Me", ColourRole.Blue), names = EXAMPLE_NAMES)
        val viewModel = SettingsViewModel(settings, FakeTemplateRepository(emptyList(), emptyList()))
        shot { SettingsScreen(viewModel, onBack = {}) }
    }

    @Test
    fun cameraPharmacy() =
        camera(
            "label-antibiotic",
            viewportOf(185f, 125f, 530f),
            { source, index -> source.lines[index].getString("text") == "Flucloxacillin 500mg Capsules" },
            "Flucloxacillin 500mg Capsules",
        )
}
