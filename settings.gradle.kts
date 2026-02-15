/**
 * SnapReceipt 项目设置
 * Gradle 项目结构配置
 */

pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google") {
            name = "AliyunGoogleMirror"
            content {
                includeGroupByRegex("com\\.android(\\..+)?")
                includeGroupByRegex("androidx(\\..+)?")
                includeGroupByRegex("com\\.google(\\..+)?")
            }
        }
        maven("https://mirrors.tencent.com/nexus/repository/maven-public") {
            name = "TencentGoogleMirror"
            content {
                includeGroupByRegex("com\\.android(\\..+)?")
                includeGroupByRegex("androidx(\\..+)?")
                includeGroupByRegex("com\\.google(\\..+)?")
            }
        }
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
