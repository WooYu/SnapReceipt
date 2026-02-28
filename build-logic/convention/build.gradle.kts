plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "skybound.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "skybound.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidHilt") {
            id = "skybound.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidPublish") {
            id = "skybound.android.publish"
            implementationClass = "AndroidPublishConventionPlugin"
        }
    }
}
