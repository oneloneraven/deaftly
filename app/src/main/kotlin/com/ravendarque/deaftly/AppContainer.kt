package com.ravendarque.deaftly

import android.content.Context
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.core.template.TemplateRepository
import com.ravendarque.deaftly.storage.DataStoreSettingsRepository
import com.ravendarque.deaftly.storage.DataStoreTemplateRepository
import com.ravendarque.deaftly.storage.createTemplateStore
import com.ravendarque.deaftly.templates.builtInTemplates
import com.ravendarque.deaftly.ui.chat.ChatViewModel
import com.ravendarque.deaftly.ui.fill.FillViewModel
import com.ravendarque.deaftly.ui.home.HomeViewModel
import com.ravendarque.deaftly.ui.show.ShowViewModel

class AppContainer(
    private val context: Context,
) {
    private val builtIns by lazy { builtInTemplates(context.resources) }

    private val store by lazy { createTemplateStore(context, builtIns) }

    val templateRepository: TemplateRepository by lazy { DataStoreTemplateRepository(store, builtIns) }

    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(store) }

    val homeViewModelFactory =
        viewModelFactory {
            initializer { HomeViewModel(templateRepository) }
        }

    val showViewModelFactory =
        viewModelFactory {
            initializer { ShowViewModel(settingsRepository) }
        }

    fun chatViewModelFactory(firstMessage: String?) =
        viewModelFactory {
            initializer { ChatViewModel(settingsRepository, createSavedStateHandle(), firstMessage) }
        }

    fun fillViewModelFactory(templateId: String) =
        viewModelFactory {
            initializer { FillViewModel(templateId, templateRepository, createSavedStateHandle()) }
        }
}
