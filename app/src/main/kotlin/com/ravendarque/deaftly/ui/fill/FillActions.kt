package com.ravendarque.deaftly.ui.fill

data class FillActions(
    val onBack: () -> Unit,
    val onEdit: () -> Unit,
    val onShow: () -> Unit,
    val onActivate: (String) -> Unit,
    val onDismiss: () -> Unit,
    val onChoose: (String) -> Unit,
    val onRemove: (String, Int) -> Unit,
    val onScan: () -> Unit,
)
