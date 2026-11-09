package com.ravendarque.deaftly.core.ocr

public data class TappedLine(
    val phraseId: Long,
    val text: String,
)

/**
 * What the user has picked in the camera view: either whole lines tapped one by one, kept in tap
 * order, or a [range] of words dragged across lines. A drag replaces the tapped lines, and a tap
 * after a drag starts a new set of tapped lines.
 */
public data class OcrSelection(
    val lines: List<TappedLine> = emptyList(),
    val range: TextSelection? = null,
) {
    val isEmpty: Boolean get() = lines.isEmpty() && range == null

    /** The tapped lines joined by spaces in the order they were tapped; empty for a dragged range. */
    val tappedText: String get() = lines.joinToString(" ") { it.text }

    public fun isTapped(phraseId: Long): Boolean = lines.any { it.phraseId == phraseId }

    public fun toggled(phrase: Phrase): OcrSelection =
        when {
            range != null -> OcrSelection(lines = listOf(TappedLine(phrase.id, phrase.text)))
            isTapped(phrase.id) -> copy(lines = lines.filterNot { it.phraseId == phrase.id })
            else -> copy(lines = lines + TappedLine(phrase.id, phrase.text))
        }

    public fun dragged(range: TextSelection): OcrSelection = OcrSelection(range = range)

    /** The parts of [phrases] to highlight: whole lines for taps, the covered words for a range. */
    public fun runs(phrases: List<Phrase>): List<SelectedRun> =
        range?.let { PhraseSelection.runs(phrases, it) }
            ?: phrases.filter { isTapped(it.id) }.map { SelectedRun(it, 0, it.words.lastIndex) }

    /** The range whose end handles can be dragged: the dragged range, or a single tapped line. */
    public fun handleRange(phrases: List<Phrase>): TextSelection? =
        range ?: lines.singleOrNull()?.let { tapped -> phrases.firstOrNull { it.id == tapped.phraseId } }?.let(PhraseSelection::wholePhrase)
}
