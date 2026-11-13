package com.ravendarque.deaftly.help

import android.content.res.AssetManager

data class HelpTopic(
    val slug: String,
    val title: String,
    val order: Int,
    val body: String,
)

private const val ASSET_DIRECTORY = "help"
private const val EXTENSION = ".md"
private const val FRONT_MATTER_FENCE = "---"

class HelpRepository(
    private val assets: AssetManager,
) {
    fun topics(): List<HelpTopic> =
        assets
            .list(ASSET_DIRECTORY)
            .orEmpty()
            .filter { it.endsWith(EXTENSION) }
            .mapNotNull { name ->
                val raw = assets.open("$ASSET_DIRECTORY/$name").bufferedReader().use { it.readText() }
                parseHelpTopic(name.removeSuffix(EXTENSION), raw)
            }.sortedBy { it.order }
}

fun parseHelpTopic(
    slug: String,
    raw: String,
): HelpTopic? {
    val lines = raw.removePrefix("\uFEFF").lines()
    val end = frontMatterEnd(lines) ?: return null
    val fields = frontMatterFields(lines.subList(1, end))
    val title = fields["title"]?.removeSurrounding("\"")?.removeSurrounding("'")?.takeIf { it.isNotBlank() }
    val order = fields["order"]?.toIntOrNull()
    val body = lines.drop(end + 1).joinToString("\n").trimStart('\n')
    return if (title == null || order == null) null else HelpTopic(slug, title, order, body)
}

private fun frontMatterEnd(lines: List<String>): Int? =
    if (lines.firstOrNull()?.trim() == FRONT_MATTER_FENCE) {
        lines
            .drop(1)
            .indexOfFirst { it.trim() == FRONT_MATTER_FENCE }
            .takeIf { it >= 0 }
            ?.plus(1)
    } else {
        null
    }

private fun frontMatterFields(lines: List<String>): Map<String, String> =
    lines
        .filter { ':' in it }
        .associate { it.substringBefore(':').trim() to it.substringAfter(':').trim() }
