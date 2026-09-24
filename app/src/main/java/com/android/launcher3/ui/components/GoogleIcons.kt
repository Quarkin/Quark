package com.android.launcher3.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun GoogleLogoIcon(
    isThemed: Boolean,
    tintColor: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    if (isThemed) {
        Canvas(modifier = modifier) {
            val strokeWidth = size.width * 0.18f
            val radius = (size.width - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer G arc
            drawArc(
                color = tintColor,
                startAngle = -45f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Horizontal crossbar
            drawLine(
                color = tintColor,
                start = center,
                end = Offset(center.x + radius, center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    } else {
        val blue = Color(0xFF4285F4)
        val red = Color(0xFFEA4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)

        Canvas(modifier = modifier) {
            val strokeWidth = size.width * 0.20f
            val rect = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // Red arc (top)
            drawArc(
                color = red,
                startAngle = 180f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = topLeft,
                size = rect,
                style = Stroke(width = strokeWidth)
            )

            // Yellow arc (left bottom)
            drawArc(
                color = yellow,
                startAngle = 110f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = topLeft,
                size = rect,
                style = Stroke(width = strokeWidth)
            )

            // Green arc (bottom)
            drawArc(
                color = green,
                startAngle = 30f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = topLeft,
                size = rect,
                style = Stroke(width = strokeWidth)
            )

            // Blue arc (right) & crossbar
            drawArc(
                color = blue,
                startAngle = -30f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = topLeft,
                size = rect,
                style = Stroke(width = strokeWidth)
            )

            val center = Offset(size.width / 2f, size.height / 2f)
            drawLine(
                color = blue,
                start = Offset(center.x - strokeWidth * 0.2f, center.y),
                end = Offset(size.width - strokeWidth / 2f, center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Square
            )
        }
    }
}

@Composable
fun GoogleLensIcon(
    tintColor: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.12f
        val w = size.width
        val h = size.height

        // Camera lens viewfinder icon
        val path = Path().apply {
            // Top left corner
            moveTo(w * 0.2f, h * 0.45f)
            lineTo(w * 0.2f, h * 0.25f)
            quadraticTo(w * 0.2f, h * 0.2f, w * 0.25f, h * 0.2f)
            lineTo(w * 0.45f, h * 0.2f)

            // Top right corner
            moveTo(w * 0.55f, h * 0.2f)
            lineTo(w * 0.75f, h * 0.2f)
            quadraticTo(w * 0.8f, h * 0.2f, w * 0.8f, h * 0.25f)
            lineTo(w * 0.8f, h * 0.45f)

            // Bottom left corner
            moveTo(w * 0.2f, h * 0.55f)
            lineTo(w * 0.2f, h * 0.75f)
            quadraticTo(w * 0.2f, h * 0.8f, w * 0.25f, h * 0.8f)
            lineTo(w * 0.45f, h * 0.8f)

            // Bottom right corner
            moveTo(w * 0.8f, h * 0.55f)
            lineTo(w * 0.8f, h * 0.75f)
            quadraticTo(w * 0.8f, h * 0.8f, w * 0.75f, h * 0.8f)
            lineTo(w * 0.55f, h * 0.8f)
        }

        drawPath(
            path = path,
            color = tintColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Center lens circle
        drawCircle(
            color = tintColor,
            radius = size.width * 0.18f,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = strokeWidth)
        )
    }
}
