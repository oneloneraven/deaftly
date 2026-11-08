package com.ravendarque.deaftly.ui.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.components.DeaftlyTopBar
import com.ravendarque.deaftly.ui.components.PrimaryButton
import com.ravendarque.deaftly.ui.components.SecondaryButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private val BadgeSize = 88.dp
private val BadgeIconSize = 40.dp

data class CameraGateActions(
    val onAllow: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onTypeInstead: () -> Unit,
    val onClose: () -> Unit,
)

@Composable
fun CameraGate(
    access: CameraAccess,
    unavailable: Boolean,
    actions: CameraGateActions,
    camera: @Composable () -> Unit,
) {
    val onAllow = actions.onAllow
    val onOpenSettings = actions.onOpenSettings
    val onTypeInstead = actions.onTypeInstead
    val onClose = actions.onClose
    when {
        unavailable -> {
            CameraNotice(
                title = stringResource(R.string.ocr_unavailable_title),
                body = stringResource(R.string.ocr_unavailable_body),
                onClose = onClose,
            ) {
                PrimaryButton(stringResource(R.string.ocr_type_instead), onTypeInstead, Modifier.fillMaxWidth(), DeaftlyIcons.Type)
            }
        }

        access == CameraAccess.Granted -> {
            camera()
        }

        access == CameraAccess.NotAsked -> {
            CameraNotice(
                title = stringResource(R.string.ocr_permission_title),
                body = stringResource(R.string.ocr_permission_body),
                onClose = onClose,
            ) {
                PrimaryButton(stringResource(R.string.ocr_permission_allow), onAllow, Modifier.fillMaxWidth(), DeaftlyIcons.Scan)
                SecondaryButton(stringResource(R.string.ocr_type_instead), onTypeInstead, Modifier.fillMaxWidth(), DeaftlyIcons.Type)
            }
        }

        access == CameraAccess.Denied -> {
            CameraNotice(
                title = stringResource(R.string.ocr_denied_title),
                body = stringResource(R.string.ocr_denied_body),
                onClose = onClose,
            ) {
                PrimaryButton(stringResource(R.string.ocr_type_instead), onTypeInstead, Modifier.fillMaxWidth(), DeaftlyIcons.Type)
                SecondaryButton(stringResource(R.string.ocr_permission_allow), onAllow, Modifier.fillMaxWidth(), DeaftlyIcons.Scan)
            }
        }

        else -> {
            CameraNotice(
                title = stringResource(R.string.ocr_denied_title),
                body = stringResource(R.string.ocr_blocked_body),
                onClose = onClose,
            ) {
                PrimaryButton(stringResource(R.string.ocr_type_instead), onTypeInstead, Modifier.fillMaxWidth(), DeaftlyIcons.Type)
                SecondaryButton(stringResource(R.string.ocr_open_settings), onOpenSettings, Modifier.fillMaxWidth(), DeaftlyIcons.Settings)
            }
        }
    }
}

@Composable
private fun CameraNotice(
    title: String,
    body: String,
    onClose: () -> Unit,
    actions: @Composable () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            DeaftlyTopBar(
                title = stringResource(R.string.route_ocr),
                onNavigate = onClose,
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Deaftly.spacing.space24, vertical = Deaftly.spacing.space20),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space16, Alignment.CenterVertically),
            ) {
                Box(
                    modifier = Modifier.size(BadgeSize).background(Deaftly.colors.accentSoft, Deaftly.shapes.pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        DeaftlyIcons.Scan,
                        contentDescription = null,
                        modifier = Modifier.size(BadgeIconSize),
                        tint = Deaftly.colors.accentStrong,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Deaftly.colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Deaftly.colors.inkSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Deaftly.spacing.space20,
                            end = Deaftly.spacing.space20,
                            bottom = Deaftly.spacing.space24,
                        ),
                verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
            ) {
                actions()
            }
        }
    }
}
