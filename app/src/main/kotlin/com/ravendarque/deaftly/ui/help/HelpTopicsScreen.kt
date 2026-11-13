package com.ravendarque.deaftly.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.help.HelpRepository
import com.ravendarque.deaftly.help.HelpTopic
import com.ravendarque.deaftly.ui.components.DeaftlyTopBar
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TopicRowMinHeight = 56.dp

@Composable
internal fun rememberHelpTopics(repository: HelpRepository): List<HelpTopic>? {
    val topics by produceState<List<HelpTopic>?>(null, repository) {
        value = withContext(Dispatchers.IO) { repository.topics() }
    }
    return topics
}

@Composable
fun HelpTopicsScreen(
    repository: HelpRepository,
    onOpen: (slug: String) -> Unit,
    onBack: () -> Unit,
) {
    val topics = rememberHelpTopics(repository)
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            DeaftlyTopBar(title = stringResource(R.string.route_help), onNavigate = onBack)
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space8),
                verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
            ) {
                topics?.forEach { topic ->
                    TopicRow(topic.title) { onOpen(topic.slug) }
                }
            }
        }
    }
}

@Composable
private fun TopicRow(
    title: String,
    onClick: () -> Unit,
) {
    val shape = Deaftly.shapes.cardSmall
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = TopicRowMinHeight)
                .clip(shape)
                .background(Deaftly.colors.surface, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space14),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Deaftly.colors.ink,
            modifier = Modifier.weight(1f),
        )
        Icon(DeaftlyIcons.Chevron, contentDescription = null, tint = Deaftly.colors.inkSecondary)
    }
}
