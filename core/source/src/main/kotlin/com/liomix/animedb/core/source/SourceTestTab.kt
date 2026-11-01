package com.liomix.animedb.core.source

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 数据源测试页契约：每个 `:source:*` 模块实现一次，通过 Hilt `@IntoSet` 注册。
 *
 * 顶部 Tab 由 `:feature:console` 统一渲染，Tab 内容来自这里，
 * 因此新增一个数据源 = 新建一个 `:source:*` 模块 + 实现一个 `SourceTestTab`，
 * 容器模块与 app 模块都不需要改动。
 */
interface SourceTestTab {

    /** Tab 顺序（小在前）。 */
    val order: Int

    /** 稳定标识，用于日后保存选中态 / 上报。 */
    val sourceId: String

    /** Tab 标题。 */
    val displayName: String

    /** 副标题，一句话说明该数据源的接入方式。 */
    val subtitle: String

    /** 该数据源的测试页内容。 */
    @Composable
    fun Content(modifier: Modifier)
}
