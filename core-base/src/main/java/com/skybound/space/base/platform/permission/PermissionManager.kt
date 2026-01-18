package com.skybound.space.base.platform.permission

import android.content.Context
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

object PermissionManager {
    fun needsPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }
}
