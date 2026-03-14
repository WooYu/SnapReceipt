plugins {
    id("skybound.android.library")
    id("skybound.android.hilt")
}

android {
    namespace = "com.snapreceipt.io.data"
}

dependencies {
    implementation(project(":core-foundation"))
    implementation(project(":core-domain"))

    api(libs.retrofit)
    api(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.core)
    testImplementation(testFixtures(project(":core-foundation")))
}
