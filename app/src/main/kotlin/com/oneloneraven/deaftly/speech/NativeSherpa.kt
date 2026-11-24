package com.oneloneraven.deaftly.speech

import android.content.res.AssetManager

internal class NativeSherpa {
    external fun create(
        assets: AssetManager,
        dir: String,
        threads: Int,
        trailingSilence: Float,
    ): Long

    external fun accept(
        handle: Long,
        samples: FloatArray,
    ): Int

    external fun text(handle: Long): String

    external fun release(handle: Long)

    internal companion object {
        const val NO_CHANGE = 0
        const val PARTIAL = 1
        const val ENDPOINT = 2

        init {
            System.loadLibrary("deaftly_native")
        }
    }
}
