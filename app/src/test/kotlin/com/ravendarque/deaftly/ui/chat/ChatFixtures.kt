package com.ravendarque.deaftly.ui.chat

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.FormattedText
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.Quote
import com.ravendarque.deaftly.core.chat.RichText
import com.ravendarque.deaftly.core.chat.SpanStyle
import com.ravendarque.deaftly.core.chat.StyleRange

private fun styled(
    text: String,
    phrase: String,
    style: SpanStyle,
) = RichText.spans(text, listOf(StyleRange(text.indexOf(phrase), text.indexOf(phrase) + phrase.length, style)))

fun orderingConversation(turn: ParticipantId = ParticipantId.B): Conversation =
    Conversation(
        Participant(ParticipantId.A, ColourRole.Indigo, "Sam"),
        Participant(ParticipantId.B, ColourRole.Orange, "Waiter"),
    ).append(styled("Hi! I'm deaf. Could I have pasta arrabbiata please?", "pasta arrabbiata", SpanStyle.Bold))
        .withTurn(ParticipantId.B)
        .append(styled("Sure! Do you want it spicy or mild?", "spicy or mild", SpanStyle.Highlight))
        .withTurn(ParticipantId.A)
        .append(styled("Mild please. And no nuts, I'm allergic.", "no nuts", SpanStyle.Bold), quote = Quote(1, "spicy or mild?"))
        .withTurn(ParticipantId.B)
        .append(styled("No problem. It will take about 15 minutes.", "15 minutes", SpanStyle.Highlight))
        .withTurn(turn)

fun splitConversation(turn: ParticipantId): Conversation =
    Conversation(
        Participant(ParticipantId.A, ColourRole.Indigo, "Sam"),
        Participant(ParticipantId.B, ColourRole.Orange, "Waiter"),
    ).append(styled("Sure! Do you want it spicy or mild?", "spicy or mild", SpanStyle.Highlight))
        .withTurn(ParticipantId.A)
        .append(styled("Mild please. And no nuts, I'm allergic.", "no nuts", SpanStyle.Bold))
        .withTurn(ParticipantId.B)
        .append(styled("No problem. About 15 minutes.", "15 minutes", SpanStyle.Highlight))
        .withTurn(turn)

fun formattingConversation(): Conversation {
    val everything = "Mild please. And absolutely no nuts, I'm allergic."
    val from = everything.indexOf("absolutely no nuts")
    val to = from + "absolutely no nuts".length
    val all =
        RichText.spans(
            everything,
            listOf(
                StyleRange(from, to, SpanStyle.Bold),
                StyleRange(from, to, SpanStyle.Italic),
                StyleRange(from, to, SpanStyle.Highlight),
            ),
        )
    return Conversation(
        Participant(ParticipantId.A, ColourRole.Indigo, "Sam"),
        Participant(ParticipantId.B, ColourRole.Orange, "Waiter"),
    ).append(styled("Hi! I'm deaf. Could I have pasta arrabbiata please?", "pasta arrabbiata", SpanStyle.Bold))
        .withTurn(ParticipantId.B)
        .append(styled("Sure! Do you want it spicy or mild?", "spicy or mild", SpanStyle.Highlight))
        .withTurn(ParticipantId.A)
        .append(all, quote = Quote(1, "spicy or mild?"))
        .withTurn(ParticipantId.B)
        .append(styled("Noted, with fresh basil on top.", "fresh basil", SpanStyle.Italic))
}

fun formattingDraft(): Draft =
    Draft(
        Composition(
            FormattedText("Of course, no nuts at all", listOf(StyleRange(11, 18, SpanStyle.Bold))),
            selectionStart = 18,
        ),
        Quote(2, "no nuts, I'm allergic."),
    )

val isParticipantNameField = hasSetTextAction() and SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription)

val isMessageField = hasSetTextAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)
