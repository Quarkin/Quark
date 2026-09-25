package com.android.launcher3.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AtAGlanceWidget(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d"))

    val textStyle = TextStyle(
        shadow = Shadow(
            color = Color(0x99000000),
            blurRadius = 4f,
            offset = Offset(0f, 2f)
        )
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(start = 28.dp, top = 32.dp)
            .testTag("at_a_glance_widget")
            .clickable {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                    intent.setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.search.weather.WeatherExportedActivity")
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val fallback = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=weather"))
                        fallback.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(fallback)
                    } catch (ignored: Exception) {
                    }
                }
            }
    ) {
        // Date
        Text(
            text = currentDate,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            style = textStyle
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Weather Icon
        Icon(
            imageVector = Icons.Rounded.WbSunny,
            contentDescription = null,
            tint = Color(0xFFFCC936),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Weather Text
        Text(
            text = "32°C • Bhubaneswar",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            style = textStyle
        )
    }
}
