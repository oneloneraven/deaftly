package com.ravendarque.deaftly.ui.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.FakeTextRecogniser
import com.ravendarque.deaftly.core.ocr.OcrSelection
import com.ravendarque.deaftly.core.ocr.Phrase
import com.ravendarque.deaftly.core.ocr.PixelFormat
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import com.ravendarque.deaftly.core.ocr.RecognitionResult
import com.ravendarque.deaftly.core.ocr.TextBlock
import com.ravendarque.deaftly.core.ocr.TextLine
import com.ravendarque.deaftly.core.ocr.TextSelection
import com.ravendarque.deaftly.core.ocr.WordRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OcrViewModelTest {
    private val store = ViewModelStore()
    private val box = BoundingBox(10f, 10f, 60f, 20f)
    private val result = RecognitionResult(listOf(TextBlock(listOf(TextLine("Lasagne", box, corners = box.toQuad())), box)))
    private val lower = BoundingBox(10f, 40f, 60f, 50f)
    private var scripted: RecognitionResult? = null
    private val fake by lazy { FakeTextRecogniser(scripted ?: result) }
    private var captured = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel(release: CountDownLatch = CountDownLatch(0)): OcrViewModel {
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T =
                    OcrViewModel(
                        createRecogniser = {
                            release.await()
                            fake
                        },
                        loadFieldLabel = { "Menu item" },
                        captureFrame = { _, _ ->
                            captured++
                            null
                        },
                    ) as T
            }
        return ViewModelProvider.create(store, factory)[OcrViewModel::class]
    }

    private fun frame() = RecognitionFrame(100, 100, 0, PixelFormat.RGBA_8888, ByteBuffer.allocate(100 * 100 * 4))

    private fun OcrViewModel.awaitReady() = runBlocking { withTimeout(TIMEOUT_MILLIS) { state.first { it.ready } } }

    @Test
    fun framesAreIgnoredUntilTheRecogniserIsReady() {
        val gate = CountDownLatch(1)
        val viewModel = viewModel(gate)

        viewModel.analyse(frame(), null)
        gate.countDown()

        assertTrue(
            viewModel.state.value.phrases
                .isEmpty(),
        )
        assertEquals(0, fake.frames.size)
    }

    @Test
    fun anAnalysedFrameShowsItsPhrasesInViewportUnits() {
        val viewModel = viewModel()
        viewModel.awaitReady()

        viewModel.analyse(frame(), null)

        val phrase =
            viewModel.state.value.phrases
                .single()
        assertEquals("Lasagne", phrase.text)
        assertEquals(BoundingBox(0.1f, 0.1f, 0.6f, 0.2f), phrase.quad.boundingBox())
    }

    @Test
    fun theSamePhraseKeepsItsIdAcrossFrames() {
        val viewModel = viewModel()
        viewModel.awaitReady()

        viewModel.analyse(frame(), null)
        val first =
            viewModel.state.value.phrases
                .single()
                .id
        viewModel.analyse(frame(), null)

        assertEquals(
            first,
            viewModel.state.value.phrases
                .single()
                .id,
        )
    }

    @Test
    fun clearingClosesTheRecogniserAndLaterFramesAreDropped() {
        val viewModel = viewModel()
        viewModel.awaitReady()

        store.clear()
        viewModel.analyse(frame(), null)

        assertTrue(fake.isClosed)
        assertFalse(
            viewModel.state.value.phrases
                .isNotEmpty(),
        )
    }

    private val twoLines =
        RecognitionResult(
            listOf(
                TextBlock(listOf(TextLine("Soup of the day", box, corners = box.toQuad())), box),
                TextBlock(listOf(TextLine("Garlic bread", lower, corners = lower.toQuad())), lower),
            ),
        )

    private fun OcrViewModel.analyseTwoLines(): List<Phrase> {
        awaitReady()
        analyse(frame(), null)
        return state.value.phrases
    }

    @Test
    fun tappingAPhraseSelectsItAndFillsTheTrayWithItsText() {
        val viewModel = viewModel()
        viewModel.awaitReady()
        viewModel.analyse(frame(), null)
        val phrase =
            viewModel.state.value.phrases
                .single()

        viewModel.togglePhrase(phrase.id)

        assertEquals("Lasagne", viewModel.state.value.fieldText)
        assertEquals(OcrSelection().toggled(phrase), viewModel.state.value.selection)
    }

    @Test
    fun tappingLinesBuildsTheTextInTapOrderAndTappingAgainRemovesOne() {
        scripted = twoLines
        val viewModel = viewModel()
        val (soup, bread) = viewModel.analyseTwoLines()

        viewModel.togglePhrase(bread.id)
        viewModel.togglePhrase(soup.id)
        assertEquals("Garlic bread Soup of the day", viewModel.state.value.fieldText)

        viewModel.togglePhrase(bread.id)
        assertEquals("Soup of the day", viewModel.state.value.fieldText)
        assertTrue(
            viewModel.state.value.selection
                .isTapped(soup.id),
        )
        assertFalse(
            viewModel.state.value.selection
                .isTapped(bread.id),
        )
    }

    @Test
    fun deselectingEveryLineEmptiesTheText() {
        val viewModel = viewModel()
        viewModel.awaitReady()
        viewModel.analyse(frame(), null)
        val id =
            viewModel.state.value.phrases
                .single()
                .id

        viewModel.togglePhrase(id)
        viewModel.togglePhrase(id)

        assertEquals("", viewModel.state.value.fieldText)
        assertTrue(viewModel.state.value.selection.isEmpty)
    }

    @Test
    fun aDragReplacesTheTappedLinesAndATapAfterwardsStartsAgain() {
        scripted = twoLines
        val viewModel = viewModel()
        val (soup, bread) = viewModel.analyseTwoLines()
        viewModel.togglePhrase(soup.id)

        viewModel.selectRange(TextSelection(WordRef(bread.id, 0), WordRef(bread.id, 0)))

        assertEquals("Garlic bread", viewModel.state.value.fieldText)
        assertTrue(
            viewModel.state.value.selection.lines
                .isEmpty(),
        )

        viewModel.togglePhrase(soup.id)

        assertEquals("Soup of the day", viewModel.state.value.fieldText)
        assertEquals(null, viewModel.state.value.selection.range)
    }

    @Test
    fun clearingEmptiesTheTextAndDeselectsEverything() {
        scripted = twoLines
        val viewModel = viewModel()
        val (soup, bread) = viewModel.analyseTwoLines()
        viewModel.togglePhrase(soup.id)
        viewModel.togglePhrase(bread.id)

        viewModel.clearText()

        assertEquals("", viewModel.state.value.fieldText)
        assertTrue(viewModel.state.value.selection.isEmpty)
    }

    @Test
    fun clearingAlsoDropsATypedEditAndADraggedRange() {
        val viewModel = viewModel()
        viewModel.awaitReady()
        viewModel.analyse(frame(), null)
        val id =
            viewModel.state.value.phrases
                .single()
                .id
        viewModel.selectRange(TextSelection(WordRef(id, 0), WordRef(id, 0)))
        viewModel.editText("Lasagne verde")

        viewModel.clearText()

        assertEquals("", viewModel.state.value.fieldText)
        assertTrue(viewModel.state.value.selection.isEmpty)
    }

    @Test
    fun tappingNothingClearsTheSelectionButKeepsTheEditedText() {
        val viewModel = viewModel()
        viewModel.awaitReady()
        viewModel.analyse(frame(), null)
        viewModel.togglePhrase(
            viewModel.state.value.phrases
                .single()
                .id,
        )
        viewModel.editText("Lasagne verde")

        viewModel.togglePhrase(null)

        assertTrue(viewModel.state.value.selection.isEmpty)
        assertEquals("Lasagne verde", viewModel.state.value.fieldText)
    }

    @Test
    fun selectingAPhraseThatIsNotThereChangesNothing() {
        val viewModel = viewModel()
        viewModel.awaitReady()
        viewModel.editText("typed")

        viewModel.togglePhrase(99)

        assertTrue(viewModel.state.value.selection.isEmpty)
        assertEquals("typed", viewModel.state.value.fieldText)
    }

    @Test
    fun aDraggedRangeSetsTheSelectionAndTheTrayText() {
        val viewModel = viewModel()
        viewModel.awaitReady()
        viewModel.analyse(frame(), null)
        val phrase =
            viewModel.state.value.phrases
                .single()
        val range = TextSelection(WordRef(phrase.id, 0), WordRef(phrase.id, 0))

        viewModel.selectRange(range)

        assertEquals(range, viewModel.state.value.selection.range)
        assertEquals("Lasagne", viewModel.state.value.fieldText)
    }

    @Test
    fun aRangeOverPhrasesThatAreGoneIsIgnored() {
        val viewModel = viewModel()
        viewModel.awaitReady()
        viewModel.editText("kept")

        viewModel.selectRange(TextSelection(WordRef(42, 0), WordRef(42, 1)))

        assertTrue(viewModel.state.value.selection.isEmpty)
        assertEquals("kept", viewModel.state.value.fieldText)
    }

    @Test
    fun freezingHoldsTheNextFrameAndItsPhrasesAndStopsAnalysis() {
        val viewModel = viewModel()
        viewModel.awaitReady()
        viewModel.analyse(frame(), null)

        viewModel.toggleFreeze()
        assertTrue(viewModel.state.value.freezeRequested)
        viewModel.analyse(frame(), null)

        val frozen = viewModel.state.value
        assertTrue(frozen.frozen)
        assertFalse(frozen.freezeRequested)
        assertEquals(listOf("Lasagne"), frozen.phrases.map { it.text })
        assertEquals(1, captured)
        val seen = fake.frames.size
        viewModel.analyse(frame(), null)
        assertEquals(seen, fake.frames.size)
    }

    @Test
    fun unfreezingResumesAnalysisAndDropsTheImage() {
        val viewModel = viewModel()
        viewModel.awaitReady()
        viewModel.toggleFreeze()
        viewModel.analyse(frame(), null)

        viewModel.toggleFreeze()
        viewModel.analyse(frame(), null)

        assertFalse(viewModel.state.value.frozen)
        assertEquals(null, viewModel.state.value.frozenImage)
        assertEquals(2, fake.frames.size)
    }

    @Test
    fun aFreezeRequestCanBeCancelledBeforeAFrameArrives() {
        val viewModel = viewModel()
        viewModel.awaitReady()

        viewModel.toggleFreeze()
        viewModel.toggleFreeze()
        viewModel.analyse(frame(), null)

        assertFalse(viewModel.state.value.frozen)
        assertEquals(0, captured)
    }

    @Test
    fun theTorchToggleOnlyWorksWhereThereIsAFlash() {
        val viewModel = viewModel()
        viewModel.toggleTorch()
        viewModel.setTorchAvailable(false)
        assertFalse(viewModel.state.value.torchOn)

        viewModel.setTorchAvailable(true)
        viewModel.toggleTorch()
        assertTrue(viewModel.state.value.torchOn)
        viewModel.setTorchAvailable(false)
        assertFalse(viewModel.state.value.torchOn)
        assertFalse(viewModel.state.value.torchAvailable)
    }

    @Test
    fun theTrayIsLabelledWithThePlaceholdersLabelOnceLoaded() {
        val viewModel = viewModel()

        val label = runBlocking { withTimeout(TIMEOUT_MILLIS) { viewModel.state.first { it.fieldLabel != null } } }

        assertEquals("Menu item", label.fieldLabel)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
