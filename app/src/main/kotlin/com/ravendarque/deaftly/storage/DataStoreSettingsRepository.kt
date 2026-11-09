package com.ravendarque.deaftly.storage

import androidx.datastore.core.DataStore
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
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
            .map { ChatProfile(it.settings.chatName, it.settings.chatColour?.toDomain() ?: ColourRole.Indigo) }
            .distinctUntilChanged()

    override val firstRunDone: Flow<Boolean> = store.data.map { it.settings.firstRunDone }.distinctUntilChanged()

    override suspend fun setShowScheme(scheme: ShowScheme) {
        store.updateData { document -> document.copy(settings = document.settings.copy(showScheme = scheme.toDto())) }
    }

    override suspend fun setFirstRunDone() {
        store.updateData { document -> document.copy(settings = document.settings.copy(firstRunDone = true)) }
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
        ParticipantColourDto.Red -> ColourRole.Red
        ParticipantColourDto.Orange -> ColourRole.Orange
        ParticipantColourDto.Gold -> ColourRole.Gold
        ParticipantColourDto.Green -> ColourRole.Green
        ParticipantColourDto.Blue -> ColourRole.Blue
        ParticipantColourDto.Indigo -> ColourRole.Indigo
    }

private fun ColourRole.toDto(): ParticipantColourDto =
    when (this) {
        ColourRole.Red -> ParticipantColourDto.Red
        ColourRole.Orange -> ParticipantColourDto.Orange
        ColourRole.Gold -> ParticipantColourDto.Gold
        ColourRole.Green -> ParticipantColourDto.Green
        ColourRole.Blue -> ParticipantColourDto.Blue
        ColourRole.Indigo -> ParticipantColourDto.Indigo
    }
