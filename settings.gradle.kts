pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "AnimeDB"

// 装配入口
include(":app")

// 基础能力层
include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:navigation")
include(":core:network")
include(":core:source")

// 功能层：顶部 Tab 容器（数据源测试台外壳）
include(":feature:console")

// 数据源层：一个数据源一个模块（数据实现 + 该数据源的测试页）
include(":source:anirpb")
include(":source:anicyk")
