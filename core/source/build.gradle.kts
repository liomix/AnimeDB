plugins {
    id("animedb.android.core")
    id("animedb.android.compose")
}

android {
    namespace = "com.liomix.animedb.core.source"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:designsystem"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
