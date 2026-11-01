package com.liomix.animedb.source.anicyk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.liomix.animedb.core.source.SourceTestTab
import com.liomix.animedb.core.ui.placeholder.SourcePlaceholder
import javax.inject.Inject

/**
 * AniCyk 数据源测试页（占位）。
 *
 * 接入方式（REST / GraphQL / 签名规则）确定后，在这里补：接口探针、参数表单、
 * 原始报文与解析结果的并排对照。
 */
class AniCykTestTab @Inject constructor() : SourceTestTab {

    override val order: Int = 1

    override val sourceId: String = "anicyk"

    override val displayName: String = "AniCyk"

    override val subtitle: String = "接入方式待确认 · 占位待接入"

    @Composable
    override fun Content(modifier: Modifier) {
        SourcePlaceholder(
            title = "AniCyk 测试台",
            description = "待补充该数据源的协议与鉴权方式。测试页结构与 AniRpb 同构，" +
                "接入后只需替换占位内容。",
            pendingItems = listOf(
                "确认协议与域名（REST / GraphQL / 原生）",
                "确认鉴权与请求头要求",
                "接口探针与参数表单",
                "原始报文 / 解析结果对照",
            ),
            modifier = modifier,
        )
    }
}
