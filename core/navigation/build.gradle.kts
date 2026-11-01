plugins {
    id("animedb.android.core")
}

android {
    namespace = "com.liomix.animedb.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.compose.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
