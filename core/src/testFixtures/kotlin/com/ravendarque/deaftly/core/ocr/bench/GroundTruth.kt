package com.ravendarque.deaftly.core.ocr.bench

import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.Point
import com.ravendarque.deaftly.core.ocr.Quad

public data class GroundTruthRegion(
    val text: String,
    val polygon: Quad,
    val box: BoundingBox,
)

public data class GroundTruthLine(
    val text: String,
    val block: Int,
    val polygon: Quad,
    val box: BoundingBox,
    val words: List<GroundTruthRegion>,
)

public data class GroundTruth(
    val id: String,
    val category: String,
    val tags: List<String>,
    val lighting: String,
    val width: Int,
    val height: Int,
    val lines: List<GroundTruthLine>,
) {
    public companion object {
        public fun parse(
            id: String,
            json: String,
        ): GroundTruth {
            val root = MiniJson.parse(json) as Map<*, *>
            val conditions = root["conditions"] as? Map<*, *>
            return GroundTruth(
                id = id,
                category = root["category"] as String,
                tags = (root["tags"] as List<*>).map { it as String },
                lighting = conditions?.get("lighting") as? String ?: "",
                width = (root["width"] as Double).toInt(),
                height = (root["height"] as Double).toInt(),
                lines =
                    (root["lines"] as List<*>).map { raw ->
                        val line = raw as Map<*, *>
                        GroundTruthLine(
                            text = line["text"] as String,
                            block = (line["block"] as Double).toInt(),
                            polygon = quad(line["polygon"]),
                            box = box(line["box"]),
                            words = (line["words"] as List<*>).map { region(it as Map<*, *>) },
                        )
                    },
            )
        }

        private fun region(map: Map<*, *>) = GroundTruthRegion(map["text"] as String, quad(map["polygon"]), box(map["box"]))

        private fun quad(raw: Any?): Quad =
            Quad(
                (raw as List<*>).map { point ->
                    val (x, y) = (point as List<*>).map { (it as Double).toFloat() }
                    Point(x, y)
                },
            )

        private fun box(raw: Any?): BoundingBox {
            val values = (raw as List<*>).map { (it as Double).toFloat() }
            return BoundingBox(values[0], values[1], values[2], values[3])
        }
    }
}
