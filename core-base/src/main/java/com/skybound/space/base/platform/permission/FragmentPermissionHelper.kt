package com.skybound.space.base.platform.permission

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class FragmentPermissionHelper(fragment: Fragment) {
    private var onGranted: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null

    private val launcher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onGranted?.invoke()
        } else {
            onDenied?.invoke()
        }
        onGranted = null
        onDenied = null
    }

    fun requestPermission(
        permission: String,
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        this.onGranted = onGranted
        this.onDenied = onDenied
        launcher.launch(permission)
    }
}
