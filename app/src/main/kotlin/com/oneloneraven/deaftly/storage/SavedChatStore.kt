package com.oneloneraven.deaftly.storage

import android.util.AtomicFile
import com.oneloneraven.deaftly.core.chat.Conversation
import com.oneloneraven.deaftly.ui.chat.ConversationDto
import com.oneloneraven.deaftly.ui.chat.toDomain
import com.oneloneraven.deaftly.ui.chat.toDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

data class SavedChatSummary(
    val id: String,
    val otherName: String?,
    val lastMessage: String?,
    val changedAt: Long,
    val readable: Boolean = true,
)

data class SavedChat(
    val id: String,
    val conversation: Conversation,
    val changedAt: Long,
)

interface SavedChatStore {
    val summaries: StateFlow<List<SavedChatSummary>>

    val loaded: StateFlow<Boolean>

    suspend fun load(id: String): SavedChat?

    suspend fun save(
        id: String,
        conversation: Conversation,
    )

    suspend fun delete(id: String): Boolean

    fun newId(): String
}

private val SavedChatId = Regex("^[0-9a-f-]{36}$")
private const val SUFFIX = ".json"
private const val FORMAT_VERSION = 1
private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class SavedChatDto(
    val version: Int,
    val changedAt: Long,
    val conversation: ConversationDto,
)

private fun encode(chat: SavedChat): String =
    json.encodeToString(SavedChatDto.serializer(), SavedChatDto(FORMAT_VERSION, chat.changedAt, chat.conversation.toDto()))

private fun decode(
    id: String,
    text: String,
): SavedChat? =
    runCatching {
        val dto = json.decodeFromString(SavedChatDto.serializer(), text)
        SavedChat(id, dto.conversation.toDomain(), dto.changedAt)
    }.getOrNull()

fun isSavedChatId(id: String): Boolean = SavedChatId.matches(id)

class FileSavedChatStore(
    private val dir: File,
    private val io: CoroutineDispatcher,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) : SavedChatStore {
    private val mutex = Mutex()
    private val all = MutableStateFlow<List<SavedChatSummary>>(emptyList())

    override val summaries: StateFlow<List<SavedChatSummary>> = all.asStateFlow()

    private val ready = MutableStateFlow(false)

    override val loaded: StateFlow<Boolean> = ready.asStateFlow()

    init {
        scope.launch(io) {
            try {
                mutex.withLock { all.value = readAll().sortedByDescending { it.changedAt } }
            } finally {
                ready.value = true
            }
        }
    }

    override suspend fun load(id: String): SavedChat? {
        if (!isSavedChatId(id)) return null
        return withContext(io) {
            mutex.withLock {
                read(id).also { chat -> if (chat == null) forgetOrMarkUnreadable(id) }
            }
        }
    }

    override suspend fun save(
        id: String,
        conversation: Conversation,
    ) {
        require(isSavedChatId(id)) { "Not a saved chat ID" }
        withContext(io) {
            mutex.withLock {
                val chat = SavedChat(id, conversation, clock())
                dir.mkdirs()
                val file = AtomicFile(File(dir, id + SUFFIX))
                val stream = file.startWrite()
                runCatching { stream.write(encode(chat).toByteArray()) }
                    .onSuccess { file.finishWrite(stream) }
                    .onFailure {
                        file.failWrite(stream)
                        throw it
                    }
                all.update { current -> (current.filterNot { it.id == id } + chat.summary()).sortedByDescending { it.changedAt } }
            }
        }
    }

    override suspend fun delete(id: String): Boolean {
        if (!isSavedChatId(id)) return true
        return withContext(io) {
            mutex.withLock {
                val file = File(dir, id + SUFFIX)
                AtomicFile(file).delete()
                val gone = !file.exists()
                if (gone) all.update { current -> current.filterNot { it.id == id } }
                gone
            }
        }
    }

    override fun newId(): String = UUID.randomUUID().toString()

    private fun forgetOrMarkUnreadable(id: String) {
        val stillThere = File(dir, id + SUFFIX).exists()
        all.update { current ->
            if (stillThere) current.map { if (it.id == id) it.copy(readable = false) else it } else current.filterNot { it.id == id }
        }
    }

    private fun readAll(): List<SavedChatSummary> =
        dir
            .listFiles { file -> file.name.endsWith(SUFFIX) }
            .orEmpty()
            .mapNotNull { file ->
                val id = file.name.removeSuffix(SUFFIX)
                when {
                    !isSavedChatId(id) -> null
                    else -> read(id)?.summary() ?: SavedChatSummary(id, null, null, file.lastModified(), readable = false)
                }
            }

    private fun read(id: String): SavedChat? =
        File(dir, id + SUFFIX)
            .takeIf { isSavedChatId(id) && it.exists() }
            ?.let { file -> runCatching { AtomicFile(file).readFully().decodeToString() }.getOrNull() }
            ?.let { text -> decode(id, text) }

    private fun SavedChat.summary() =
        SavedChatSummary(
            id = id,
            otherName = conversation.participantB.name,
            lastMessage = conversation.messages.lastOrNull()?.plainText,
            changedAt = changedAt,
        )
}
