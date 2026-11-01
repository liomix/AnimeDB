package com.liomix.animedb.source.anicyk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liomix.animedb.core.common.format.hostLabel
import com.liomix.animedb.core.designsystem.theme.AnimeDbTheme
import com.liomix.animedb.core.designsystem.theme.MonoTextStyle
import com.liomix.animedb.core.ui.component.CoverImage
import com.liomix.animedb.core.ui.component.InfoChip
import com.liomix.animedb.core.ui.component.MethodBadge
import com.liomix.animedb.core.ui.component.PillTone
import com.liomix.animedb.core.ui.component.SectionLabel
import com.liomix.animedb.core.ui.component.StatTile
import com.liomix.animedb.core.ui.util.openUrl
import com.liomix.animedb.source.anicyk.data.CykView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.CykStructuredContent(view: CykView) {
    when (view) {
        CykView.None -> Hint("运行一个接口后，结构化结果会显示在这里。")
        CykView.Plain -> Hint("该接口未做结构化渲染，切到「JSON」查看原始报文。")
        is CykView.Chips -> {
            SectionLabel(text = view.heading)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                view.items.forEach { item -> InfoChip(label = item, tone = PillTone.Accent) }
            }
        }
        is CykView.Config -> {
            SectionLabel(text = view.heading)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                view.tiles.forEach { (value, label) ->
                    StatTile(value = value, label = label, modifier = Modifier.weight(1f))
                }
            }
            if (view.sources.isNotEmpty()) {
                SectionLabel(text = "首页源", modifier = Modifier.padding(top = 6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    view.sources.forEach { InfoChip(label = it, tone = PillTone.Accent) }
                }
            }
            if (view.appWeb.isNotBlank()) {
                SectionLabel(text = "官网 / 激活页", modifier = Modifier.padding(top = 6.dp))
                LinkRow(url = view.appWeb)
            }
            if (view.hosts.isNotEmpty()) {
                SectionLabel(
                    text = "源站 hosts",
                    trailing = "前 ${minOf(12, view.hosts.size)} / ${view.hostsTotal}",
                    modifier = Modifier.padding(top = 6.dp),
                )
                MonoCard(
                    text = view.hosts.take(12).joinToString("\n") { "• $it" } +
                        if (view.hostsTotal > 12) "\n… 还有 ${view.hostsTotal - 12} 个" else "",
                )
            }
            view.updateInfo?.let { info ->
                SectionLabel(text = "更新信息", modifier = Modifier.padding(top = 6.dp))
                MonoCard(text = info)
            }
        }
        is CykView.Days -> {
            SectionLabel(text = view.heading)
            view.days.forEach { day ->
                if (day.items.isEmpty()) return@forEach
                Row(
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "第 ${day.index} 天",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AnimeDbTheme.consoleColors.warning,
                    )
                    InfoChip(label = "${day.items.size} 部")
                }
                day.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { /* 链接在右侧箭头点击时打开 */ }
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
                            if (item.chapter.isNotBlank()) {
                                Text(
                                    text = item.chapter,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AnimeDbTheme.consoleColors.warning,
                                )
                            }
                            Text(
                                text = hostLabel(item.link),
                                style = MonoTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        is CykView.Rules -> {
            SectionLabel(text = view.heading)
            view.rows.forEach { (code, name) ->
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
                        text = code,
                        style = MonoTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        is CykView.Links -> {
            val context = LocalContext.current
            SectionLabel(text = view.heading)
            view.groups.forEach { group ->
                Text(
                    text = group.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (group.status.contains("SUCCESS", ignoreCase = true)) {
                        AnimeDbTheme.consoleColors.success
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(top = 6.dp),
                )
                group.links.forEach { url ->
                    LinkRow(url = url, onClick = { context.openUrl(url) })
                }
            }
        }
        is CykView.Menu -> {
            val context = LocalContext.current
            SectionLabel(text = view.heading)
            view.rows.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(enabled = item.target.isNotBlank()) { context.openUrl(item.target) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        if (item.vip) {
                            MethodBadge(text = "VIP", tone = PillTone.Warning)
                        }
                    }
                    if (item.desc.isNotBlank()) {
                        Text(
                            text = item.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (item.target.isNotBlank()) {
                        Text(
                            text = hostLabel(item.target),
                            style = MonoTextStyle,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        is CykView.Avatars -> {
            SectionLabel(text = view.heading)
            view.urls.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { url ->
                        CoverImage(url = url, modifier = Modifier.size(72.dp), corner = 12)
                    }
                }
            }
        }
        is CykView.Pay -> {
            SectionLabel(text = view.heading)
            Text(
                text = view.flags,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                view.prices.forEach { price ->
                    InfoChip(label = "¥$price", tone = PillTone.Warning)
                }
            }
            if (view.desc.isNotBlank()) {
                Text(
                    text = "活动：${view.desc}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AnimeDbTheme.consoleColors.warning,
                )
            }
            if (view.intro.isNotBlank()) {
                MonoCard(text = view.intro)
            }
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
private fun LinkRow(
    url: String,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onClick?.invoke() ?: context.openUrl(url) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = hostLabel(url),
            style = MonoTextStyle,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = "↗", style = MonoTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonoCard(text: String) {
    Text(
        text = text,
        style = MonoTextStyle,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AnimeDbTheme.consoleColors.monoSurface)
            .padding(10.dp),
    )
}
