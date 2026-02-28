package com.skybound.space.base.platform.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionManager {
    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun needsPermission(context: Context, permission: String): Boolean =
        !isGranted(context, permission)

    fun allGranted(context: Context, permissions: List<String>): Boolean =
        permissions.all { isGranted(context, it) }

    fun needsAny(context: Context, permissions: List<String>): Boolean =
        permissions.any { needsPermission(context, it) }

    fun shouldShowRationale(fragment: Fragment, permission: String): Boolean =
        fragment.shouldShowRequestPermissionRationale(permission)

    fun filterDenied(context: Context, permissions: List<String>): List<String> =
        permissions.filter { needsPermission(context, it) }
}