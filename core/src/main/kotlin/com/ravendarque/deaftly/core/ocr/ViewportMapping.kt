package com.ravendarque.deaftly.core.ocr

/**
 * Maps points in the upright frame to viewport units, where the part of the frame that is visible
 * on screen (the preview's crop) spans 0 to 1 in both directions.
 */
public class ViewportMapping(
    private val visible: BoundingBox,
) {
    init {
        require(visible.width > 0f && visible.height > 0f) { "Visible area must not be empty" }
    }

    public fun toViewport(point: Point): Point = Point((point.x - visible.left) / visible.width, (point.y - visible.top) / visible.height)

    public fun toViewport(quad: Quad): Quad = Quad(quad.points.map(::toViewport))

    public companion object {
        public fun of(
            frame: RecognitionFrame,
            crop: BoundingBox?,
        ): ViewportMapping =
            ViewportMapping(
                crop?.let { frame.toUpright(it) } ?: BoundingBox(0f, 0f, frame.uprightWidth.toFloat(), frame.uprightHeight.toFloat()),
            )
    }
}
