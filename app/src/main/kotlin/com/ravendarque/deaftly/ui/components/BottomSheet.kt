package com.ravendarque.deaftly.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.ui.theme.Deaftly

private val HandleWidth = 40.dp
private val HandleHeight = 5.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeaftlyBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = Deaftly.shapes.sheet,
        containerColor = Deaftly.colors.surface,
        contentColor = Deaftly.colors.ink,
        dragHandle = { SheetHandle() },
    ) {
        SheetBody(title, content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetHandle() {
    BottomSheetDefaults.DragHandle(
        width = HandleWidth,
        height = HandleHeight,
        color = Deaftly.colors.lineStrong,
    )
}

@Composable
internal fun SheetBody(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = Deaftly.spacing.space20,
                    end = Deaftly.spacing.space20,
                    bottom = Deaftly.spacing.space24,
                ),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space14),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}
