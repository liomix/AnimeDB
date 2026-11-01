plugins {
    id("animedb.android.source")
}

android {
    namespace = "com.liomix.animedb.source.anirpb"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:source"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
