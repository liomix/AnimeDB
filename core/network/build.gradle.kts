plugins {
    id("animedb.android.core")
}

android {
    namespace = "com.liomix.animedb.core.network"
}

dependencies {
    api(libs.okhttp)
    api(libs.okhttp.logging)
    api(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
