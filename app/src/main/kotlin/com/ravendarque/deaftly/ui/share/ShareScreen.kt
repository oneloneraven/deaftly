package com.ravendarque.deaftly.ui.share

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.share.ShareLink
import com.ravendarque.deaftly.ui.components.DeaftlyTopBar
import com.ravendarque.deaftly.ui.components.PrimaryButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private val QrMaxWidth = 320.dp

fun shareChooser(url: String): Intent =
    Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        },
        null,
    )

@Composable
fun ShareScreen(
    onShare: (url: String) -> Unit,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            DeaftlyTopBar(title = stringResource(R.string.route_share), onNavigate = onBack)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space12),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space24),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_share_qr),
                    contentDescription = stringResource(R.string.share_qr_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.widthIn(max = QrMaxWidth).fillMaxWidth().aspectRatio(1f),
                )
                SelectionContainer {
                    Text(
                        text = ShareLink.DISPLAY,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Deaftly.colors.ink,
                        textAlign = TextAlign.Center,
                    )
                }
                PrimaryButton(
                    text = stringResource(R.string.share_link_button),
                    onClick = { onShare(ShareLink.URL) },
                    icon = DeaftlyIcons.Share,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
