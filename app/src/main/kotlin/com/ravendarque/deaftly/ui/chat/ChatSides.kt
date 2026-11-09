package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.ParticipantId

fun ParticipantId.isOnRight(sidesSwapped: Boolean): Boolean = (this == ParticipantId.A) != sidesSwapped
