package com.ravendarque.deaftly.ui.typeshow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.components.DeaftlyTopBar
import com.ravendarque.deaftly.ui.components.PrimaryButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun TypeShowScreen(
    onShow: (String) -> Unit,
    onBack: () -> Unit,
) {
    TypeShowContent(rememberTextFieldState(), onShow, onBack)
}

@Composable
fun TypeShowContent(
    field: TextFieldState,
    onShow: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (field.text.isEmpty()) focus.requestFocus() }
    val message = field.text.toString()
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding().imePadding()) {
            DeaftlyTopBar(
                title = stringResource(R.string.home_type_and_show),
                onNavigate = onBack,
                trailing = {
                    DeaftlyIconButton(
                        icon = DeaftlyIcons.Close,
                        contentDescription = stringResource(R.string.type_show_clear),
                        onClick = field::clearText,
                        enabled = message.isNotEmpty(),
                    )
                },
            )
            MessageField(field, focus, Modifier.weight(1f).padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space8))
            PrimaryButton(
                text = stringResource(R.string.fill_show),
                onClick = { onShow(message.trim()) },
                enabled = message.isNotBlank(),
                icon = DeaftlyIcons.Expand,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Deaftly.spacing.space20,
                            top = Deaftly.spacing.space14,
                            end = Deaftly.spacing.space20,
                            bottom = Deaftly.spacing.space24,
                        ),
            )
        }
    }
}

@Composable
private fun MessageField(
    field: TextFieldState,
    focus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.type_show_label)
    val style = MaterialTheme.typography.displaySmall.copy(color = Deaftly.colors.ink)
    BasicTextField(
        state = field,
        modifier =
            modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .semantics { contentDescription = label },
        textStyle = style,
        cursorBrush = SolidColor(Deaftly.colors.accent),
        lineLimits = TextFieldLineLimits.MultiLine(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorator = { inner ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Deaftly.colors.surface, Deaftly.shapes.cardLarge)
                        .padding(Deaftly.spacing.space22),
            ) {
                if (field.text.isEmpty()) {
                    Text(text = stringResource(R.string.type_show_placeholder), style = style, color = Deaftly.colors.inkMuted)
                }
                inner()
            }
        },
    )
}
