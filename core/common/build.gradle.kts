plugins {
    id("animedb.android.core")
}

android {
    namespace = "com.liomix.animedb.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
