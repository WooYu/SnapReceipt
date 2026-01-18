package com.skybound.space.base.presentation.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavOptions
import com.skybound.space.base.presentation.NavigationCommand

/**
 * 基于 Navigation Component 的默认导航实现，供 Activity/Fragment 选择性复用。
 */
class Navigator(
    private val navControllerProvider: () -> NavController?
) {

    fun navigate(command: NavigationCommand) {
        val controller = navControllerProvider() ?: return
        val navOptions = NavOptions.Builder().apply {
            setLaunchSingleTop(command.launchSingleTop)
            command.popUpTo?.let { destination ->
                setPopUpTo(destination, command.inclusive)
            }
        }.build()
        val request = NavDeepLinkRequest.Builder.fromUri(Uri.parse(command.route)).build()
        controller.navigate(request, navOptions, null)
    }

    fun navigateBack() {
        navControllerProvider()?.popBackStack()
    }
}
