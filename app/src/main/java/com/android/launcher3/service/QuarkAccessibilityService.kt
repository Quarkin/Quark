package com.android.launcher3.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.android.launcher3.R

class QuarkAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: QuarkAccessibilityService? = null

        fun isServiceEnabled(): Boolean = instance != null

        fun lockScreen(context: Context): Boolean {
            val service = instance
            if (service != null) {
                return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }

            Toast.makeText(
                context,
                context.getString(R.string.accessibility_permission_toast),
                Toast.LENGTH_LONG
            ).show()

            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback gracefully
            }
            return false
        }

        fun expandNotifications(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event inspection needed
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
