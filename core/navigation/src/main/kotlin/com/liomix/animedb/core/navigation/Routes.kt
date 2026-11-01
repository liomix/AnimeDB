package com.liomix.animedb.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 顶级目的地：数据源测试台。
 *
 * 顶部 Tab（每个数据源一个）属于该页面内部状态，不占用返回栈；
 * 后续「请求详情 / 报文对比」等二级页面再以 `@Serializable` 路由压栈。
 */
@Serializable
data object ConsoleDestination : NavKey
