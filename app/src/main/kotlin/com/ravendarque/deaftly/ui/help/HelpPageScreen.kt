package com.ravendarque.deaftly.ui.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.help.HelpRepository
import com.ravendarque.deaftly.ui.components.DeaftlyTopBar
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun HelpPageScreen(
    slug: String,
    repository: HelpRepository,
    onOpenTopic: (slug: String) -> Unit,
    onBack: () -> Unit,
) {
    val topic = rememberHelpTopics(repository)?.firstOrNull { it.slug == slug }
    val external = LocalUriHandler.current
    val links = remember(external, onOpenTopic) { HelpLinkHandler(external, onOpenTopic) }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            DeaftlyTopBar(title = stringResource(R.string.route_help), onNavigate = onBack)
            if (topic != null) {
                CompositionLocalProvider(LocalUriHandler provides links) {
                    Markdown(
                        content = topic.body,
                        colors =
                            markdownColor(
                                text = Deaftly.colors.ink,
                                dividerColor = Deaftly.colors.line,
                                codeBackground = Deaftly.colors.surfaceSunken,
                            ),
                        typography =
                            markdownTypography(
                                h1 = MaterialTheme.typography.headlineMedium,
                                h2 = MaterialTheme.typography.headlineSmall,
                                h3 = MaterialTheme.typography.titleLarge,
                                h4 = MaterialTheme.typography.titleMedium,
                                h5 = MaterialTheme.typography.titleSmall,
                                h6 = MaterialTheme.typography.titleSmall,
                            ),
                        padding = markdownPadding(block = Deaftly.spacing.space8),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space8),
                    )
                }
            }
        }
    }
}
