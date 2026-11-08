package com.ravendarque.deaftly.storage

import androidx.datastore.core.DataStore
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.ChatLayout
import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.core.settings.ShowScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val store: DataStore<StoreDocument>,
) : SettingsRepository {
    override val showScheme: Flow<ShowScheme?> = store.data.map { it.settings.showScheme?.toDomain() }.distinctUntilChanged()

    override val chatProfile: Flow<ChatProfile> =
        store.data
            .map { ChatProfile(it.settings.chatName, it.settings.chatColour?.toDomain() ?: ColourRole.Accent) }
            .distinctUntilChanged()

    override val chatLayout: Flow<ChatLayout> =
        store.data.map { it.settings.chatLayout?.toDomain() ?: ChatLayout.Sides }.distinctUntilChanged()

    override suspend fun setShowScheme(scheme: ShowScheme) {
        store.updateData { document -> document.copy(settings = document.settings.copy(showScheme = scheme.toDto())) }
    }

    override suspend fun setChatLayout(layout: ChatLayout) {
        store.updateData { document -> document.copy(settings = document.settings.copy(chatLayout = layout.toDto())) }
    }

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

private fun ChatLayoutDto.toDomain(): ChatLayout =
    when (this) {
        ChatLayoutDto.Sides -> ChatLayout.Sides
        ChatLayoutDto.Split -> ChatLayout.Split
    }

private fun ChatLayout.toDto(): ChatLayoutDto =
    when (this) {
        ChatLayout.Sides -> ChatLayoutDto.Sides
        ChatLayout.Split -> ChatLayoutDto.Split
    }

private fun ShowSchemeDto.toDomain(): ShowScheme =
    when (this) {
        ShowSchemeDto.BlackOnWhite -> ShowScheme.BlackOnWhite
        ShowSchemeDto.WhiteOnBlack -> ShowScheme.WhiteOnBlack
        ShowSchemeDto.YellowOnBlack -> ShowScheme.YellowOnBlack
    }

private fun ShowScheme.toDto(): ShowSchemeDto =
    when (this) {
        ShowScheme.BlackOnWhite -> ShowSchemeDto.BlackOnWhite
        ShowScheme.WhiteOnBlack -> ShowSchemeDto.WhiteOnBlack
        ShowScheme.YellowOnBlack -> ShowSchemeDto.YellowOnBlack
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
