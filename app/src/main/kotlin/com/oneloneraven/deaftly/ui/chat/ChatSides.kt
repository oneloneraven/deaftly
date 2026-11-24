package com.oneloneraven.deaftly.ui.chat

import com.oneloneraven.deaftly.core.chat.ParticipantId

fun ParticipantId.isOnRight(sidesSwapped: Boolean): Boolean = (this == ParticipantId.A) != sidesSwapped
