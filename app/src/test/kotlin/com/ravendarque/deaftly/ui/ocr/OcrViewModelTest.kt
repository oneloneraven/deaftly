package com.ravendarque.deaftly.ui.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.FakeTextRecogniser
import com.ravendarque.deaftly.core.ocr.PixelFormat
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import com.ravendarque.deaftly.core.ocr.RecognitionResult
import com.ravendarque.deaftly.core.ocr.TextBlock
import com.ravendarque.deaftly.core.ocr.TextLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
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
    private val fake = FakeTextRecogniser(result)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
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
                    OcrViewModel {
                        release.await()
                        fake
                    } as T
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

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
