package com.ravendarque.deaftly.ui.help

import androidx.compose.ui.platform.UriHandler

private val TopicLink = Regex("""([a-z0-9-]+)\.md(#.*)?""")
private const val SECURE_SCHEME = "https://"

internal class HelpLinkHandler(
    private val external: UriHandler,
    private val onTopic: (String) -> Unit,
) : UriHandler {
    override fun openUri(uri: String) {
        when {
            uri.startsWith(SECURE_SCHEME) -> external.openUri(uri)
            else -> TopicLink.matchEntire(uri)?.let { onTopic(it.groupValues[1]) }
        }
    }
}
