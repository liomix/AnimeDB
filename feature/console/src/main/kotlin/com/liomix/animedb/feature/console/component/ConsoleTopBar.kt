package com.liomix.animedb.feature.console.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.liomix.animedb.core.source.SourceTestTab

/**
 * 顶部栏：品牌标题 + 副标题（当前数据源接法）+ 数据源 Tab。
 *
 * Tab 数量随 `:source:*` 模块增长，后续换 `PrimaryScrollableTabRow` 支持横向滚动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleTopBar(
    tabs: List<SourceTestTab>,
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "AnimeDB",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = tabs.getOrNull(selectedIndex)?.subtitle ?: "动漫数据源测试台",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        if (tabs.isNotEmpty()) {
            TabRow(selectedTabIndex = selectedIndex) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == selectedIndex,
                        onClick = { onSelectTab(index) },
                        text = { Text(text = tab.displayName) },
                    )
                }
            }
        }
    }
}
