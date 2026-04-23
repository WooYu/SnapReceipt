package com.snapreceipt.io.ui.common

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.login.LoginActivity
import com.skybound.space.core.network.auth.SessionEvent

/**
 * 统一处理会话失效后的跳转逻辑，所有 Activity 共用此实现。
 *
 * NEW_TASK + CLEAR_TASK 清空整个任务栈，确保按返回键不会回到旧页面。
 */
fun Activity.navigateToLoginOnSessionExpired(event: SessionEvent) {
    if (event is SessionEvent.RequireLogin) {
        Toast.makeText(this, R.string.session_expired_message, Toast.LENGTH_LONG).show()
    }
    startActivity(
        Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    )
}
