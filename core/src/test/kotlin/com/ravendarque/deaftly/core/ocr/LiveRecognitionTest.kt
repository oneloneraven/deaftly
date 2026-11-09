package com.ravendarque.deaftly.core.ocr

import kotlinx.coroutines.test.runTest
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LiveRecognitionTest {
    private fun box(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) = BoundingBox(left, top, right, bottom)

    private fun line(
        text: String,
        area: BoundingBox,
        words: List<TextWord> = emptyList(),
    ) = TextLine(text, area, words, corners = area.toQuad())

    private fun result(vararg lines: TextLine) = RecognitionResult(lines.map { TextBlock(listOf(it), it.boundingBox) })

    private fun frame(
        width: Int,
        height: Int,
        rotation: Int = 0,
    ) = RecognitionFrame(width, height, rotation, PixelFormat.RGBA_8888, ByteBuffer.allocate(width * height * 4))

    private fun phrase(
        text: String,
        area: BoundingBox,
    ) = DetectedPhrase(text, area.toQuad(), listOf(PhraseWord(text, area.toQuad())))

    @Test
    fun viewportMappingScalesTheVisibleAreaToTheUnitSquare() {
        val mapping = ViewportMapping(box(10f, 20f, 110f, 220f))

        assertEquals(Point(0f, 0f), mapping.toViewport(Point(10f, 20f)))
        assertEquals(Point(0.5f, 0.5f), mapping.toViewport(Point(60f, 120f)))
        assertEquals(Point(1f, 1f), mapping.toViewport(Point(110f, 220f)))
    }

    @Test
    fun theCropOfARotatedFrameIsTurnedUprightBeforeMapping() {
        val rotated = frame(8, 4, rotation = 90)
        val mapping = ViewportMapping.of(rotated, box(0f, 0f, 8f, 2f))

        assertEquals(Point(0f, 0.25f), mapping.toViewport(Point(2f, 2f)))
        assertEquals(Point(1f, 1f), mapping.toViewport(Point(4f, 8f)))
    }

    @Test
    fun withoutACropTheWholeUprightFrameIsVisible() {
        val mapping = ViewportMapping.of(frame(8, 4, rotation = 90), null)

        assertEquals(Point(0.5f, 0.5f), mapping.toViewport(Point(2f, 4f)))
    }

    @Test
    fun recognisedLinesBecomePhrasesWithWordsInViewportUnits() =
        runTest {
            val area = box(10f, 10f, 50f, 20f)
            val word = TextWord("Soup", box(10f, 10f, 30f, 20f))
            val fake = FakeTextRecogniser(result(line("Soup 4.50", area, listOf(word))))
            val phrases = LiveRecognition(fake).process(frame(100, 100))

            val phrase = phrases.single()
            assertEquals("Soup 4.50", phrase.text)
            assertEquals(box(0.1f, 0.1f, 0.5f, 0.2f), phrase.quad.boundingBox())
            assertEquals(listOf("Soup"), phrase.words.map { it.text })
            assertEquals(
                box(0.1f, 0.1f, 0.3f, 0.2f),
                phrase.words
                    .single()
                    .quad
                    .boundingBox(),
            )
        }

    @Test
    fun aLineWithoutWordsGetsOneWordCoveringIt() =
        runTest {
            val fake = FakeTextRecogniser(result(line("Lasagne", box(0f, 0f, 50f, 10f))))

            val phrase = LiveRecognition(fake).process(frame(100, 100)).single()

            assertEquals(listOf("Lasagne"), phrase.words.map { it.text })
        }

    @Test
    fun blankLinesAreIgnored() =
        runTest {
            val fake = FakeTextRecogniser(result(line("  ", box(0f, 0f, 5f, 5f)), line("Tea", box(0f, 10f, 30f, 20f))))

            assertEquals(listOf("Tea"), LiveRecognition(fake).process(frame(100, 100)).map { it.text })
        }

    @Test
    fun shapesAndLowConfidenceDetectionsNeverBecomePhrases() =
        runTest {
            val fake =
                FakeTextRecogniser(
                    result(
                        line("Tea", box(0f, 10f, 30f, 20f)).copy(confidence = 0.95f),
                        line("|", box(40f, 10f, 42f, 30f)).copy(confidence = 0.9f),
                        line("-- ~", box(50f, 10f, 80f, 14f)).copy(confidence = 0.9f),
                        line("lO0", box(10f, 40f, 40f, 50f)).copy(confidence = 0.1f),
                        line("Coffee", box(0f, 60f, 40f, 70f)).copy(confidence = 0.9f),
                    ),
                )

            val phrases = LiveRecognition(fake).process(frame(100, 100))

            assertEquals(listOf("Tea", "Coffee"), phrases.map { it.text })
        }

    @Test
    fun aPhraseKeepsItsIdWhileItDriftsAndIsSmoothed() {
        val tracker = PhraseTracker(smoothing = 0.5f)
        val first = tracker.update(listOf(phrase("Tea", box(0f, 0f, 0.4f, 0.1f)))).single()

        val second = tracker.update(listOf(phrase("Tea", box(0.02f, 0f, 0.42f, 0.1f)))).single()

        assertEquals(first.id, second.id)
        assertEquals(0.01f, second.quad.boundingBox().left, 1e-6f)
    }

    @Test
    fun aPhraseThatMovesAwayGetsANewIdAndTheOldOneIsKeptBriefly() {
        val tracker = PhraseTracker(maxMisses = 1)
        val first = tracker.update(listOf(phrase("Tea", box(0f, 0f, 0.4f, 0.1f)))).single()

        val moved = tracker.update(listOf(phrase("Tea", box(0.5f, 0.5f, 0.9f, 0.6f))))
        assertEquals(2, moved.size)
        assertNotEquals(first.id, moved.first().id)
        assertEquals(first.id, moved.last().id)

        val later = tracker.update(listOf(phrase("Tea", box(0.5f, 0.5f, 0.9f, 0.6f))))
        assertEquals(1, later.size)
    }

    @Test
    fun aMissedPhraseReturnsWithItsOldIdIfItComesBackInTime() {
        val tracker = PhraseTracker(maxMisses = 2)
        val first = tracker.update(listOf(phrase("Tea", box(0f, 0f, 0.4f, 0.1f)))).single()
        tracker.update(emptyList())

        val back = tracker.update(listOf(phrase("Tea", box(0f, 0f, 0.4f, 0.1f)))).single()

        assertEquals(first.id, back.id)
    }

    @Test
    fun twoDetectionsNeverShareOnePhrase() {
        val tracker = PhraseTracker()
        tracker.update(listOf(phrase("Tea", box(0f, 0f, 0.4f, 0.1f))))

        val both =
            tracker.update(
                listOf(phrase("Tea", box(0f, 0f, 0.4f, 0.1f)), phrase("Tea", box(0f, 0.005f, 0.4f, 0.105f))),
            )

        assertEquals(2, both.map { it.id }.toSet().size)
        assertTrue(both.size >= 2)
    }

    @Test
    fun newTextOnAMatchedPhraseReplacesTheOldText() {
        val tracker = PhraseTracker()
        tracker.update(listOf(phrase("Te4", box(0f, 0f, 0.4f, 0.1f))))

        assertEquals("Tea", tracker.update(listOf(phrase("Tea", box(0f, 0f, 0.4f, 0.1f)))).single().text)
    }
}
