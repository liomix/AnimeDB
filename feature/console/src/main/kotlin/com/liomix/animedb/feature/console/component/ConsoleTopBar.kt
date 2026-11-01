package com.liomix.animedb.feature.console.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liomix.animedb.core.source.SourceTestTab

/**
 * 顶栏即数据源 Tab 本身：不再有标题 / 副标题，Tab 直接贴在状态栏下方。
 *
 * Tab 数量随 `:source:*` 模块增长，后续换 `PrimaryScrollableTabRow` 支持横向滚动。
 */
@Composable
fun ConsoleTopBar(
    tabs: List<SourceTestTab>,
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            if (tabs.isNotEmpty()) {
                TabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedIndex,
                            onClick = { onSelectTab(index) },
                            text = { Text(text = tab.displayName) },
                        )
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )
        }
    }
}
