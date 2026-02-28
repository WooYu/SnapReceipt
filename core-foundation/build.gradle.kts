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
    implementation(libs.security.crypto)
    implementation(libs.timber)
    implementation("javax.inject:javax.inject:1")

    testImplementation(libs.junit)

    testFixturesImplementation(libs.coroutines.core)
    testFixturesImplementation(libs.coroutines.test)
}
