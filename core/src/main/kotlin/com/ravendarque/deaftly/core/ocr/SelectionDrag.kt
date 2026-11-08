package com.ravendarque.deaftly.core.ocr

import kotlin.math.hypot

/** The size of the view the viewport units are drawn into, in pixels, and how close a touch must be. */
public class DragGeometry(
    public val width: Float,
    public val height: Float,
    public val startSlop: Float,
    public val handleRadius: Float,
)

/**
 * Drag selection over words: a drag that starts on a word (or within [DragGeometry.startSlop] of one)
 * starts a new range; a drag that starts on a handle of the current range moves that end. While
 * dragging, the moving end is the word nearest the finger, so the range follows reading order across
 * lines and never needs an exact hit.
 */
public object SelectionDrag {
    public fun begin(
        phrases: List<Phrase>,
        current: TextSelection?,
        touch: Point,
        geometry: DragGeometry,
    ): TextSelection? =
        current?.let { handleDrag(phrases, it, touch, geometry) }
            ?: wordWithin(phrases, touch, geometry)?.let { TextSelection(it, it) }

    public fun move(
        phrases: List<Phrase>,
        selection: TextSelection,
        touch: Point,
        geometry: DragGeometry,
    ): TextSelection {
        val focus = nearestWord(phrases, touch, geometry) ?: return selection
        return selection.copy(focus = focus)
    }

    private fun handleDrag(
        phrases: List<Phrase>,
        selection: TextSelection,
        touch: Point,
        geometry: DragGeometry,
    ): TextSelection? {
        val runs = PhraseSelection.runs(phrases, selection)
        val first = runs.firstOrNull() ?: return null
        val last = runs.last()
        val startHandle = pixels(first.start, geometry)
        val endHandle = pixels(last.end, geometry)
        val startDistance = hypot(touch.x - startHandle.x, touch.y - startHandle.y)
        val endDistance = hypot(touch.x - endHandle.x, touch.y - endHandle.y)
        val startRef = WordRef(first.phrase.id, first.firstWord)
        val endRef = WordRef(last.phrase.id, last.lastWord)
        return when {
            startDistance > geometry.handleRadius && endDistance > geometry.handleRadius -> null
            startDistance <= endDistance -> TextSelection(endRef, startRef)
            else -> TextSelection(startRef, endRef)
        }
    }

    private fun wordWithin(
        phrases: List<Phrase>,
        touch: Point,
        geometry: DragGeometry,
    ): WordRef? = nearest(phrases, touch, geometry)?.takeIf { it.second <= geometry.startSlop }?.first

    private fun nearestWord(
        phrases: List<Phrase>,
        touch: Point,
        geometry: DragGeometry,
    ): WordRef? = nearest(phrases, touch, geometry)?.first

    private fun nearest(
        phrases: List<Phrase>,
        touch: Point,
        geometry: DragGeometry,
    ): Pair<WordRef, Float>? =
        phrases
            .flatMap { phrase ->
                phrase.words.mapIndexed { index, word ->
                    WordRef(phrase.id, index) to
                        PhraseHitTest.distance(word.quad, touch.x, touch.y, geometry.width, geometry.height)
                }
            }.minByOrNull { it.second }

    private fun pixels(
        point: Point,
        geometry: DragGeometry,
    ): Point = Point(point.x * geometry.width, point.y * geometry.height)
}
