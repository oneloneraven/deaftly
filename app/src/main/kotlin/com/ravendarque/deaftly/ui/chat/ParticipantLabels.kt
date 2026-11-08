package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.ui.theme.ParticipantColours

private val AvatarSize = 28.sp

@Composable
fun Participant.labelled(): Participant =
    namedOr(stringResource(if (id == ParticipantId.A) R.string.chat_name_me else R.string.chat_name_them))

@Composable
fun ColourRole.label(): String =
    stringResource(
        when (this) {
            ColourRole.Accent -> R.string.colour_indigo
            ColourRole.PersonB -> R.string.colour_peach
            ColourRole.Teal -> R.string.colour_teal
            ColourRole.Rose -> R.string.colour_rose
            ColourRole.Berry -> R.string.colour_berry
        },
    )

@Composable
fun ParticipantAvatar(
    initial: String,
    colours: ParticipantColours,
    modifier: Modifier = Modifier,
    size: TextUnit = AvatarSize,
) {
    Box(
        modifier =
            modifier
                .size(
                    with(LocalDensity.current) { size.toDp() },
                ).background(colours.container, CircleShape)
                .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, style = MaterialTheme.typography.labelLarge, color = colours.onContainer)
    }
}
