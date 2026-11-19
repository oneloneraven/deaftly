package com.ravendarque.deaftly.speech

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.ravendarque.deaftly.core.speech.SPEECH_CHUNK_SAMPLES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MicrophoneAudioSourceTest {
    @get:Rule
    val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun emitsFullChunksAndReleases() {
        val chunks = runBlocking { MicrophoneAudioSource(Dispatchers.IO).chunks().take(CHUNKS).toList() }
        assertEquals(CHUNKS, chunks.size)
        chunks.forEach { assertEquals(SPEECH_CHUNK_SAMPLES, it.size) }
    }

    private companion object {
        const val CHUNKS = 3
    }
}
