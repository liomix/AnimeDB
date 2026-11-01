plugins {
    id("animedb.android.feature")
}

android {
    namespace = "com.liomix.animedb.feature.console"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:source"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)

    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
}
