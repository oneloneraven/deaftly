package com.oneloneraven.deaftly.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.oneloneraven.deaftly.R
import com.oneloneraven.deaftly.core.template.PersonalField
import com.oneloneraven.deaftly.ui.theme.Deaftly

@Composable
internal fun FillsWith(
    selected: PersonalField?,
    onSelect: (PersonalField?) -> Unit,
) {
    val c = Deaftly.colors
    val group =
        Modifier
            .fillMaxWidth()
            .clip(Deaftly.shapes.controlSmall)
            .background(c.surfaceSunken, Deaftly.shapes.controlSmall)
            .selectableGroup()
            .padding(Deaftly.spacing.space4)
    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
        Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4)) {
            FieldLabel(stringResource(R.string.editor_fills_with))
            Text(stringResource(R.string.editor_fills_with_hint), style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        }
        Column(group, verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4)) {
            (listOf(null) + PersonalField.entries).forEach { field ->
                Segment(stringResource(field.labelRes()), field == selected, { onSelect(field) }, Modifier.fillMaxWidth())
            }
        }
    }
}

private fun PersonalField?.labelRes() =
    when (this) {
        null -> R.string.editor_fills_with_none
        PersonalField.FirstName -> R.string.editor_fills_with_first_name
        PersonalField.LastName -> R.string.editor_fills_with_last_name
        PersonalField.FullName -> R.string.editor_fills_with_full_name
        PersonalField.LegalName -> R.string.editor_fills_with_legal_name
    }
