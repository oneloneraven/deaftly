package com.ravendarque.deaftly

import android.content.Context
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravendarque.deaftly.core.template.TemplateRepository
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.storage.createTemplateRepository
import com.ravendarque.deaftly.templates.builtInTemplates
import com.ravendarque.deaftly.ui.home.HomeViewModel

class AppContainer(
    private val context: Context,
) {
    val templateRepository: TemplateRepository by lazy {
        createTemplateRepository(context, builtInTemplates(context.resources))
    }

    val homeViewModelFactory =
        viewModelFactory {
            initializer { HomeViewModel(Route.entries.filter { it != Route.Home }) }
        }
}
