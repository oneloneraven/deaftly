package com.ravendarque.deaftly.storage

import androidx.datastore.core.DataStore
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val store: DataStore<StoreDocument>,
) : SettingsRepository {
    override val chatProfile: Flow<ChatProfile> =
        store.data
            .map { ChatProfile(it.settings.chatName, it.settings.chatColour?.toDomain() ?: ColourRole.Accent) }
            .distinctUntilChanged()

    override suspend fun setChatProfile(profile: ChatProfile) {
        store.updateData { document ->
            document.copy(
                settings =
                    document.settings.copy(
                        chatName = profile.name?.trim()?.takeIf { it.isNotEmpty() },
                        chatColour = profile.colour.toDto(),
                    ),
            )
        }
    }
}

private fun ParticipantColourDto.toDomain(): ColourRole =
    when (this) {
        ParticipantColourDto.Accent -> ColourRole.Accent
        ParticipantColourDto.PersonB -> ColourRole.PersonB
        ParticipantColourDto.Teal -> ColourRole.Teal
        ParticipantColourDto.Rose -> ColourRole.Rose
        ParticipantColourDto.Berry -> ColourRole.Berry
    }

private fun ColourRole.toDto(): ParticipantColourDto =
    when (this) {
        ColourRole.Accent -> ParticipantColourDto.Accent
        ColourRole.PersonB -> ParticipantColourDto.PersonB
        ColourRole.Teal -> ParticipantColourDto.Teal
        ColourRole.Rose -> ParticipantColourDto.Rose
        ColourRole.Berry -> ParticipantColourDto.Berry
    }
