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
import com.android.launcher3.iconpack.IconPackManager
import com.android.launcher3.util.IconThemer

@Composable
fun AsyncPackIcon(
    drawableName: String,
    iconPackPackage: String,
    modifier: Modifier = Modifier
) {
    var drawable by remember { mutableStateOf<Drawable?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(drawableName, iconPackPackage) {
        val cacheKey = "$iconPackPackage:$drawableName"
        
        // 1. Instantly load from RAM if available
        val cachedIcon = GlobalIconCache.bitmapCache.get(cacheKey)
        if (cachedIcon != null) {
            drawable = cachedIcon
            return@LaunchedEffect
        }
        
        // 2. Otherwise, fetch on background thread and save to RAM
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var resources = GlobalIconCache.resourceCache[iconPackPackage]
                if (resources == null) {
                    resources = context.createPackageContext(iconPackPackage, 0).resources
                    GlobalIconCache.resourceCache[iconPackPackage] = resources
                }
                
                val resId = resources.getIdentifier(drawableName, "drawable", iconPackPackage)
                if (resId != 0) {
                    val rawDrawable = resources.getDrawable(resId, null)
                    
                    // Rasterize and downscale the heavy vector to a lightweight 144x144 bitmap
                    val bitmap = android.graphics.Bitmap.createBitmap(144, 144, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    rawDrawable.setBounds(0, 0, 144, 144)
                    rawDrawable.draw(canvas)
                    
                    val finalDrawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
                    
                    GlobalIconCache.bitmapCache.put(cacheKey, finalDrawable)
                    drawable = finalDrawable
                }
            } catch (e: Exception) { 
                e.printStackTrace() 
            }
        }
    }

    if (drawable != null) {
        val bitmap = remember(drawable, iconPackPackage, drawableName) {
            IconThemer.getStandardBitmap("$iconPackPackage:$drawableName", drawable, 144)
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
