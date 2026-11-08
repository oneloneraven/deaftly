package com.ravendarque.deaftly.ui.home

import com.ravendarque.deaftly.navigation.Route

data class HomeActions(
    val onNavigate: (Route) -> Unit,
    val onQueryChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
    val onCategorySelected: (String?) -> Unit,
    val onSetPinned: (String, Boolean) -> Unit,
    val onSetCategory: (String, String) -> Unit,
)
