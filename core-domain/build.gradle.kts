plugins {
    id("skybound.android.library")
    id("kotlin-parcelize")
}

android {
    namespace = "com.snapreceipt.io.domain"
}

dependencies {
    implementation(libs.coroutines.core)
    implementation("javax.inject:javax.inject:1")

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.core)
}
