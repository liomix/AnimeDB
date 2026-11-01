package com.liomix.animedb.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liomix.animedb.core.designsystem.theme.AnimeDbTheme
import com.liomix.animedb.core.designsystem.theme.MonoTextStyle
import com.liomix.animedb.core.ui.model.ConsoleOutput
import com.liomix.animedb.core.ui.model.OutputMode
import com.liomix.animedb.core.ui.model.OutputStatus

/**
 * 底部输出窗口：固定在屏幕下方。
 *
 * 一行头：状态点 + 标题 + 「结构化 / JSON」切换；拖动顶部把手可切换高度。
 * 结构化内容由各数据源模块提供（[structured]），这里只负责窗口外壳与滚动。
 */
@Composable
fun OutputPanel(
    output: ConsoleOutput,
    mode: OutputMode,
    onModeChange: (OutputMode) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    resetKey: Any? = null,
    structured: @Composable ColumnScope.() -> Unit,
) {
    val console = AnimeDbTheme.consoleColors
    val dotColor = when (output.status) {
        OutputStatus.Ok -> console.success
        OutputStatus.Failed -> MaterialTheme.colorScheme.error
        OutputStatus.Running -> console.warning
        OutputStatus.Idle -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 拖拽条：点击在「半屏 / 大半个屏」之间切换。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(dotColor),
                )
                Text(
                    text = output.title.ifBlank { "输出" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SegmentedControl(
                    options = listOf("结构化", "JSON"),
                    selectedIndex = mode.ordinal,
                    onSelect = { onModeChange(OutputMode.entries[it]) },
                    modifier = Modifier.width(126.dp),
                    height = 28,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                val structuredScroll = rememberScrollState()
                // 新报文到达时回到顶部，避免沿用上一次的滚动位置。
                LaunchedEffect(output.title, output.json, output.error, resetKey) {
                    structuredScroll.scrollTo(0)
                }
                when {
                    output.error != null -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(structuredScroll),
                    ) {
                        ErrorBlock(output.error)
                    }
                    output.isIdle -> HintBlock("选择接口运行后，报文会显示在这里。")
                    mode == OutputMode.Json -> JsonBlock(output.json)
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(structuredScroll),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = structured,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBlock(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "请求失败",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                style = MonoTextStyle,
            )
        }
    }
}

@Composable
private fun HintBlock(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 页面骨架：顶部控制区固定、中间接口目录、底部输出窗口。
 * 输出窗口高度随 [panelExpanded] 在 38% / 72% 之间过渡。
 */
@Composable
fun ConsoleScaffold(
    panelExpanded: Boolean,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit,
    catalog: @Composable (Modifier) -> Unit,
    output: @Composable (Modifier) -> Unit,
) {
    val fraction by animateFloatAsState(
        targetValue = if (panelExpanded) 0.72f else 0.38f,
        label = "output-panel-height",
    )
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth()) { header() }
        catalog(
            Modifier
                .weight(1f - fraction)
                .fillMaxWidth(),
        )
        output(
            Modifier
                .weight(fraction)
                .fillMaxWidth(),
        )
    }
}
