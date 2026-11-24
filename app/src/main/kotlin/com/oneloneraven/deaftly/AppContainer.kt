package com.oneloneraven.deaftly

import android.content.Context
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oneloneraven.deaftly.core.settings.SettingsRepository
import com.oneloneraven.deaftly.core.template.TemplateRepository
import com.oneloneraven.deaftly.feedback.ENGAGE_SUBMISSIONS
import com.oneloneraven.deaftly.feedback.FeedbackKind
import com.oneloneraven.deaftly.feedback.FeedbackSender
import com.oneloneraven.deaftly.feedback.FeedbackViewModel
import com.oneloneraven.deaftly.feedback.HttpFeedbackSender
import com.oneloneraven.deaftly.help.HelpRepository
import com.oneloneraven.deaftly.ocr.paddle.PaddleOcrNcnnRecogniser
import com.oneloneraven.deaftly.speech.MicrophoneAudioSource
import com.oneloneraven.deaftly.speech.SherpaSpeechRecogniser
import com.oneloneraven.deaftly.storage.DataStoreSettingsRepository
import com.oneloneraven.deaftly.storage.DataStoreTemplateRepository
import com.oneloneraven.deaftly.storage.FileSavedChatStore
import com.oneloneraven.deaftly.storage.SavedChatStore
import com.oneloneraven.deaftly.storage.createTemplateStore
import com.oneloneraven.deaftly.storage.refreshBuiltIns
import com.oneloneraven.deaftly.templates.builtInTemplates
import com.oneloneraven.deaftly.ui.chat.ChatInsertions
import com.oneloneraven.deaftly.ui.chat.ChatOpening
import com.oneloneraven.deaftly.ui.chat.ChatViewModel
import com.oneloneraven.deaftly.ui.editor.EditorStrings
import com.oneloneraven.deaftly.ui.editor.EditorViewModel
import com.oneloneraven.deaftly.ui.facetoface.FaceToFaceViewModel
import com.oneloneraven.deaftly.ui.fill.FillViewModel
import com.oneloneraven.deaftly.ui.firstrun.FirstRunViewModel
import com.oneloneraven.deaftly.ui.home.HomeViewModel
import com.oneloneraven.deaftly.ui.listen.ListenViewModel
import com.oneloneraven.deaftly.ui.ocr.FrameImage
import com.oneloneraven.deaftly.ui.ocr.OcrOutcomes
import com.oneloneraven.deaftly.ui.ocr.OcrViewModel
import com.oneloneraven.deaftly.ui.savedchats.SavedChatsViewModel
import com.oneloneraven.deaftly.ui.settings.SettingsViewModel
import com.oneloneraven.deaftly.ui.show.ShowViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import java.io.File
import java.net.URL

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

    val appVersion: String by lazy {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
    }

    private val feedbackSender: FeedbackSender by lazy { HttpFeedbackSender(URL(ENGAGE_SUBMISSIONS), appVersion) }

    fun feedbackViewModelFactory(kind: FeedbackKind) =
        viewModelFactory {
            initializer { FeedbackViewModel(kind, feedbackSender, createSavedStateHandle()) }
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
