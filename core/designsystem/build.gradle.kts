plugins {
    id("animedb.android.core")
    id("animedb.android.compose")
}

android {
    namespace = "com.liomix.animedb.core.designsystem"
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    debugApi(libs.androidx.compose.ui.tooling)
}
