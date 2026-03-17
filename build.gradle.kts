/**
 * SnapReceipt - 企业级收据管理应用
 * 根 Gradle 构建脚本 (KTS - Kotlin DSL)
 * 
 * 特点:
 * - 版本集中管理通过 version catalog
 * - 支持跨平台扩展
 * - 企业级代码质量标准
 */

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}
