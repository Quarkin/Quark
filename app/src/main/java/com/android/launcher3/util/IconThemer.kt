package com.android.launcher3.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.android.launcher3.iconpack.IconPackManager
import com.android.launcher3.model.AppItem
import com.android.launcher3.repository.AppOverride

object IconThemer {
    private val themedIconCache = mutableMapOf<String, ImageBitmap>()
    private val standardIconCache = mutableMapOf<String, ImageBitmap>()
    private val overrideIconCache = mutableMapOf<String, ImageBitmap>()
    private val globalPackIconCache = mutableMapOf<String, ImageBitmap>()

    fun getStandardBitmap(key: String, drawable: Drawable?, size: Int = 144): ImageBitmap? {
        if (drawable == null) return null
        standardIconCache[key]?.let { return it }

        return try {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            val imageBitmap = bitmap.asImageBitmap()
            standardIconCache[key] = imageBitmap
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }

    fun getThemedBitmap(
        key: String,
        drawable: Drawable?,
        containerColor: Color,
        tintColor: Color,
        size: Int = 144
    ): ImageBitmap? {
        if (drawable == null) return null
        val cacheKey = "$key-${containerColor.value}-${tintColor.value}"
        themedIconCache[cacheKey]?.let { return it }

        return try {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = containerColor.toArgb()
                style = Paint.Style.FILL
            }

            val padding = size * 0.04f
            val rect = RectF(padding, padding, size - padding, size - padding)
            val radius = size * 0.38f // Pixel squircle curve

            // Draw tinted squircle container
            canvas.drawRoundRect(rect, radius, radius, bgPaint)

            var handledMonochrome = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
                val monochrome = drawable.monochrome
                if (monochrome != null) {
                    val mono = monochrome.mutate()
                    mono.colorFilter = PorterDuffColorFilter(tintColor.toArgb(), PorterDuff.Mode.SRC_IN)
                    val iconPadding = (size * 0.24f).toInt()
                    mono.setBounds(iconPadding, iconPadding, size - iconPadding, size - iconPadding)
                    mono.draw(canvas)
                    handledMonochrome = true
                }
            }

            if (!handledMonochrome) {
                // Fallback for legacy apps: centered icon inside tinted squircle container
                val iconPadding = (size * 0.22f).toInt()
                drawable.setBounds(iconPadding, iconPadding, size - iconPadding, size - iconPadding)
                drawable.draw(canvas)
            }

            val imageBitmap = bitmap.asImageBitmap()
            themedIconCache[cacheKey] = imageBitmap
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Priority sequence for rendering an app icon:
     * 1. Per-App Custom Override (Custom Icon Pack Resource)
     * 2. Global Icon Pack (Matched via appfilter.xml)
     * 3. Material You Dynamic Theming (AdaptiveIconDrawable.getMonochrome() / Monet squircle)
     * 4. System Default Icon
     */
    fun getRenderedIcon(
        app: AppItem,
        override: AppOverride?,
        globalIconPackPackage: String?,
        globalAppFilter: Map<String, String>?,
        iconPackManager: IconPackManager,
        isThemedIcons: Boolean,
        containerColor: Color,
        tintColor: Color,
        size: Int = 144
    ): ImageBitmap? {
        // 1. Per-App Custom Override
        if (!override?.customIconPackPackage.isNullOrBlank() && !override?.customIconDrawableName.isNullOrBlank()) {
            val cacheKey = "${override.customIconPackPackage}/${override.customIconDrawableName}"
            overrideIconCache[cacheKey]?.let { return it }
            val customDrawable = iconPackManager.loadDrawable(
                override.customIconPackPackage!!,
                override.customIconDrawableName!!
            )
            if (customDrawable != null) {
                val customBitmap = getStandardBitmap(cacheKey, customDrawable, size)
                if (customBitmap != null) {
                    overrideIconCache[cacheKey] = customBitmap
                    return customBitmap
                }
            }
        }

        // 2. Global Icon Pack (Matched via appfilter.xml)
        if (!globalIconPackPackage.isNullOrBlank() && globalAppFilter != null) {
            val drawableName = globalAppFilter[app.componentKey] ?: globalAppFilter[app.packageName]
            if (!drawableName.isNullOrBlank()) {
                val cacheKey = "global-$globalIconPackPackage-$drawableName"
                globalPackIconCache[cacheKey]?.let { return it }
                val packDrawable = iconPackManager.loadDrawable(globalIconPackPackage, drawableName)
                if (packDrawable != null) {
                    val packBitmap = getStandardBitmap(cacheKey, packDrawable, size)
                    if (packBitmap != null) {
                        globalPackIconCache[cacheKey] = packBitmap
                        return packBitmap
                    }
                }
            }
        }

        // 3. Material You Dynamic Theming
        if (isThemedIcons) {
            return getThemedBitmap(app.componentKey, app.icon, containerColor, tintColor, size)
        }

        // 4. System Default Icon
        return getStandardBitmap(app.componentKey, app.icon, size)
    }

    /**
     * Resolves app label according to priority:
     * 1. Per-App Custom Label
     * 2. System Default Label
     */
    fun getRenderedLabel(app: AppItem, override: AppOverride?): String {
        return if (!override?.customLabel.isNullOrBlank()) {
            override.customLabel!!
        } else {
            app.label
        }
    }

    fun clearCache() {
        themedIconCache.clear()
        standardIconCache.clear()
        overrideIconCache.clear()
        globalPackIconCache.clear()
    }
}
