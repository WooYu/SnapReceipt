import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.android")

        val compileSdkVersion = prop("android.compileSdk").toInt()
        val minSdkVersion = prop("android.minSdk").toInt()
        val targetSdkVersion = prop("android.targetSdk").toInt()
        val javaVersion = JavaVersion.toVersion(prop("java.version"))
        val jvmTargetValue = JvmTarget.fromTarget(prop("kotlin.jvmTarget"))

        extensions.configure<BaseAppModuleExtension> {
            compileSdk = compileSdkVersion

            defaultConfig {
                minSdk = minSdkVersion
                targetSdk = targetSdkVersion
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            compileOptions {
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }

            buildFeatures {
                buildConfig = true
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(jvmTargetValue)
            }
        }
    }
}

private fun Project.prop(key: String): String =
    property(key) as String
