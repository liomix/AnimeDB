package com.liomix.animedb.feature.console

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ConsoleRoute(
    modifier: Modifier = Modifier,
    viewModel: ConsoleViewModel = hiltViewModel(),
) {
    val selectedIndex by viewModel.selectedIndex.collectAsStateWithLifecycle()

    ConsoleScreen(
        tabs = viewModel.tabs,
        selectedIndex = selectedIndex,
        onSelectTab = viewModel::selectTab,
        modifier = modifier,
    )
}
