package com.liomix.animedb.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/** core-* 模块：Android Library + Hilt + kotlinx.serialization。 */
class AnimeDbCoreConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("animedb.android.library")
            pluginManager.apply("animedb.android.hilt")
            pluginManager.apply("animedb.android.serialization")
        }
    }
}
