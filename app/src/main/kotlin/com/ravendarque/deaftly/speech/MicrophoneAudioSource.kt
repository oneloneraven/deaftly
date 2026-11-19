package com.ravendarque.deaftly.speech

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.ravendarque.deaftly.core.speech.AudioSource
import com.ravendarque.deaftly.core.speech.AudioUnavailableException
import com.ravendarque.deaftly.core.speech.SPEECH_CHUNK_SAMPLES
import com.ravendarque.deaftly.core.speech.SPEECH_SAMPLE_RATE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MicrophoneAudioSource(
    private val io: CoroutineDispatcher,
) : AudioSource {
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun chunks(): Flow<FloatArray> =
        flow {
            val record = open()
            try {
                start(record)
                val buffer = FloatArray(SPEECH_CHUNK_SAMPLES)
                while (true) {
                    val read = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (read < 0) throw AudioUnavailableException("Microphone read failed: $read")
                    if (read == buffer.size) emit(buffer.copyOf())
                }
            } finally {
                runCatching { record.stop() }
                record.release()
            }
        }.flowOn(io)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun open(): AudioRecord {
        val minimum = AudioRecord.getMinBufferSize(SPEECH_SAMPLE_RATE, CHANNEL, ENCODING)
        val bytes = maxOf(minimum, 2 * SPEECH_CHUNK_SAMPLES * Float.SIZE_BYTES)
        val record =
            try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SPEECH_SAMPLE_RATE,
                    CHANNEL,
                    ENCODING,
                    bytes,
                )
            } catch (e: SecurityException) {
                throw AudioUnavailableException("Microphone permission missing", e)
            }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw AudioUnavailableException("Microphone did not initialise")
        }
        return record
    }

    private fun start(record: AudioRecord) {
        try {
            record.startRecording()
        } catch (e: IllegalStateException) {
            throw AudioUnavailableException("Microphone failed to start", e)
        }
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            throw AudioUnavailableException("Microphone is in use elsewhere")
        }
    }

    private companion object {
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_FLOAT
    }
}
