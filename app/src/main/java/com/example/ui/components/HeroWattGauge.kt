package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BatteryData
import com.example.data.model.ChargingSpeedCategory
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HeroWattGauge(
    batteryData: BatteryData,
    modifier: Modifier = Modifier
) {
    val maxScaleWatts = if (batteryData.powerWatts > 65f) 120f else 65f
    val animatedWatts by animateFloatAsState(
        targetValue = batteryData.powerWatts,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "watts_anim"
    )

    val progress = (animatedWatts / maxScaleWatts).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(280.dp)
            .testTag("hero_watt_gauge"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val arcSize = size.minDimension - strokeWidth * 2
            val topLeft = Offset((size.width - arcSize) / 2, (size.height - arcSize) / 2)

            // Start angle: 140 degrees (bottom left), sweep 260 degrees (to bottom right)
            val startAngle = 140f
            val totalSweep = 260f

            // Background track
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active neon gradient arc
            if (progress > 0.005f) {
                val activeSweep = totalSweep * progress
                val gradientBrush = Brush.sweepGradient(
                    0.0f to Color(0xFF00E5FF),
                    0.4f to Color(0xFF00E676),
                    0.8f to Color(0xFFFFAB00),
                    1.0f to Color(0xFFFF5252),
                    center = center
                )

                drawArc(
                    brush = gradientBrush,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Draw tick marks around the gauge
            val radius = arcSize / 2f
            val centerPoint = center
            val tickValues = listOf(0, 15, 30, 45, maxScaleWatts.toInt())
            for (tick in tickValues) {
                val tickFraction = tick / maxScaleWatts
                val tickAngleDeg = startAngle + (totalSweep * tickFraction)
                val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())

                val innerR = radius - strokeWidth * 0.8f
                val outerR = radius + strokeWidth * 0.8f

                val startX = centerPoint.x + (innerR * cos(tickAngleRad)).toFloat()
                val startY = centerPoint.y + (innerR * sin(tickAngleRad)).toFloat()
                val endX = centerPoint.x + (outerR * cos(tickAngleRad)).toFloat()
                val endY = centerPoint.y + (outerR * sin(tickAngleRad)).toFloat()

                drawLine(
                    color = Color(0x6694A3B8),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            if (batteryData.isCharging) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", animatedWatts),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(batteryData.speedCategory.colorHex)
                    )
                    Text(
                        text = "W",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(batteryData.speedCategory.colorHex),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }

                // Speed Category Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(batteryData.speedCategory.colorHex).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚡ " + batteryData.speedCategory.label,
                        color = Color(batteryData.speedCategory.colorHex),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "+%.0f mA • %.2f V",
                        batteryData.currentMa,
                        batteryData.voltageV
                    ),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "0.0",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMuted
                )
                Text(
                    text = "Tidak Mengisi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sambungkan Charger",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
    }
}
