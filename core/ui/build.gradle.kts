plugins {
    id("animedb.android.core")
    id("animedb.android.compose")
}

android {
    namespace = "com.liomix.animedb.core.ui"
}

dependencies {
    api(project(":core:designsystem"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
