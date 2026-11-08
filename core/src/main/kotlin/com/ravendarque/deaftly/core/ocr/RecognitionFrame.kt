package com.ravendarque.deaftly.core.ocr

import java.nio.ByteBuffer

private const val RGBA_BYTES_PER_PIXEL = 4
private const val LUMA_BYTES_PER_PIXEL = 1
private const val CHROMA_SUBSAMPLING = 2

public enum class PixelFormat(
    internal val bytesPerPixel: Int,
) {
    /** Bytes R, G, B, A: `Bitmap.Config.ARGB_8888` memory, CameraX `OUTPUT_IMAGE_FORMAT_RGBA_8888`. */
    RGBA_8888(RGBA_BYTES_PER_PIXEL),

    /** Y plane, then interleaved V, U at half resolution: ML Kit `IMAGE_FORMAT_NV21`. */
    NV21(LUMA_BYTES_PER_PIXEL),
}

/**
 * A camera or bitmap frame handed to a [TextRecogniser].
 *
 * [pixels] is shared, not copied; implementations must only read it, using absolute
 * indexes or a `duplicate()`, and must not keep it after `recognise` returns.
 * [width] and [height] describe the buffer as stored; [rotationDegrees] is the clockwise
 * turn needed to display it upright. All coordinates in a [RecognitionResult] are in
 * that upright image, in pixels.
 */
public class RecognitionFrame(
    public val width: Int,
    public val height: Int,
    public val rotationDegrees: Int,
    public val format: PixelFormat,
    public val pixels: ByteBuffer,
    public val rowStrideBytes: Int = width * format.bytesPerPixel,
) {
    init {
        require(width > 0 && height > 0) { "Frame must not be empty" }
        require(rotationDegrees in VALID_ROTATIONS) { "Rotation must be 0, 90, 180 or 270" }
        require(rowStrideBytes >= width * format.bytesPerPixel) { "Row stride is smaller than a row" }
        if (format == PixelFormat.NV21) {
            require(width.isEven() && height.isEven()) { "NV21 needs even dimensions" }
        }
        require(pixels.capacity() >= requiredBytes()) { "Buffer is smaller than the frame" }
    }

    public val uprightWidth: Int get() = if (isQuarterTurn) height else width
    public val uprightHeight: Int get() = if (isQuarterTurn) width else height

    public fun toUpright(point: Point): Point =
        when (rotationDegrees) {
            QUARTER -> Point(height - point.y, point.x)
            HALF -> Point(width - point.x, height - point.y)
            THREE_QUARTERS -> Point(point.y, width - point.x)
            else -> point
        }

    public fun toUpright(box: BoundingBox): BoundingBox = toUpright(box.toQuad()).boundingBox()

    public fun toUpright(quad: Quad): Quad = Quad(quad.points.map(::toUpright))

    private fun Int.isEven(): Boolean = this % CHROMA_SUBSAMPLING == 0

    private val isQuarterTurn: Boolean get() = rotationDegrees == QUARTER || rotationDegrees == THREE_QUARTERS

    private fun requiredBytes(): Int =
        when (format) {
            PixelFormat.RGBA_8888 -> rowStrideBytes * (height - 1) + width * format.bytesPerPixel
            PixelFormat.NV21 -> rowStrideBytes * (height + height / CHROMA_SUBSAMPLING)
        }

    private companion object {
        const val QUARTER = 90
        const val HALF = 180
        const val THREE_QUARTERS = 270
        val VALID_ROTATIONS = setOf(0, QUARTER, HALF, THREE_QUARTERS)
    }
}
