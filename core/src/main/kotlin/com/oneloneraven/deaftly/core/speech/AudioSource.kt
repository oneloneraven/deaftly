package com.oneloneraven.deaftly.core.speech

import kotlinx.coroutines.flow.Flow

public class AudioUnavailableException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

public fun interface AudioSource {
    public fun chunks(): Flow<FloatArray>
}
