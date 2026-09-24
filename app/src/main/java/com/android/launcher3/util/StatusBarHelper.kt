package com.android.launcher3.util

import android.content.Context
import android.util.Log

object StatusBarHelper {
    fun expandNotificationShade(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManagerClass.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
        } catch (e: Exception) {
            try {
                val statusBarService = context.getSystemService("statusbar")
                val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
                val expandMethod = statusBarManagerClass.getMethod("expand")
                expandMethod.invoke(statusBarService)
            } catch (e2: Exception) {
                Log.e("StatusBarHelper", "Unable to expand notification shade", e2)
            }
        }
    }
}
