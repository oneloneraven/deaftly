package com.ravendarque.deaftly.ui.ocr

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.PixelFormat
import com.ravendarque.deaftly.core.ocr.RecognitionFrame

class FrameAnalyser(
    private val analyse: (RecognitionFrame, BoundingBox?) -> Unit,
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        image.use {
            val plane = it.planes[0]
            val frame =
                RecognitionFrame(
                    width = it.width,
                    height = it.height,
                    rotationDegrees = it.imageInfo.rotationDegrees,
                    format = PixelFormat.RGBA_8888,
                    pixels = plane.buffer,
                    rowStrideBytes = plane.rowStride,
                )
            val crop = it.cropRect
            analyse(frame, BoundingBox(crop.left.toFloat(), crop.top.toFloat(), crop.right.toFloat(), crop.bottom.toFloat()))
        }
    }
}
