package com.ravendarque.deaftly.core.settings

import kotlinx.coroutines.flow.Flow

public interface SettingsRepository {
    public val showScheme: Flow<ShowScheme?>

    public suspend fun setShowScheme(scheme: ShowScheme)
}
