import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * SnapReceipt App 模块 - Gradle Build Script (KTS)
 *
 * 模块职责:
 * - UI 层 (Fragments, Activities)
 * - ViewModel 层 (数据管理)
 * - 依赖注入配置
 * - 资源文件管理
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    val compileSdkVersion = (project.property("android.compileSdk") as String).toInt()
    val minSdkVersion = (project.property("android.minSdk") as String).toInt()
    val targetSdkVersion = (project.property("android.targetSdk") as String).toInt()
    val appId = project.property("app.applicationId") as String
    val versionCodeValue = (project.property("app.versionCode") as String).toInt()
    val versionNameValue = project.property("app.versionName") as String
    val javaVersionValue = JavaVersion.toVersion(project.property("java.version") as String)

    namespace = appId
    compileSdk = compileSdkVersion

    defaultConfig {
        applicationId = appId
        minSdk = minSdkVersion
        targetSdk = targetSdkVersion
        versionCode = versionCodeValue
        versionName = versionNameValue

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = javaVersionValue
        targetCompatibility = javaVersionValue
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // 禁用不必要的资源
    android.bundle {
        language.enableSplit = false
    }
}

android.applicationVariants.all {
    val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
    val versionName = project.property("app.versionName") as String
    val versionCode = project.property("app.versionCode") as String
    outputs.all {
        (this as ApkVariantOutputImpl).outputFileName =
            "SnapReceipt-${name}-v${versionName}-${versionCode}-${timestamp}.apk"
    }
}

kotlin {
    compilerOptions {
        val jvmTargetValue = project.property("kotlin.jvmTarget") as String
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(jvmTargetValue))
        freeCompilerArgs.addAll(
            listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
            )
        )
    }
}

dependencies {
    // Core 依赖
    implementation(project(":core-base"))
    implementation(project(":core-foundation"))
    implementation(project(":core-data"))
    implementation(project(":core-domain"))
    implementation(project(":feature_ocr"))

    // 应用依赖
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.fragment.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.core)
    implementation(libs.hilt.android)
    implementation(libs.swiperefreshlayout)
    implementation(libs.timber)
    implementation(libs.ucrop)
    ksp(libs.hilt.compiler)

    // 测试依赖
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
