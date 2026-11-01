package com.liomix.animedb.source.anirpb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liomix.animedb.core.designsystem.theme.AnimeDbTheme
import com.liomix.animedb.core.designsystem.theme.MonoTextStyle
import com.liomix.animedb.core.ui.component.CoverImage
import com.liomix.animedb.core.ui.component.InfoChip
import com.liomix.animedb.core.ui.component.MethodBadge
import com.liomix.animedb.core.ui.component.PillTone
import com.liomix.animedb.core.ui.component.SectionLabel
import com.liomix.animedb.core.common.format.formatBytes
import com.liomix.animedb.core.common.format.formatMs
import com.liomix.animedb.source.anirpb.data.CalendarGroup
import com.liomix.animedb.source.anirpb.data.ChannelRow
import com.liomix.animedb.source.anirpb.data.RpbProbeRow
import com.liomix.animedb.source.anirpb.data.RpbView
import com.liomix.animedb.source.anirpb.data.VideoItem

/** 输出窗口的结构化内容：按接口形状渲染，未识别的形状回退提示切 JSON。 */
@Composable
fun ColumnScope.RpbStructuredContent(
    view: RpbView,
    onProbeRowClick: (RpbProbeRow) -> Unit,
) {
    when (view) {
        RpbView.None -> Hint("还没有数据，运行一个接口试试。")
        RpbView.Plain -> Hint("该接口未做结构化渲染，切到「JSON」查看原始报文。")
        is RpbView.VideoFeed -> {
            SectionLabel(text = view.heading, trailing = "共 ${view.items.size} 条")
            view.items.forEach { item -> VideoRow(item) }
            view.footer?.let {
                Text(
                    text = it,
                    style = MonoTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        is RpbView.Calendar -> {
            SectionLabel(text = view.heading)
            view.groups.forEach { group -> CalendarSection(group) }
        }
        is RpbView.Channels -> {
            SectionLabel(text = view.heading)
            view.rows.forEach { row -> ChannelRowItem(row) }
        }
        is RpbView.Probes -> {
            SectionLabel(
                text = "全量探测",
                trailing = "${view.okCount}/${view.rows.size} 可用",
            )
            view.rows.forEach { row -> ProbeRowItem(row, onClick = { onProbeRowClick(row) }) }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun VideoRow(item: VideoItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (item.rank > 0) {
            Text(
                text = "${item.rank}",
                style = if (item.rank <= 3) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodySmall
                },
                fontWeight = FontWeight.SemiBold,
                color = rankColor(item.rank),
                modifier = Modifier.width(22.dp),
            )
        }
        CoverImage(url = item.cover, modifier = Modifier.size(width = 44.dp, height = 58.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.badge.isNotBlank()) {
                Text(
                    text = item.badge,
                    style = MaterialTheme.typography.bodySmall,
                    color = AnimeDbTheme.consoleColors.warning,
                    maxLines = 1,
                )
            }
            Text(
                text = item.meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (item.trailing.isNotBlank()) {
            Text(
                text = item.trailing,
                style = MonoTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun rankColor(rank: Int) = when (rank) {
    1 -> AnimeDbTheme.consoleColors.warning
    2 -> MaterialTheme.colorScheme.onSurfaceVariant
    3 -> AnimeDbTheme.consoleColors.warning.copy(alpha = 0.75f)
    else -> MaterialTheme.colorScheme.outline
}

@Composable
private fun CalendarSection(group: CalendarGroup) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        ) {
            Text(
                text = group.time.ifBlank { "--:--" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AnimeDbTheme.consoleColors.warning,
            )
            InfoChip(label = "${group.items.size} 部")
        }
        group.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CoverImage(url = item.cover, modifier = Modifier.size(width = 40.dp, height = 53.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val updateLine = listOf(item.update, item.episode)
                        .filter { it.isNotBlank() && it != "null" }
                        .joinToString(" · ")
                    if (updateLine.isNotBlank()) {
                        Text(
                            text = updateLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = AnimeDbTheme.consoleColors.warning,
                        )
                    }
                    if (item.channel.isNotBlank()) {
                        Text(
                            text = item.channel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelRowItem(row: ChannelRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = row.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(64.dp),
        )
        Text(
            text = row.id,
            style = MonoTextStyle,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = row.code,
            style = MonoTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProbeRowItem(row: RpbProbeRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = row.ok) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${row.op} · code ${row.code} · ${formatBytes(row.bytes)} · ${formatMs(row.ms)}",
                style = MonoTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.error.isNotBlank()) {
                Text(
                    text = row.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        MethodBadge(
            text = if (row.ok) "200" else "ERR",
            tone = if (row.ok) PillTone.Ok else PillTone.Error,
        )
    }
}
