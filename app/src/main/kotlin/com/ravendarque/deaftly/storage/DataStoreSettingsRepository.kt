package com.ravendarque.deaftly.storage

import androidx.datastore.core.DataStore
import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.core.settings.ShowScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val store: DataStore<StoreDocument>,
) : SettingsRepository {
    override val showScheme: Flow<ShowScheme?> = store.data.map { it.settings.showScheme?.toDomain() }.distinctUntilChanged()

    override suspend fun setShowScheme(scheme: ShowScheme) {
        store.updateData { document -> document.copy(settings = document.settings.copy(showScheme = scheme.toDto())) }
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
