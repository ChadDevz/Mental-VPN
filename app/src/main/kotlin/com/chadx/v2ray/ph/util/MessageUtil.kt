package com.chadx.v2ray.ph.util

import android.content.Context
import android.content.Intent
import com.chadx.v2ray.ph.AppConfig


object MessageUtil {

    fun sendMsg2Service(ctx: Context, what: Int, content: String) {
        sendMsg(ctx, AppConfig.BROADCAST_ACTION_SERVICE, what, content)
    }

    fun sendMsg2UI(ctx: Context, what: Int, content: String) {
        sendMsg(ctx, AppConfig.BROADCAST_ACTION_ACTIVITY, what, content)
    }

    private fun sendMsg(ctx: Context, action: String, what: Int, content: String) {
        try {
            val intent = Intent()
            intent.action = action
            intent.`package` = "com.chadx.v2ray.ph"
            intent.putExtra("key", what)
            intent.putExtra("content", content)
            ctx.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
