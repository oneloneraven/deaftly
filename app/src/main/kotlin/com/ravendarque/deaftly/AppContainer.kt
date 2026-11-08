package com.ravendarque.deaftly

import android.content.Context
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.core.template.TemplateRepository
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.storage.createTemplateRepository
import com.ravendarque.deaftly.ui.home.HomeViewModel

class AppContainer(
    private val context: Context,
) {
    private val builtInTemplates: List<Template> = emptyList()

    val templateRepository: TemplateRepository by lazy { createTemplateRepository(context, builtInTemplates) }

    val homeViewModelFactory =
        viewModelFactory {
            initializer { HomeViewModel(Route.entries.filter { it != Route.Home }) }
        }
}
