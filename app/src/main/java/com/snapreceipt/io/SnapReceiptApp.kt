package com.snapreceipt.io

import android.app.Application
import com.snapreceipt.io.di.AppDiConfig
import com.skybound.space.core.config.AppConfig
import com.skybound.space.core.di.AppInjector
import com.skybound.space.core.util.LogHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SnapReceiptApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppConfig.init(isDebug = BuildConfig.DEBUG)
        LogHelper.init(isDebug = BuildConfig.DEBUG)
        LogHelper.i(
            "AppConfig",
            "init debug=${BuildConfig.DEBUG} baseUrl=${AppConfig.baseUrl} version=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        )
        AppInjector.applyConfig(AppDiConfig(this))
    }
}
