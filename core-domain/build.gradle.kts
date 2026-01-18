plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    val compileSdkVersion = (project.property("android.compileSdk") as String).toInt()
    val minSdkVersion = (project.property("android.minSdk") as String).toInt()
    val javaVersionValue = JavaVersion.toVersion(project.property("java.version") as String)

    namespace = "com.snapreceipt.io.domain"
    compileSdk = compileSdkVersion

    defaultConfig {
        minSdk = minSdkVersion
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
    implementation(libs.coroutines.core)
    implementation("javax.inject:javax.inject:1")
}
