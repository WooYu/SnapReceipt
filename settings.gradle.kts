pluginManagement {
    includeBuild("build-logic")

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

