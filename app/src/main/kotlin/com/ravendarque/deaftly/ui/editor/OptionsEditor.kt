package com.ravendarque.deaftly.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private const val NO_FOCUS = -1

@Composable
fun OptionsEditor(
    options: List<String>,
    onChange: (List<String>) -> Unit,
) {
    var focusIndex by remember { mutableIntStateOf(NO_FOCUS) }
    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
        FieldLabel(stringResource(R.string.editor_options))
        options.forEachIndexed { index, option ->
            OptionField(
                number = index + 1,
                value = option,
                requestFocus = focusIndex == index,
                onValueChange = { value -> onChange(options.mapIndexed { i, old -> if (i == index) value else old }) },
                onRemove = {
                    focusIndex = NO_FOCUS
                    onChange(options.filterIndexed { i, _ -> i != index })
                },
            )
        }
        ToolButton(
            text = stringResource(R.string.editor_add_option),
            icon = DeaftlyIcons.Plus,
            onClick = {
                focusIndex = options.size
                onChange(options + "")
            },
        )
    }
}

@Composable
private fun OptionField(
    number: Int,
    value: String,
    requestFocus: Boolean,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        label = { Text(stringResource(R.string.editor_option_label, number)) },
        trailingIcon = { DeaftlyIconButton(DeaftlyIcons.Close, stringResource(R.string.editor_remove_option, number), onRemove) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
        shape = Deaftly.shapes.control,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors = editorFieldColors(),
    )
}
