import java.io.FileInputStream
import java.util.Properties
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    id("skybound.android.application")
    id("skybound.android.hilt")
    id("kotlin-parcelize")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
fun localOrEnv(key: String): String? =
    localProps.getProperty(key) ?: System.getenv(key)

android {
    val appId = project.property("app.applicationId") as String
    val versionCodeValue = (project.property("app.versionCode") as String).toInt()
    val versionNameValue = project.property("app.versionName") as String

    val releaseStoreFilePath = localOrEnv("SNAPRECEIPT_RELEASE_STORE_FILE")
    val releaseStorePassword = localOrEnv("SNAPRECEIPT_RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = localOrEnv("SNAPRECEIPT_RELEASE_KEY_ALIAS")
    val releaseKeyPassword = localOrEnv("SNAPRECEIPT_RELEASE_KEY_PASSWORD")
    val releaseSigningReady = listOf(
        releaseStoreFilePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword
    ).all { !it.isNullOrBlank() }

    if (gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
        && !releaseSigningReady
    ) {
        throw GradleException(
            "Missing release signing configuration. " +
                "Set SNAPRECEIPT_RELEASE_STORE_FILE, SNAPRECEIPT_RELEASE_STORE_PASSWORD, " +
                "SNAPRECEIPT_RELEASE_KEY_ALIAS, SNAPRECEIPT_RELEASE_KEY_PASSWORD " +
                "in local.properties or as environment variables."
        )
    }

    namespace = appId

    defaultConfig {
        applicationId = appId
        versionCode = versionCodeValue
        versionName = versionNameValue
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        viewBinding = true
    }

    bundle {
        language.enableSplit = false
    }

    applicationVariants.all {
        val vName = versionNameValue
        val vCode = versionCodeValue
        val date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            output.outputFileName = if (buildType.name == "release") {
                "SnapReceipt-release-v${vName}-${vCode}.apk"
            } else {
                "SnapReceipt-debug-v${vName}-${vCode}-${date}.apk"
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(project(":core-base"))
    implementation(project(":core-foundation"))
    implementation(project(":core-data"))
    implementation(project(":core-domain"))

    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.exifinterface)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.fragment.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.viewpager2)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.core)
    implementation(libs.timber)
    implementation(libs.glide)
    implementation(libs.swiperefreshlayout)
    implementation(libs.ucrop)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(testFixtures(project(":core-foundation")))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
