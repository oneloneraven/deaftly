package com.ravendarque.deaftly.core.ocr.bench

import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.Point
import com.ravendarque.deaftly.core.ocr.Quad
import com.ravendarque.deaftly.core.ocr.RecognitionResult
import com.ravendarque.deaftly.core.ocr.TextBlock
import com.ravendarque.deaftly.core.ocr.TextLine
import com.ravendarque.deaftly.core.ocr.TextWord
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BenchMetricsTest {
    @Test
    fun editDistanceCountsInsertionsDeletionsAndSubstitutions() {
        assertEquals(3, TextMetrics.editDistance("kitten".toList(), "sitting".toList()))
        assertEquals(0, TextMetrics.editDistance(emptyList<Char>(), emptyList()))
        assertEquals(4, TextMetrics.editDistance(emptyList(), "abcd".toList()))
    }

    @Test
    fun characterErrorRateNormalisesWhitespaceAndUnicode() {
        val count = TextMetrics.characterErrors("Café  au\nlait", "Café au lait", ignoreCase = false)
        assertEquals(ErrorCount(0, 12), count)
    }

    @Test
    fun caseInsensitiveRateIgnoresCaseOnly() {
        assertEquals(3, TextMetrics.characterErrors("Fish", "FISH", ignoreCase = false).edits)
        assertEquals(0.0, TextMetrics.characterErrors("Fish", "FISH", ignoreCase = true).rate)
        assertEquals(0.5, TextMetrics.wordErrors("fish chips", "fish chop", ignoreCase = true).rate)
    }

    @Test
    fun wordBagIgnoresOrderAndLineSplitting() {
        val (chars, words) = TextMetrics.bagErrors("price 8.50 fish chips", "chips fish price 8.50", ignoreCase = false)
        assertEquals(ErrorCount(0, 18), chars)
        assertEquals(ErrorCount(0, 4), words)
    }

    @Test
    fun wordBagCountsSubstitutionsMissingAndExtraWords() {
        val (chars, words) = TextMetrics.bagErrors("fish and chips today", "chips fosh extra", ignoreCase = false)
        assertEquals(ErrorCount(1 + 3 + 5 + 5, 17), chars)
        assertEquals(ErrorCount(1 + 2 + 1, 4), words)
    }

    @Test
    fun emptyReferenceWithOutputIsFullError() {
        assertEquals(1.0, TextMetrics.characterErrors("", "x", ignoreCase = false).rate)
        assertEquals(0.0, TextMetrics.characterErrors("", "", ignoreCase = false).rate)
    }

    @Test
    fun quadIouIsOneForIdenticalAndHandlesRotationAndDisjoint() {
        val square = Quad(listOf(Point(0f, 0f), Point(10f, 0f), Point(10f, 10f), Point(0f, 10f)))
        val shifted = Quad(listOf(Point(5f, 0f), Point(15f, 0f), Point(15f, 10f), Point(5f, 10f)))
        val far = Quad(listOf(Point(50f, 50f), Point(60f, 50f), Point(60f, 60f), Point(50f, 60f)))
        assertEquals(1.0, PolygonIou.of(square, square), EPSILON)
        assertEquals(50.0 / 150.0, PolygonIou.of(square, shifted), EPSILON)
        assertEquals(0.0, PolygonIou.of(square, far), EPSILON)
        assertEquals(1.0, PolygonIou.of(square, Quad(square.points.reversed())), EPSILON)
        val diamond = Quad(listOf(Point(5f, -5f), Point(15f, 5f), Point(5f, 15f), Point(-5f, 5f)))
        assertEquals(100.0 / 200.0, PolygonIou.of(square, diamond), EPSILON)
    }

    @Test
    fun boxIouMatchesQuadIou() {
        val a = BoundingBox(0f, 0f, 10f, 10f)
        val b = BoundingBox(5f, 0f, 15f, 10f)
        assertEquals(PolygonIou.of(a.toQuad(), b.toQuad()), PolygonIou.of(a, b), EPSILON)
    }

    @Test
    fun percentilesUseNearestRank() {
        val values = (1..100).map { it.toDouble() }
        assertEquals(50.0, Stats.median(values))
        assertEquals(95.0, Stats.percentile(values, Stats.P95))
        assertEquals(7.0, Stats.median(listOf(7.0)))
    }

    @Test
    fun readingOrderReadsColumnsThenRows() {
        fun box(
            left: Float,
            top: Float,
        ) = BoundingBox(left, top, left + 100f, top + 20f)
        val lines = listOf(box(0f, 100f), box(200f, 0f), box(0f, 0f), box(200f, 100f), box(0f, 50f), box(200f, 50f))
        val sorted = ReadingOrder.sort(lines) { it.toQuad() }
        assertEquals(
            listOf(box(0f, 0f), box(0f, 50f), box(0f, 100f), box(200f, 0f), box(200f, 50f), box(200f, 100f)),
            sorted,
        )
    }

    @Test
    fun scorerMatchesLinesAndWordsByGeometry() {
        val truth =
            GroundTruth.parse(
                "t",
                """{"category":"menu","tags":["a"],"width":100,"height":50,"conditions":{"lighting":"gradient"},
                |"lines":[{"text":"Fish and chips","block":0,"polygon":[[0,0],[100,0],[100,20],[0,20]],"box":[0,0,100,20],
                |"words":[{"text":"Fish","polygon":[[0,0],[20,0],[20,20],[0,20]],"box":[0,0,20,20]},
                |{"text":"and","polygon":[[30,0],[50,0],[50,20],[30,20]],"box":[30,0,50,20]},
                |{"text":"chips","polygon":[[60,0],[100,0],[100,20],[60,20]],"box":[60,0,100,20]}]}]}
                """.trimMargin(),
            )
        val line =
            TextLine(
                text = "FISH and chops",
                boundingBox = BoundingBox(0f, 0f, 100f, 20f),
                words =
                    listOf(
                        TextWord("FISH", BoundingBox(0f, 0f, 20f, 20f)),
                        TextWord("and", BoundingBox(30f, 0f, 50f, 20f)),
                        TextWord("chops", BoundingBox(60f, 0f, 100f, 20f)),
                    ),
            )
        val score = OcrScorer.score(truth, RecognitionResult(listOf(TextBlock(listOf(line), line.boundingBox))))
        assertEquals(1, score.matchedLines)
        assertEquals(3, score.wordBoxHits)
        assertEquals(2, score.wordBoxAndTextHits)
        assertEquals(ErrorCount(4, 14), score.ordered.charCaseSensitive)
        assertEquals(ErrorCount(1, 14), score.ordered.charCaseInsensitive)
        assertEquals(ErrorCount(9, 12), score.bag.charCaseSensitive)
        assertEquals(ErrorCount(1, 12), score.bag.charCaseInsensitive)
    }

    @Test
    fun unmatchedGroundTruthLineCountsAsDeletion() {
        val truth =
            GroundTruth.parse(
                "t",
                """{"category":"sign","tags":[],"width":100,"height":50,
                |"lines":[{"text":"OPEN","block":0,"polygon":[[0,0],[10,0],[10,10],[0,10]],"box":[0,0,10,10],"words":[]}]}
                """.trimMargin(),
            )
        val score = OcrScorer.score(truth, RecognitionResult.EMPTY)
        assertEquals(0, score.matchedLines)
        assertEquals(ErrorCount(4, 4), score.bag.charCaseSensitive)
    }

    @Test
    fun readingOrderReproducesGroundTruthOrderOnMostFixtures() {
        val directory = File("../app/src/androidTest/assets/ocr-fixtures")
        val truths =
            directory.listFiles { file -> file.extension == "json" }.orEmpty().sortedBy { it.name }.map {
                GroundTruth.parse(it.nameWithoutExtension, it.readText())
            }
        val exact =
            truths.count { truth ->
                ReadingOrder.sort(truth.lines.shuffled(java.util.Random(1))) { it.polygon }.map { it.text } == truth.lines.map { it.text }
            }
        assertTrue(exact >= truths.size * MIN_ORDER_SHARE, "Reading order matched ground truth on $exact of ${truths.size}")
    }

    private companion object {
        const val EPSILON = 1e-6
        const val MIN_ORDER_SHARE = 0.5
    }
}
