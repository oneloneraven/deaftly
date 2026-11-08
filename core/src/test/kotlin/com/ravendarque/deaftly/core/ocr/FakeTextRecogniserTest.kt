package com.ravendarque.deaftly.core.ocr

import kotlinx.coroutines.test.runTest
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FakeTextRecogniserTest {
    private val frame = RecognitionFrame(2, 2, 0, PixelFormat.RGBA_8888, ByteBuffer.allocate(16))

    private fun result(text: String): RecognitionResult {
        val box = BoundingBox(0f, 0f, 1f, 1f)
        val word = TextWord(text, box, confidence = 0.9f)
        val line = TextLine(text, box, listOf(word), corners = box.toQuad(), angleDegrees = 0f)
        return RecognitionResult(listOf(TextBlock(listOf(line), box)))
    }

    @Test
    fun returnsScriptedResultsInOrderThenRepeatsTheLast() =
        runTest {
            val fake = FakeTextRecogniser(listOf(result("one"), result("two")))
            assertEquals("one", fake.recognise(frame).text)
            assertEquals("two", fake.recognise(frame).text)
            assertEquals("two", fake.recognise(frame).text)
            assertEquals(3, fake.frames.size)
        }

    @Test
    fun singleResultConstructorAlwaysReturnsIt() =
        runTest {
            val fake = FakeTextRecogniser(result("menu"))
            assertEquals("menu", fake.recognise(frame).text)
            assertEquals("menu", fake.recognise(frame).text)
        }

    @Test
    fun refusesFramesOnceClosed() =
        runTest {
            val fake = FakeTextRecogniser(RecognitionResult.EMPTY)
            fake.close()
            assertTrue(fake.isClosed)
            assertFailsWith<IllegalStateException> { fake.recognise(frame) }
        }

    @Test
    fun resultTextJoinsBlocksAndLines() {
        val box = BoundingBox(0f, 0f, 1f, 1f)
        val block = TextBlock(listOf(TextLine("a", box), TextLine("b", box)), box)
        assertEquals("a\nb\na\nb", RecognitionResult(listOf(block, block)).text)
    }

    @Test
    fun emptyScriptIsRejected() {
        assertFailsWith<IllegalArgumentException> { FakeTextRecogniser(emptyList()) }
    }
}
