package com.ravendarque.deaftly.spikestt

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class AndroidOnDeviceEngine(private val context: Context, private val language: String) : Engine {
    override val name = "android-on-device-$language"
    private val main = Handler(Looper.getMainLooper())

    override fun transcribe(samples: FloatArray): Transcript {
        val done = CountDownLatch(1)
        var text = ""
        var lastPartial = ""
        var firstPartialMs: Double? = null
        var endedAt = 0L
        var finishedAt = 0L
        var error: Int? = null
        val pipe = ParcelFileDescriptor.createPipe()
        lateinit var recogniser: SpeechRecognizer
        val startedAt = System.nanoTime()
        main.post {
            recogniser = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            recogniser.setRecognitionListener(
                object : RecognitionListener {
                    override fun onPartialResults(partialResults: Bundle) {
                        val p = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (firstPartialMs == null && !p.isNullOrBlank()) firstPartialMs = (System.nanoTime() - startedAt) / 1e6
                        if (!p.isNullOrBlank()) lastPartial = p
                    }

                    override fun onResults(results: Bundle) {
                        text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        android.util.Log.d("SPIKESTT", "results=${results.keySet().associateWith { results.get(it) }} lastPartial=$lastPartial")
                        finishedAt = System.nanoTime()
                        done.countDown()
                    }

                    override fun onError(e: Int) {
                        error = e
                        finishedAt = System.nanoTime()
                        done.countDown()
                    }

                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                },
            )
            val intent =
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pipe[0])
                    .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                    .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, 16000)
            recogniser.startListening(intent)
        }
        var cutOff = false
        thread {
            try { ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { out ->
                val tail = FloatArray(16000)
                for (part in listOf(samples, tail)) {
                    var i = 0
                    while (i < part.size) {
                        val n = minOf(CHUNK, part.size - i)
                        val bytes = ByteArray(n * 2)
                        for (k in 0 until n) {
                            val s = (part[i + k].coerceIn(-1f, 1f) * 32767).toInt()
                            bytes[2 * k] = s.toByte()
                            bytes[2 * k + 1] = (s shr 8).toByte()
                        }
                        out.write(bytes)
                        Thread.sleep(100)
                        i += n
                    }
                    if (part === samples) endedAt = System.nanoTime()
                }
            } } catch (e: java.io.IOException) {
                if (endedAt == 0L) cutOff = true
            }
        }
        done.await(60, TimeUnit.SECONDS)
        main.post { recogniser.destroy() }
        pipe[0].close()
        val audioMs = samples.size / 16.0
        val first = firstPartialMs?.let { (it / 100).toInt() }
        val finalise = if (endedAt != 0L && finishedAt != 0L) (finishedAt - endedAt) / 1e6 else -1.0
        val best = text.ifBlank { lastPartial } + if (cutOff) " <cut-off>" else ""
        return Transcript(if (error != null) "<error $error> $lastPartial" else best, first, finalise, listOf(audioMs))
    }
}
