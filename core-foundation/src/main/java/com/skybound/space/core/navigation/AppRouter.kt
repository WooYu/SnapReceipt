package com.skybound.space.core.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

data class RouteRequest(
    val route: String? = null,
    val target: Class<*>? = null,
    val extras: Bundle? = null,
    val flags: Int? = null
)

interface RouteInterceptor {
    fun intercept(request: RouteRequest): Boolean
}

interface RouteFallback {
    fun onFallback(request: RouteRequest, reason: String? = null)
}

object NoOpRouteFallback : RouteFallback {
    override fun onFallback(request: RouteRequest, reason: String?) = Unit
}

class AppRouter(
    private val context: Context,
    private val interceptors: List<RouteInterceptor> = emptyList(),
    private val fallback: RouteFallback = NoOpRouteFallback
) {
    fun navigate(request: RouteRequest) {
        if (!interceptors.all { it.intercept(request) }) {
            fallback.onFallback(request, "intercepted")
            return
        }
        val intent = buildIntent(request) ?: run {
            fallback.onFallback(request, "invalid_request")
            return
        }
        request.flags?.let { intent.addFlags(it) } ?: applyDefaultFlags(intent)
        runCatching { context.startActivity(intent) }
            .onFailure { fallback.onFallback(request, it.message) }
    }

    fun navigate(route: String, extras: Bundle? = null) {
        navigate(RouteRequest(route = route, extras = extras))
    }

    fun navigate(target: Class<*>, extras: Bundle? = null) {
        navigate(RouteRequest(target = target, extras = extras))
    }

    private fun buildIntent(request: RouteRequest): Intent? {
        val intent = when {
            request.target != null -> Intent(context, request.target)
            request.route != null -> Intent(Intent.ACTION_VIEW, Uri.parse(request.route))
            else -> null
        } ?: return null
        request.extras?.let { intent.putExtras(it) }
        return intent
    }

    private fun applyDefaultFlags(intent: Intent) {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
