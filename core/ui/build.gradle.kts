plugins {
    id("animedb.android.core")
    id("animedb.android.compose")
}

android {
    namespace = "com.liomix.animedb.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    implementation(project(":core:common"))

    api(libs.coil.compose)
    api(libs.coil.network.okhttp)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
