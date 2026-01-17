/**
 * SnapReceipt 项目设置
 * Gradle 项目结构配置
 */

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "SnapReceipt"

include(":app")
include(":base")
include(":core")
include(":data")
include(":feature_ocr")
