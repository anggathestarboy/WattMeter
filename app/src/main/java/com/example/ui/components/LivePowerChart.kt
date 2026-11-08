package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceCardBorder
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun LivePowerChart(
    wattHistory: List<Float>,
    modifier: Modifier = Modifier
) {
    val peakWatt = wattHistory.maxOrNull() ?: 0f
    val currentWatt = wattHistory.lastOrNull() ?: 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .testTag("live_power_chart")
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Grafik Daya Masuk Real-Time",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fluktuasi Watt saat charging",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = CyanPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "Puncak: %.1f W", peakWatt),
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(top = 12.dp)
            ) {
                if (wattHistory.size < 2) {
                    // Empty or single point
                    drawLine(
                        color = DarkSurfaceCardBorder,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 1.dp.toPx()
                    )
                    return@Canvas
                }

                val maxVal = (wattHistory.maxOrNull() ?: 10f).coerceAtLeast(10f) * 1.15f
                val minVal = 0f
                val range = maxVal - minVal

                val stepX = size.width / (wattHistory.size - 1)

                // Grid lines (3 horizontal lines)
                for (i in 1..2) {
                    val y = size.height * (i / 3f)
                    drawLine(
                        color = Color(0x2294A3B8),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val strokePath = Path()
                val fillPath = Path()

                wattHistory.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = size.height - ((value - minVal) / range * size.height).coerceIn(0f, size.height)

                    if (index == 0) {
                        strokePath.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        strokePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }

                    if (index == wattHistory.lastIndex) {
                        fillPath.lineTo(x, size.height)
                        fillPath.close()
                    }
                }

                // Fill gradient under line
                val fillBrush = Brush.verticalGradient(
                    colors = listOf(
                        CyanPrimary.copy(alpha = 0.35f),
                        NeonGreen.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height
                )
                drawPath(path = fillPath, brush = fillBrush)

                // Stroke line
                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(NeonGreen, CyanPrimary)
                    ),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Current point dot
                val lastX = (wattHistory.size - 1) * stepX
                val lastY = size.height - ((currentWatt - minVal) / range * size.height).coerceIn(0f, size.height)
                drawCircle(
                    color = CyanPrimary,
                    radius = 4.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
            }
        }
    }
}
