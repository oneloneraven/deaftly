package com.ravendarque.deaftly.core.ocr.bench

import java.util.Locale

public object BenchFormat {
    private const val PERCENT = 100.0
    private val TAGS = listOf("chalkboard", "handwriting", "small-print", "rotated", "perspective", "accents")

    public fun json(report: BenchReport): String =
        MiniJson.write(
            mapOf(
                "candidate" to report.candidate,
                "settings" to report.settings,
                "environment" to report.environment,
                "aggregate" to aggregateJson(report.aggregate()),
                "categories" to report.byCategory().mapValues { aggregateJson(it.value) },
                "images" to report.runs.map(::imageJson),
            ),
        )

    public fun markdown(report: BenchReport): String {
        val total = report.aggregate()
        return buildString {
            appendLine("### ${report.candidate}")
            appendLine()
            appendLine(report.settings.entries.joinToString(", ") { "${it.key}: ${it.value}" })
            appendLine()
            appendLine("| Metric | Value |")
            appendLine("|---|---|")
            appendLine("| Images | ${total.images} |")
            appendErrors("order-agnostic word bag", total.bag)
            appendErrors("strict ground-truth order", total.ordered)
            appendLine("| Line recall (IoU >= 0.5) | ${percent(total.lineRecall)} (${total.matchedLines}/${total.truthLines}) |")
            appendLine("| Line precision | ${percent(total.linePrecision)} (${total.matchedLines}/${total.predictedLines}) |")
            appendLine("| Word box recall (IoU >= 0.5) | ${percent(total.wordBoxRecall)} (${total.wordBoxHits}/${total.truthWords}) |")
            appendLine("| Word box recall with matching text | ${percent(total.wordBoxAndTextRecall)} |")
            appendLine("| Latency median / p95 | ${ms(total.medianLatencyMs)} / ${ms(total.p95LatencyMs)} ms |")
            report.environment.forEach { (key, value) -> appendLine("| $key | $value |") }
            appendLine()
            appendLine("| Group | Images | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |")
            appendLine("|---|---|---|---|---|---|---|")
            report.byCategory().forEach { (name, group) -> appendLine(groupRow("category: $name", group)) }
            TAGS.forEach { tag -> report.byTag(tag)?.let { appendLine(groupRow("tag: $tag", it)) } }
            report.byLighting("low-light")?.let { appendLine(groupRow("lighting: low-light", it)) }
            appendLine()
            appendLine("| Image | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |")
            appendLine("|---|---|---|---|---|---|")
            report.runs.forEach { appendLine(imageRow(it)) }
        }
    }

    private fun StringBuilder.appendErrors(
        name: String,
        errors: TextErrors,
    ) {
        appendLine("| CER, $name, case-sensitive | ${percent(errors.charCaseSensitive.rate)} |")
        appendLine("| CER, $name, case-insensitive | ${percent(errors.charCaseInsensitive.rate)} |")
        appendLine("| WER, $name, case-sensitive | ${percent(errors.wordCaseSensitive.rate)} |")
        appendLine("| WER, $name, case-insensitive | ${percent(errors.wordCaseInsensitive.rate)} |")
    }

    private fun imageRow(run: ImageRun): String {
        val score = run.score
        val lines = "${percent(ratio(score.matchedLines, score.truthLines))} (${score.matchedLines}/${score.truthLines})"
        return "| ${score.id} | ${percent(score.bag.charCaseInsensitive.rate)} | ${percent(score.bag.wordCaseInsensitive.rate)} | " +
            "$lines | ${percent(ratio(score.wordBoxHits, score.truthWords))} | ${ms(Stats.median(run.latenciesMs))} |"
    }

    private fun groupRow(
        name: String,
        group: Aggregate,
    ): String =
        "| $name | ${group.images} | ${percent(group.bag.charCaseInsensitive.rate)} | " +
            "${percent(group.bag.wordCaseInsensitive.rate)} | ${percent(group.lineRecall)} | " +
            "${percent(group.wordBoxRecall)} | ${ms(group.medianLatencyMs)} |"

    private fun imageJson(run: ImageRun): Map<String, Any> {
        val score = run.score
        return mapOf(
            "id" to score.id,
            "category" to score.category,
            "tags" to score.tags,
            "lighting" to score.lighting,
            "truthLines" to score.truthLines,
            "predictedLines" to score.predictedLines,
            "matchedLines" to score.matchedLines,
            "truthWords" to score.truthWords,
            "wordBoxHits" to score.wordBoxHits,
            "wordBoxAndTextHits" to score.wordBoxAndTextHits,
            "cerOrdered" to score.ordered.charCaseSensitive.rate,
            "cerOrderedCaseInsensitive" to score.ordered.charCaseInsensitive.rate,
            "werOrdered" to score.ordered.wordCaseSensitive.rate,
            "werOrderedCaseInsensitive" to score.ordered.wordCaseInsensitive.rate,
            "cerBag" to score.bag.charCaseSensitive.rate,
            "cerBagCaseInsensitive" to score.bag.charCaseInsensitive.rate,
            "werBag" to score.bag.wordCaseSensitive.rate,
            "werBagCaseInsensitive" to score.bag.wordCaseInsensitive.rate,
            "latenciesMs" to run.latenciesMs,
            "predictedText" to score.predictedText,
        )
    }

    private fun aggregateJson(aggregate: Aggregate): Map<String, Any> =
        mapOf(
            "images" to aggregate.images,
            "cerBag" to aggregate.bag.charCaseSensitive.rate,
            "cerBagCaseInsensitive" to aggregate.bag.charCaseInsensitive.rate,
            "werBag" to aggregate.bag.wordCaseSensitive.rate,
            "werBagCaseInsensitive" to aggregate.bag.wordCaseInsensitive.rate,
            "cerOrdered" to aggregate.ordered.charCaseSensitive.rate,
            "cerOrderedCaseInsensitive" to aggregate.ordered.charCaseInsensitive.rate,
            "werOrdered" to aggregate.ordered.wordCaseSensitive.rate,
            "werOrderedCaseInsensitive" to aggregate.ordered.wordCaseInsensitive.rate,
            "lineRecall" to aggregate.lineRecall,
            "linePrecision" to aggregate.linePrecision,
            "wordBoxRecall" to aggregate.wordBoxRecall,
            "wordBoxAndTextRecall" to aggregate.wordBoxAndTextRecall,
            "medianLatencyMs" to aggregate.medianLatencyMs,
            "p95LatencyMs" to aggregate.p95LatencyMs,
        )

    private fun ratio(
        part: Int,
        whole: Int,
    ): Double = if (whole == 0) 0.0 else part.toDouble() / whole

    private fun percent(value: Double): String = "%.1f%%".format(Locale.ROOT, value * PERCENT)

    private fun ms(value: Double): String = "%.0f".format(Locale.ROOT, value)
}
