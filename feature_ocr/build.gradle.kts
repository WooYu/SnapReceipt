/**
 * Feature OCR 模块 - 光学文字识别功能
 * 
 * 职责:
 * - OCR 识别实现 (本地 ML Kit + 后端 API)
 * - 识别结果处理
 * - 工厂模式实现多种 OCR 策略
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    val compileSdkVersion = (project.property("android.compileSdk") as String).toInt()
    val minSdkVersion = (project.property("android.minSdk") as String).toInt()
    val javaVersionValue = JavaVersion.toVersion(project.property("java.version") as String)

    namespace = "com.snapreceipt.io.ocr"
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

    buildFeatures {
        viewBinding = false
    }
}

kotlin {
    compilerOptions {
        val jvmTargetValue = project.property("kotlin.jvmTarget") as String
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(jvmTargetValue))
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    
    implementation(libs.mlkit.text)
    implementation(libs.retrofit)
    implementation(libs.hilt.android)
    implementation(libs.okhttp)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
}
