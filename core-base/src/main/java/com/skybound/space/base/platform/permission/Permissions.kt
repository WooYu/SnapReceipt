package com.skybound.space.base.platform.permission

import android.Manifest

object Permissions {
    const val CAMERA = Manifest.permission.CAMERA
    const val READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
    const val WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
    const val ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    const val ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
    const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
}