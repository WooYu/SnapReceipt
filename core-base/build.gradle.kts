plugins {
    id("skybound.android.library")
    id("skybound.android.publish")
}

android {
    namespace = "com.skybound.space.base"
}

dependencies {
    implementation(project(":core-foundation"))

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.fragment.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
}
