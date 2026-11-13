package com.ravendarque.deaftly

import android.content.Context
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.core.template.TemplateRepository
import com.ravendarque.deaftly.help.HelpRepository
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrNcnnRecogniser
import com.ravendarque.deaftly.storage.DataStoreSettingsRepository
import com.ravendarque.deaftly.storage.DataStoreTemplateRepository
import com.ravendarque.deaftly.storage.createTemplateStore
import com.ravendarque.deaftly.templates.builtInTemplates
import com.ravendarque.deaftly.ui.chat.ChatInsertions
import com.ravendarque.deaftly.ui.chat.ChatOpening
import com.ravendarque.deaftly.ui.chat.ChatViewModel
import com.ravendarque.deaftly.ui.editor.EditorStrings
import com.ravendarque.deaftly.ui.editor.EditorViewModel
import com.ravendarque.deaftly.ui.facetoface.FaceToFaceViewModel
import com.ravendarque.deaftly.ui.fill.FillViewModel
import com.ravendarque.deaftly.ui.firstrun.FirstRunViewModel
import com.ravendarque.deaftly.ui.home.HomeViewModel
import com.ravendarque.deaftly.ui.ocr.FrameImage
import com.ravendarque.deaftly.ui.ocr.OcrOutcomes
import com.ravendarque.deaftly.ui.ocr.OcrViewModel
import com.ravendarque.deaftly.ui.settings.SettingsViewModel
import com.ravendarque.deaftly.ui.show.ShowViewModel
import kotlinx.coroutines.flow.first

class AppContainer(
    private val context: Context,
) {
    private val builtIns by lazy { builtInTemplates(context.resources) }

    private val store by lazy { createTemplateStore(context, builtIns) }

    val templateRepository: TemplateRepository by lazy { DataStoreTemplateRepository(store, builtIns) }

    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(store) }

    val helpRepository by lazy { HelpRepository(context.assets) }

    val ocrOutcomes = OcrOutcomes()
    val chatInsertions = ChatInsertions()

    fun ocrViewModelFactory(
        templateId: String,
        placeholderKey: String,
    ) = viewModelFactory {
        initializer {
            OcrViewModel(
                createRecogniser = { PaddleOcrNcnnRecogniser(context.applicationContext) },
                captureFrame = { frame, crop -> FrameImage.toBitmap(frame, crop).asImageBitmap() },
                loadFieldLabel = {
                    templateRepository.templates
                        .first()
                        .firstOrNull { it.id == templateId }
                        ?.placeholders
                        ?.get(placeholderKey)
                        ?.label
                },
            )
        }
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

    val faceToFaceViewModelFactory =
        viewModelFactory {
            initializer { FaceToFaceViewModel(createSavedStateHandle()) }
        }

    val showViewModelFactory =
        viewModelFactory {
            initializer { ShowViewModel(settingsRepository) }
        }

    val settingsViewModelFactory =
        viewModelFactory {
            initializer { SettingsViewModel(settingsRepository, templateRepository) }
        }

    val firstRunViewModelFactory =
        viewModelFactory {
            initializer { FirstRunViewModel(settingsRepository) }
        }

    fun chatViewModelFactory(opening: ChatOpening) =
        viewModelFactory {
            initializer { ChatViewModel(settingsRepository, createSavedStateHandle(), opening, chatInsertions) }
        }

    fun fillViewModelFactory(templateId: String) =
        viewModelFactory {
            initializer { FillViewModel(templateId, templateRepository, createSavedStateHandle(), ocrOutcomes) }
        }
}
