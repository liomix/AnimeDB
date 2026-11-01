package com.liomix.animedb.core.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * 各 feature 通过 Hilt 提供一个 installer，向应用级 entryProvider 注册自己的目的地。
 *
 * 多模块装配点：`AnimeDbNavHost` 遍历所有 installer，避免 app 模块直接依赖页面实现。
 */
typealias EntryProviderInstaller = EntryProviderScope<NavKey>.() -> Unit
