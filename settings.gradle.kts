pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
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
    }
}

rootProject.name = "SnapReceipt"

include(":app")
include(":core-base")
include(":core-foundation")
include(":core-data")
include(":core-domain")

