package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ACMode
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceCardBorderDark
import com.example.ui.theme.SurfaceDark
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ClimateWheel(
    targetTemp: Int,
    ambientTemp: Double,
    isPowerOn: Boolean,
    mode: ACMode,
    onTempChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val minTemp = 16
    val maxTemp = 31
    val startAngle = 135f
    val sweepTotal = 270f

    val normalizedFraction = ((targetTemp - minTemp).toFloat() / (maxTemp - minTemp)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = normalizedFraction,
        animationSpec = tween(durationMillis = 200),
        label = "temp_progress"
    )

    val activeColor = if (isPowerOn) mode.accentColor else Color(0xFF64748B)

    Box(
        modifier = modifier
            .size(280.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer interactive arc track
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isPowerOn) {
                    if (!isPowerOn) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val touch = change.position
                        val angleRad = atan2(touch.y - center.y, touch.x - center.x)
                        var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                        if (angleDeg < 0) angleDeg += 360f

                        // Map angle back to temp
                        // Start angle is 135deg, sweep 270deg -> range 135 to 405 (or 45)
                        var relAngle = (angleDeg - startAngle)
                        if (relAngle < 0) relAngle += 360f
                        if (relAngle <= sweepTotal) {
                            val fraction = (relAngle / sweepTotal).coerceIn(0f, 1f)
                            val computedTemp = (minTemp + fraction * (maxTemp - minTemp)).roundToInt()
                            onTempChange(computedTemp)
                        }
                    }
                }
        ) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2)
            val arcOffset = Offset(strokeWidth, strokeWidth)

            // Background inactive track
            drawArc(
                color = Color(0xFF1E2D48),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active glowing progress track
            if (isPowerOn) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to CyanPrimary,
                        0.5f to mode.accentColor,
                        1.0f to CyanPrimary,
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * animatedProgress,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth + 2f, cap = StrokeCap.Round)
                )

                // Thumb knob at the end of active track
                val currentAngleDeg = startAngle + sweepTotal * animatedProgress
                val currentAngleRad = Math.toRadians(currentAngleDeg.toDouble())
                val radius = (size.width - strokeWidth * 2) / 2f
                val thumbCenter = Offset(
                    x = center.x + radius * cos(currentAngleRad).toFloat(),
                    y = center.y + radius * sin(currentAngleRad).toFloat()
                )

                drawCircle(
                    color = Color.White,
                    radius = strokeWidth * 0.75f,
                    center = thumbCenter
                )
                drawCircle(
                    color = activeColor,
                    radius = strokeWidth * 0.45f,
                    center = thumbCenter
                )
            }
        }

        // Inner display disc
        Surface(
            modifier = Modifier.size(200.dp),
            shape = CircleShape,
            color = SurfaceDark,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, SurfaceCardBorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mode chip or off state
                Text(
                    text = if (isPowerOn) "${mode.iconEmoji} ${mode.displayName.uppercase()}" else "STANDBY / OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPowerOn) activeColor else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Target Temp digits
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isPowerOn) "$targetTemp" else "--",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPowerOn) Color.White else Color(0xFF64748B),
                        lineHeight = 60.sp
                    )
                    Text(
                        text = "°C",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPowerOn) activeColor else Color(0xFF64748B),
                        modifier = Modifier.padding(top = 8.dp, start = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Ambient Room Temp badge
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E2D48),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROOM",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = String.format("%.1f°C", ambientTemp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Quick decrement button
        FilledIconButton(
            onClick = { onTempChange((targetTemp - 1).coerceAtLeast(minTemp)) },
            enabled = isPowerOn && targetTemp > minTemp,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFF1E2D48),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF131F33),
                disabledContentColor = Color(0xFF475569)
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 12.dp)
                .size(44.dp)
                .testTag("temp_decrement_btn")
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease target temperature")
        }

        // Quick increment button
        FilledIconButton(
            onClick = { onTempChange((targetTemp + 1).coerceAtMost(maxTemp)) },
            enabled = isPowerOn && targetTemp < maxTemp,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFF1E2D48),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF131F33),
                disabledContentColor = Color(0xFF475569)
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 12.dp)
                .size(44.dp)
                .testTag("temp_increment_btn")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase target temperature")
        }
    }
}
