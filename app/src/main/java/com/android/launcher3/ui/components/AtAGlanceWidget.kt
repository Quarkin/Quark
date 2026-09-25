package com.android.launcher3.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.android.launcher3.R
import kotlinx.coroutines.launch

@Composable
fun AtAGlanceWidget(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var currentTemp by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("32°C") }

    val currentDate = java.time.LocalDate.now().format(
        java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d")
    )

    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
            .clickable {
                // 1. Fetch live temperature asynchronously on click
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val url = java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=20.296&longitude=85.824&current=temperature_2m")
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 3000
                        val response = connection.inputStream.bufferedReader().readText()
                        
                        val rawVal = response.substringAfter("\"temperature_2m\":").substringBefore("}").substringBefore(",").trim()
                        val rounded = kotlin.math.round(rawVal.toFloat()).toInt()
                        currentTemp = "${rounded}°C"
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 2. Open Google Weather
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.search.weather.WeatherExportedActivity")
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    ) {
        // Live Date
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

        // Vector Sun Icon
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_pixel_weather_sun),
            contentDescription = "Weather",
            modifier = androidx.compose.ui.Modifier.size(22.dp)
        )

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(6.dp))

        // Live Temperature Text
        androidx.compose.material3.Text(
            text = currentTemp,
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
