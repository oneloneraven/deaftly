package com.ravendarque.deaftly.ui.chat

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
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
        Participant(ParticipantId.A, ColourRole.Accent, "Sam"),
        Participant(ParticipantId.B, ColourRole.PersonB, "Waiter"),
    ).append(styled("Hi! I'm deaf. Could I have pasta arrabbiata please?", "pasta arrabbiata", SpanStyle.Bold))
        .withTurn(ParticipantId.B)
        .append(styled("Sure! Do you want it spicy or mild?", "spicy or mild", SpanStyle.Highlight))
        .withTurn(ParticipantId.A)
        .append(styled("Mild please. And no nuts, I'm allergic.", "no nuts", SpanStyle.Bold), quoting = 1)
        .withTurn(ParticipantId.B)
        .append(styled("No problem. It will take about 15 minutes.", "15 minutes", SpanStyle.Highlight))
        .withTurn(turn)

val isParticipantNameField = hasSetTextAction() and SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription)

val isMessageField = hasSetTextAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)
