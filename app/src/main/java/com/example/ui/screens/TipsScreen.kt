package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.WidgetGuideSheet
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceCardBorder
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun TipsScreen(
    onTriggerRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWidgetGuide by remember { mutableStateOf(false) }

    if (showWidgetGuide) {
        WidgetGuideSheet(
            onDismiss = { showWidgetGuide = false },
            onTestRefreshWidget = onTriggerRefresh
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("tips_screen")
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CyanPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Panduan & Tips Daya",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Pelajari cara kerja watt charger & widget hemat baterai",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Widget Action Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceCard)
                .border(1.dp, CyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fitur Widget Layar Utama",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aplikasi dirancang sangat hemat baterai: widget hanya aktif saat ada perubahan status charger melalui broadcast sistem Android tanpa background service yang boros baterai.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showWidgetGuide = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_open_widget_guide_tips")
                ) {
                    Text(
                        text = "Lihat Cara Pasang Widget",
                        color = Color(0xFF00363D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Formula Card: Watt = Volt x Ampere
        TipInfoCard(
            icon = Icons.Default.Calculate,
            iconColor = NeonGreen,
            title = "Rumus Daya Masuk (Watt)",
            content = "Daya (Watt) = Tegangan (Volt) × Arus (Ampere).\n\n" +
                    "Contoh: Jika charger memberikan tegangan baterai 4.20V dan arus 2000mA (2 Ampere), maka daya masuk aktual ke baterai adalah:\n" +
                    "4.20 V × 2.0 A = 8.40 Watt."
        )

        Spacer(modifier = Modifier.height(12.dp))

        TipInfoCard(
            icon = Icons.Default.Cable,
            iconColor = CyanPrimary,
            title = "Kenapa Watt Charger Saya Turun / Rendah?",
            content = "1. Level Baterai: Saat baterai di atas 70-80%, sistem secara otomatis menurunkan arus untuk mencegah panas berlebih (Trickle Charging).\n\n" +
                    "2. Kualitas Kabel USB: Kabel yang panjang atau murah memiliki hambatan tinggi yang menurunkan arus masuk.\n\n" +
                    "3. Suhu HP: Jika suhu baterai di atas 38°C, ponsel membatasi daya pengisian untuk melindungi komponen."
        )

        Spacer(modifier = Modifier.height(12.dp))

        TipInfoCard(
            icon = Icons.Default.Thermostat,
            iconColor = Color(0xFFFFAB00),
            title = "Tips Pengisian Hemat Baterai & Awet",
            content = "• Hindari menggunakan smartphone untuk bermain game berat saat sedang dicharge.\n" +
                    "• Pasang batas alarm pada 80% agar jumlah siklus baterai bertahan lebih lama.\n" +
                    "• Lepaskan casing tebal saat Fast Charging agar panas cepat terbuang."
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TipInfoCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    content: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkSurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}
