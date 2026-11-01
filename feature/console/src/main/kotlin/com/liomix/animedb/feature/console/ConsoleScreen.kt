package com.liomix.animedb.feature.console

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.liomix.animedb.core.source.SourceTestTab
import com.liomix.animedb.feature.console.component.ConsoleTopBar

/**
 * 无状态测试台：顶部 Tab 一栏对应一个数据源，内容区就是该数据源的测试页。
 */
@Composable
fun ConsoleScreen(
    tabs: List<SourceTestTab>,
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ConsoleTopBar(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onSelectTab = onSelectTab,
            )
        },
    ) { innerPadding ->
        val current = tabs.getOrNull(selectedIndex)
        if (current == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "还没有注册任何数据源",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            current.Content(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            )
        }
    }
}
