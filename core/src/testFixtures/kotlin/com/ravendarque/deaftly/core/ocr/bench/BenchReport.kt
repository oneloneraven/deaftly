package com.ravendarque.deaftly.core.ocr.bench

import kotlin.math.ceil

public data class ImageRun(
    val score: ImageScore,
    val latenciesMs: List<Double>,
)

public data class Aggregate(
    val images: Int,
    val ordered: TextErrors,
    val bag: TextErrors,
    val truthLines: Int,
    val predictedLines: Int,
    val matchedLines: Int,
    val truthWords: Int,
    val wordBoxHits: Int,
    val wordBoxAndTextHits: Int,
    val medianLatencyMs: Double,
    val p95LatencyMs: Double,
) {
    val lineRecall: Double get() = if (truthLines == 0) 0.0 else matchedLines.toDouble() / truthLines
    val linePrecision: Double get() = if (predictedLines == 0) 0.0 else matchedLines.toDouble() / predictedLines
    val wordBoxRecall: Double get() = if (truthWords == 0) 0.0 else wordBoxHits.toDouble() / truthWords
    val wordBoxAndTextRecall: Double get() = if (truthWords == 0) 0.0 else wordBoxAndTextHits.toDouble() / truthWords
}

public object Stats {
    public fun median(values: List<Double>): Double = percentile(values, MEDIAN)

    public fun percentile(
        values: List<Double>,
        percent: Double,
    ): Double {
        if (values.isEmpty()) return Double.NaN
        val sorted = values.sorted()
        val rank = ceil(percent / PERCENT * sorted.size).toInt().coerceIn(1, sorted.size)
        return sorted[rank - 1]
    }

    public const val MEDIAN: Double = 50.0
    public const val P95: Double = 95.0
    private const val PERCENT = 100.0
}

public class BenchReport(
    public val candidate: String,
    public val settings: Map<String, String>,
    public val environment: Map<String, String>,
    public val runs: List<ImageRun>,
) {
    public fun aggregate(selection: List<ImageRun> = runs): Aggregate {
        val scores = selection.map { it.score }
        val latencies = selection.flatMap { it.latenciesMs }
        return Aggregate(
            images = scores.size,
            ordered = scores.fold(TextErrors.ZERO) { sum, score -> sum + score.ordered },
            bag = scores.fold(TextErrors.ZERO) { sum, score -> sum + score.bag },
            truthLines = scores.sumOf { it.truthLines },
            predictedLines = scores.sumOf { it.predictedLines },
            matchedLines = scores.sumOf { it.matchedLines },
            truthWords = scores.sumOf { it.truthWords },
            wordBoxHits = scores.sumOf { it.wordBoxHits },
            wordBoxAndTextHits = scores.sumOf { it.wordBoxAndTextHits },
            medianLatencyMs = Stats.median(latencies),
            p95LatencyMs = Stats.percentile(latencies, Stats.P95),
        )
    }

    public fun byCategory(): Map<String, Aggregate> = runs.groupBy { it.score.category }.toSortedMap().mapValues { aggregate(it.value) }

    public fun byTag(tag: String): Aggregate? = runs.filter { tag in it.score.tags }.takeIf { it.isNotEmpty() }?.let(::aggregate)

    public fun byLighting(lighting: String): Aggregate? =
        runs.filter { it.score.lighting == lighting }.takeIf { it.isNotEmpty() }?.let(::aggregate)

    public fun toJson(): String = BenchFormat.json(this)

    public fun toMarkdown(): String = BenchFormat.markdown(this)
}
