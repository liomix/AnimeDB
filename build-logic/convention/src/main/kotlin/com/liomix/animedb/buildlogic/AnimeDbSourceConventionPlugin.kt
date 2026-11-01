package com.liomix.animedb.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * source-* 模块：一个数据源 = 一个模块。
 *
 * 同时承载「数据源实现」与「该数据源的测试页」，因此比 feature 多一层含义：
 * Compose（测试页）+ Hilt（向 Tab 容器注册）+ serialization（报文解析）。
 */
class AnimeDbSourceConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("animedb.android.library")
            pluginManager.apply("animedb.android.compose")
            pluginManager.apply("animedb.android.hilt")
            pluginManager.apply("animedb.android.serialization")
        }
    }
}
