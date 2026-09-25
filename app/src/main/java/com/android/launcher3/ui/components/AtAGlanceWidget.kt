package com.android.launcher3.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.R

@androidx.compose.runtime.Composable
fun Smartspace() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = androidx.compose.runtime.remember { LauncherPrefs(context) }
    
    val cachedTemp by prefs.weatherTempFlow.collectAsState(initial = "...")
    val lastFetch by prefs.lastFetchFlow.collectAsState(initial = System.currentTimeMillis())

    val currentDate = java.time.LocalDate.now().format(
        java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d")
    )

    // 1. Smart Caching: Only fetch if 60 minutes (3,600,000 ms) have passed
    androidx.compose.runtime.LaunchedEffect(lastFetch) {
        if (System.currentTimeMillis() - lastFetch > 3600000L || cachedTemp == "...") {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val url = java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=20.296&longitude=85.824&current=temperature_2m")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 3000
                    val response = connection.inputStream.bufferedReader().readText()
                    
                    val json = org.json.JSONObject(response)
                    val temp = json.getJSONObject("current").getDouble("temperature_2m")
                    val newTempStr = "${kotlin.math.round(temp).toInt()}°C"
                    
                    prefs.saveWeather(newTempStr, System.currentTimeMillis())
                } catch (e: Exception) {
                    // Let Compose safely kill the coroutine when the widget leaves the screen
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    
                    // Log actual network/parsing errors silently
                    e.printStackTrace() 
                }
            }
        }
    }

    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
    ) {
        androidx.compose.material3.Text(
            text = currentDate,
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 20.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = androidx.compose.ui.graphics.Color(0x99000000),
                    blurRadius = 4f,
                    offset = androidx.compose.ui.geometry.Offset(0f, 2f)
                )
            )
        )

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))

        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = androidx.compose.ui.Modifier.clickable {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.search.weather.WeatherExportedActivity")
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Let Compose safely kill the coroutine when the widget leaves the screen
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    
                    // Log actual network/parsing errors silently
                    e.printStackTrace() 
                }
            }
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_pixel_weather_sun),
                contentDescription = "Weather",
                modifier = androidx.compose.ui.Modifier.size(22.dp)
            )

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(6.dp))

            androidx.compose.material3.Text(
                text = cachedTemp,
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = androidx.compose.ui.graphics.Color(0x99000000),
                        blurRadius = 4f,
                        offset = androidx.compose.ui.geometry.Offset(0f, 2f)
                    )
                )
            )
        }
    }
}

@androidx.compose.runtime.Composable
fun AtAGlanceWidget(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Smartspace()
}
