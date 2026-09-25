package com.android.launcher3.model

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

@Immutable
data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable? = null
) {
    private var cachedBitmap: ImageBitmap? = null

    val componentKey: String
        get() = "$packageName/$activityName"

    val launchIntent: Intent
        get() = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(packageName, activityName)
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun getImageBitmap(): ImageBitmap? {
        if (cachedBitmap != null) return cachedBitmap
        if (icon == null) return null

        cachedBitmap = try {
            if (icon is BitmapDrawable && icon.bitmap != null) {
                icon.bitmap.asImageBitmap()
            } else {
                val width = if (icon.intrinsicWidth > 0) icon.intrinsicWidth else 96
                val height = if (icon.intrinsicHeight > 0) icon.intrinsicHeight else 96
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                icon.setBounds(0, 0, canvas.width, canvas.height)
                icon.draw(canvas)
                bitmap.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
        return cachedBitmap
    }
}
