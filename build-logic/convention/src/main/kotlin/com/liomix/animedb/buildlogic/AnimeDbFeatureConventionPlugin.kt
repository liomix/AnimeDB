package com.liomix.animedb.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/** feature-* 模块：Android Library + Compose + Hilt + kotlinx.serialization。 */
class AnimeDbFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("animedb.android.library")
            pluginManager.apply("animedb.android.compose")
            pluginManager.apply("animedb.android.hilt")
            pluginManager.apply("animedb.android.serialization")
        }
    }
}
