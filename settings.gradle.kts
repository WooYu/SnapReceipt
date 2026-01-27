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
include(":core-base")
include(":core-foundation")
include(":core-data")
include(":core-domain")

project(":core-base").projectDir = file("core-base")
project(":core-foundation").projectDir = file("core-foundation")
project(":core-data").projectDir = file("core-data")
project(":core-domain").projectDir = file("core-domain")
