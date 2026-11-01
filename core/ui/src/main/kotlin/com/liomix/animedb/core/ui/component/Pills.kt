package com.liomix.animedb.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liomix.animedb.core.designsystem.theme.AnimeDbTheme

/** 状态徽标色调。 */
enum class PillTone { Idle, Ok, Warning, Error, Accent }

@Composable
fun pillColors(tone: PillTone): Pair<Color, Color> {
    val console = AnimeDbTheme.consoleColors
    return when (tone) {
        PillTone.Ok -> console.success to console.successContainer
        PillTone.Warning -> console.warning to console.warningContainer
        PillTone.Error -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
        PillTone.Accent -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
        PillTone.Idle -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceContainerHigh
    }
}

/** 带状态圆点的胶囊标签，用于链路自检（LIB / UDS / KEY）与接口状态。 */
@Composable
fun StatusPill(
    label: String,
    tone: PillTone,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val (foreground, background) = pillColors(tone)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(foreground),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 无圆点的信息胶囊（频道、日期、计数）。 */
@Composable
fun InfoChip(
    label: String,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.Idle,
    onClick: (() -> Unit)? = null,
) {
    val (foreground, background) = pillColors(tone)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
        )
    }
}

/** HTTP 方法 / op 的方形角标。 */
@Composable
fun MethodBadge(
    text: String,
    tone: PillTone = PillTone.Accent,
    modifier: Modifier = Modifier,
) {
    val (foreground, background) = pillColors(tone)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 分组小标题。 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/** 数据卡片：等宽数字 + 说明。 */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
