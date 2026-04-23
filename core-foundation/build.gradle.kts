plugins {
    id("skybound.android.library")
    id("skybound.android.publish")
}

android {
    namespace = "com.skybound.space.core"
    testFixtures.enable = true
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.coroutines.core)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.timber)
    implementation("javax.inject:javax.inject:1")

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.truth)

    testFixturesImplementation(libs.coroutines.core)
    testFixturesImplementation(libs.coroutines.test)
}
