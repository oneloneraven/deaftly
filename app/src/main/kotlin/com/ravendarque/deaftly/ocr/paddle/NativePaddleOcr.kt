package com.ravendarque.deaftly.ocr.paddle

import android.content.res.AssetManager
import java.nio.ByteBuffer

@Suppress("LongParameterList")
internal class NativePaddleOcr {
    external fun create(
        assets: AssetManager,
        detParam: String,
        detBin: String,
        recParam: String,
        recBin: String,
        dict: String,
        detSide: Int,
        threads: Int,
        fp16: Boolean,
    ): Long

    external fun release(handle: Long)

    external fun recognise(
        handle: Long,
        pixels: ByteBuffer,
        width: Int,
        height: Int,
        strideBytes: Int,
        rotation: Int,
    ): Int

    external fun geometry(handle: Long): FloatArray

    external fun texts(handle: Long): Array<String>

    external fun timings(handle: Long): DoubleArray

    private companion object {
        init {
            System.loadLibrary("deaftly_ppocr")
        }
    }
}
