plugins {
    id("animedb.android.application")
}

android {
    namespace = "com.liomix.animedb"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AniRpb 的原生传输只有 arm64-v8a 的 so；直接限定 ABI，避免其他架构装上后崩溃。
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    packaging {
        jniLibs {
            // libzhuque.so 会 dlopen 同目录的 libaddressing_ffi.so，需传统解压安装。
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:console"))
    // 数据源模块：仅为了把各自的 Hilt Tab 绑定装进同一个 Component
    implementation(project(":source:anirpb"))
    implementation(project(":source:anicyk"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
