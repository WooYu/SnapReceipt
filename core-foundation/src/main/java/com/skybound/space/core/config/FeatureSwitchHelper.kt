package com.skybound.space.core.config

class FeatureSwitchHelper(
    private val configManager: ConfigManager
) {
    fun isFeatureEnabled(featureKey: String, defaultValue: Boolean = false): Boolean {
        return configManager.getBoolean(featureKey, defaultValue)
    }
}
