plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    val compileSdkVersion = (project.property("android.compileSdk") as String).toInt()
    val minSdkVersion = (project.property("android.minSdk") as String).toInt()
    val javaVersionValue = JavaVersion.toVersion(project.property("java.version") as String)

    namespace = "com.skybound.space.core"
    compileSdk = compileSdkVersion

    defaultConfig {
        minSdk = minSdkVersion
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = javaVersionValue
        targetCompatibility = javaVersionValue
    }
}

kotlin {
    compilerOptions {
        val jvmTargetValue = project.property("kotlin.jvmTarget") as String
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(jvmTargetValue))
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.coroutines.core)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.security.crypto)
    implementation(libs.timber)

    testImplementation(libs.junit)
}
