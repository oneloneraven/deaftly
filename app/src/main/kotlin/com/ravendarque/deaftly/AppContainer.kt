package com.ravendarque.deaftly

import android.content.Context
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravendarque.deaftly.core.template.TemplateRepository
import com.ravendarque.deaftly.storage.createTemplateRepository
import com.ravendarque.deaftly.templates.builtInTemplates
import com.ravendarque.deaftly.ui.editor.EditorStrings
import com.ravendarque.deaftly.ui.editor.EditorViewModel
import com.ravendarque.deaftly.ui.fill.FillViewModel
import com.ravendarque.deaftly.ui.home.HomeViewModel

class AppContainer(
    private val context: Context,
) {
    val templateRepository: TemplateRepository by lazy {
        createTemplateRepository(context, builtInTemplates(context.resources))
    }

    val homeViewModelFactory =
        viewModelFactory {
            initializer { HomeViewModel(templateRepository) }
        }

    fun editorViewModelFactory(templateId: String?) =
        viewModelFactory {
            initializer {
                val resources = context.resources
                val strings =
                    EditorStrings(
                        newPlaceholderName = resources.getString(R.string.editor_new_placeholder_name),
                        copyTitle = { resources.getString(R.string.editor_copy_title, it) },
                    )
                EditorViewModel(templateId, templateRepository, createSavedStateHandle(), strings)
            }
        }

    fun fillViewModelFactory(templateId: String) =
        viewModelFactory {
            initializer { FillViewModel(templateId, templateRepository, createSavedStateHandle()) }
        }
}
