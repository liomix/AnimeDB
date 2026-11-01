plugins {
    `kotlin-dsl`
}

group = "com.liomix.animedb.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("animedbAndroidApplication") {
            id = "animedb.android.application"
            implementationClass = "com.liomix.animedb.buildlogic.AnimeDbApplicationConventionPlugin"
        }
        register("animedbAndroidLibrary") {
            id = "animedb.android.library"
            implementationClass = "com.liomix.animedb.buildlogic.AnimeDbLibraryConventionPlugin"
        }
        register("animedbAndroidCompose") {
            id = "animedb.android.compose"
            implementationClass = "com.liomix.animedb.buildlogic.AnimeDbComposeConventionPlugin"
        }
        register("animedbAndroidHilt") {
            id = "animedb.android.hilt"
            implementationClass = "com.liomix.animedb.buildlogic.AnimeDbHiltConventionPlugin"
        }
        register("animedbAndroidSerialization") {
            id = "animedb.android.serialization"
            implementationClass = "com.liomix.animedb.buildlogic.AnimeDbSerializationConventionPlugin"
        }
        register("animedbAndroidCore") {
            id = "animedb.android.core"
            implementationClass = "com.liomix.animedb.buildlogic.AnimeDbCoreConventionPlugin"
        }
        register("animedbAndroidFeature") {
            id = "animedb.android.feature"
            implementationClass = "com.liomix.animedb.buildlogic.AnimeDbFeatureConventionPlugin"
        }
        register("animedbAndroidSource") {
            id = "animedb.android.source"
            implementationClass = "com.liomix.animedb.buildlogic.AnimeDbSourceConventionPlugin"
        }
    }
}
