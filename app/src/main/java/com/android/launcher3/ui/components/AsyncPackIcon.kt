package com.android.launcher3.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.launcher3.iconpack.GlobalIconCache
import com.android.launcher3.util.IconThemer

@Composable
fun AsyncPackIcon(
    drawableName: String,
    iconPackPackage: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var drawable by remember { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(drawableName, iconPackPackage) {
        val cacheKey = "$iconPackPackage:$drawableName"

        // 1. Check RAM first
        val cachedIcon = GlobalIconCache.bitmapCache.get(cacheKey)
        if (cachedIcon != null) {
            drawable = cachedIcon
            return@LaunchedEffect
        }

        // 2. Offload to background thread
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var resources = GlobalIconCache.resourceCache[iconPackPackage]
                if (resources == null) {
                    resources = context.createPackageContext(iconPackPackage, 0).resources
                    GlobalIconCache.resourceCache[iconPackPackage] = resources
                }

                val resId = resources.getIdentifier(drawableName, "drawable", iconPackPackage)
                if (resId != 0) {
                    val loadedDrawable = resources.getDrawable(resId, null)
                    GlobalIconCache.bitmapCache.put(cacheKey, loadedDrawable)
                    drawable = loadedDrawable
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (drawable != null) {
        val bitmap = remember(drawable, iconPackPackage, drawableName) {
            IconThemer.getStandardBitmap("$iconPackPackage:$drawableName", drawable, 128)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = drawableName,
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier.background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            )
        }
    } else {
        Box(
            modifier = modifier.background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        )
    }
}
