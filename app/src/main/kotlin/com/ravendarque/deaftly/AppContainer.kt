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
import com.ravendarque.deaftly.speech.MicrophoneAudioSource
import com.ravendarque.deaftly.speech.SherpaSpeechRecogniser
import com.ravendarque.deaftly.storage.DataStoreSettingsRepository
import com.ravendarque.deaftly.storage.DataStoreTemplateRepository
import com.ravendarque.deaftly.storage.FileSavedChatStore
import com.ravendarque.deaftly.storage.SavedChatStore
import com.ravendarque.deaftly.storage.createTemplateStore
import com.ravendarque.deaftly.storage.refreshBuiltIns
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
import com.ravendarque.deaftly.ui.listen.ListenViewModel
import com.ravendarque.deaftly.ui.ocr.FrameImage
import com.ravendarque.deaftly.ui.ocr.OcrOutcomes
import com.ravendarque.deaftly.ui.ocr.OcrViewModel
import com.ravendarque.deaftly.ui.savedchats.SavedChatsViewModel
import com.ravendarque.deaftly.ui.settings.SettingsViewModel
import com.ravendarque.deaftly.ui.show.ShowViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import java.io.File

class AppContainer(
    private val context: Context,
) {
    private val store by lazy { createTemplateStore(context, builtInTemplates(context.resources)) }

    val templateRepository: TemplateRepository by lazy { DataStoreTemplateRepository(store, { builtInTemplates(context.resources) }) }

    suspend fun refreshBuiltInTemplates() = store.refreshBuiltIns(builtInTemplates(context.resources))

    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(store) }

    private val appScope = appScope()

    val savedChatStore: SavedChatStore by lazy { FileSavedChatStore(File(context.filesDir, "chats"), Dispatchers.IO, appScope) }

    fun startReadingSavedChats() {
        savedChatStore
    }

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

    val listenViewModelFactory =
        viewModelFactory {
            initializer {
                ListenViewModel(
                    settingsRepository,
                    createSavedStateHandle(),
                    createSpeechRecogniser = { SherpaSpeechRecogniser(context.applicationContext) },
                    audio = MicrophoneAudioSource(Dispatchers.IO),
                )
            }
        }

    val showViewModelFactory =
        viewModelFactory {
            initializer { ShowViewModel(settingsRepository) }
        }

    val settingsViewModelFactory =
        viewModelFactory {
            initializer { SettingsViewModel(settingsRepository, templateRepository) }
        }

    val savedChatsViewModelFactory =
        viewModelFactory {
            initializer { SavedChatsViewModel(savedChatStore, appScope) }
        }

    val firstRunViewModelFactory =
        viewModelFactory {
            initializer { FirstRunViewModel(settingsRepository) }
        }

    fun chatViewModelFactory(opening: ChatOpening) =
        viewModelFactory {
            initializer {
                ChatViewModel(
                    settingsRepository,
                    createSavedStateHandle(),
                    opening,
                    chatInsertions,
                    savedChatStore,
                    appScope,
                    createSpeechRecogniser = { SherpaSpeechRecogniser(context.applicationContext) },
                    audio = MicrophoneAudioSource(Dispatchers.IO),
                )
            }
        }

    fun fillViewModelFactory(templateId: String) =
        viewModelFactory {
            initializer { FillViewModel(templateId, templateRepository, settingsRepository, createSavedStateHandle(), ocrOutcomes) }
        }
}
