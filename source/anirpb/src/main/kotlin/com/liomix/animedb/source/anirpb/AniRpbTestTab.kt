package com.liomix.animedb.source.anirpb

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.liomix.animedb.core.source.SourceTestTab
import com.liomix.animedb.core.ui.placeholder.SourcePlaceholder
import javax.inject.Inject

/**
 * AniRpb 数据源测试页（占位）。
 *
 * AniRpb 走 libzhuque 原生加密通道而非 HTTP，接入后测试页需要展示：
 * 握手状态、op 名称、请求/解密后报文、分页信息。
 */
class AniRpbTestTab @Inject constructor() : SourceTestTab {

    override val order: Int = 0

    override val sourceId: String = "anirpb"

    override val displayName: String = "AniRpb"

    override val subtitle: String = "原生加密传输 · 占位待接入"

    @Composable
    override fun Content(modifier: Modifier) {
        SourcePlaceholder(
            title = "AniRpb 测试台",
            description = "Mikomi 的主数据源，通过 libzhuque 原生加密通道收发语义请求（非 HTTP），" +
                "因此测试重点不是状态码，而是握手、op 出参与解密后的 JSON。",
            pendingItems = listOf(
                "ZhuqueTransport 初始化与握手结果面板",
                "op 列表（首页热播 / 日历 / 频道）与参数表单",
                "解密后报文的树形展示与字段告警",
                "分页信息（pageinfo）与耗时统计",
            ),
            modifier = modifier,
        )
    }
}
