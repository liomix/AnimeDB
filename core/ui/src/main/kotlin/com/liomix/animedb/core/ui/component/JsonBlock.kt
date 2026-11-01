package com.liomix.animedb.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.liomix.animedb.core.common.format.prettyJson
import com.liomix.animedb.core.designsystem.theme.AnimeDbTheme
import com.liomix.animedb.core.designsystem.theme.MonoTextStyle

/**
 * 等宽报文块：自动格式化成缩进 JSON（手机上比横向滚动更好读），可选中复制。
 */
@Composable
fun JsonBlock(
    text: String,
    modifier: Modifier = Modifier,
) {
    val formatted = remember(text) { prettyJson(text) }
    val scroll = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(AnimeDbTheme.consoleColors.monoSurface)
            .verticalScroll(scroll)
            .padding(12.dp),
    ) {
        SelectionContainer {
            Text(
                text = formatted,
                style = MonoTextStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 报文体里的单行亮点（如统计数字）。 */
@Composable
fun MonoLine(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MonoTextStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}
