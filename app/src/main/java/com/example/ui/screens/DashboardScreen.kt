package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BatteryData
import com.example.ui.components.HeroWattGauge
import com.example.ui.components.LivePowerChart
import com.example.ui.components.MetricCard
import com.example.ui.components.WidgetGuideSheet
import com.example.ui.components.WidgetPromoCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun DashboardScreen(
    batteryData: BatteryData,
    wattHistory: List<Float>,
    simulatedWatts: Float?,
    onSetSimulationWatts: (Float?) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWidgetGuide by remember { mutableStateOf(false) }
    var showSimulationPanel by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    if (showWidgetGuide) {
        WidgetGuideSheet(
            onDismiss = { showWidgetGuide = false },
            onTestRefreshWidget = { onRefresh() }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("dashboard_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WattMeter",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Text(
                    text = if (batteryData.isCharging) "Menerima Daya Masuk" else "Perangkat Lepas Charger",
                    fontSize = 12.sp,
                    color = if (batteryData.isCharging) NeonGreen else TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Simulation toggle chip button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (simulatedWatts != null) CyanPrimary.copy(alpha = 0.2f) else DarkSurfaceCard)
                        .clickable { showSimulationPanel = !showSimulationPanel }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_toggle_sim_panel")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Uji Simulasi Daya",
                            tint = if (simulatedWatts != null) CyanPrimary else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (simulatedWatts != null) "${simulatedWatts.toInt()}W Test" else "Uji Daya",
                            color = if (simulatedWatts != null) CyanPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Refresh button
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceCard)
                        .testTag("btn_dashboard_refresh")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Segarkan Daya",
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Optional Simulation Picker Row
        if (showSimulationPanel) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceCard)
                    .padding(12.dp)
            ) {
                Text(
                    text = "Mode Uji Simulasi Daya (Untuk Uji Coba Kecepatan):",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SimulationChip(
                        label = "Sensor Asli",
                        isSelected = simulatedWatts == null,
                        onClick = { onSetSimulationWatts(null) }
                    )
                    SimulationChip(
                        label = "2.5W USB",
                        isSelected = simulatedWatts == 2.5f,
                        onClick = { onSetSimulationWatts(2.5f) }
                    )
                    SimulationChip(
                        label = "10W Standar",
                        isSelected = simulatedWatts == 10f,
                        onClick = { onSetSimulationWatts(10f) }
                    )
                    SimulationChip(
                        label = "18W Fast",
                        isSelected = simulatedWatts == 18f,
                        onClick = { onSetSimulationWatts(18f) }
                    )
                    SimulationChip(
                        label = "33W Turbo",
                        isSelected = simulatedWatts == 33f,
                        onClick = { onSetSimulationWatts(33f) }
                    )
                    SimulationChip(
                        label = "67W Super",
                        isSelected = simulatedWatts == 67f,
                        onClick = { onSetSimulationWatts(67f) }
                    )
                    SimulationChip(
                        label = "120W Hyper",
                        isSelected = simulatedWatts == 120f,
                        onClick = { onSetSimulationWatts(120f) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Watt Gauge
        HeroWattGauge(batteryData = batteryData)

        Spacer(modifier = Modifier.height(16.dp))

        // Widget Promo Card
        WidgetPromoCard(
            onOpenGuide = { showWidgetGuide = true },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Live Real-Time Chart
        LivePowerChart(wattHistory = wattHistory)

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry Grid
        Text(
            text = "Telemetri Daya & Baterai",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                icon = Icons.Default.Bolt,
                iconColor = NeonGreen,
                title = "Arus Masuk",
                value = if (batteryData.isCharging) {
                    String.format(Locale.getDefault(), "+%.0f mA", batteryData.currentMa)
                } else if (batteryData.currentMa > 0) {
                    String.format(Locale.getDefault(), "-%.0f mA", batteryData.currentMa)
                } else {
                    "0 mA"
                },
                subValue = if (batteryData.isCharging) "Arus masuk ke baterai" else "Konsumsi daya sistem",
                modifier = Modifier.weight(1f),
                testTag = "metric_current"
            )

            MetricCard(
                icon = Icons.Default.ElectricMeter,
                iconColor = CyanPrimary,
                title = "Tegangan Baterai",
                value = String.format(Locale.getDefault(), "%.2f V", batteryData.voltageV),
                subValue = "${batteryData.voltageMv} mV nominal",
                modifier = Modifier.weight(1f),
                testTag = "metric_voltage"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val tempColor = when {
                batteryData.temperatureC > 40f -> Color(0xFFFF5252)
                batteryData.temperatureC > 35f -> AmberWarning
                else -> NeonGreen
            }

            MetricCard(
                icon = Icons.Default.Thermostat,
                iconColor = tempColor,
                title = "Suhu Baterai",
                value = String.format(Locale.getDefault(), "%.1f °C", batteryData.temperatureC),
                badgeText = if (batteryData.temperatureC > 40f) "PANAS" else "NORMAL",
                badgeColor = tempColor,
                modifier = Modifier.weight(1f),
                testTag = "metric_temperature"
            )

            MetricCard(
                icon = Icons.Default.Power,
                iconColor = CyanPrimary,
                title = "Sumber Daya",
                value = batteryData.plugType,
                subValue = batteryData.statusDescription,
                modifier = Modifier.weight(1f),
                testTag = "metric_plug_type"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val etaStr = if (batteryData.chargeTimeRemainingMs > 0 && batteryData.isCharging) {
                val totalMin = (batteryData.chargeTimeRemainingMs / 60000).toInt()
                val hrs = totalMin / 60
                val mins = totalMin % 60
                if (hrs > 0) "${hrs}j ${mins}m" else "${mins} menit"
            } else if (batteryData.level == 100) {
                "Sudah Penuh"
            } else {
                "Tergantung Beban"
            }

            MetricCard(
                icon = Icons.Default.AccessTime,
                iconColor = AmberWarning,
                title = "Estimasi Penuh",
                value = etaStr,
                subValue = "Ke 100% kapasitas",
                modifier = Modifier.weight(1f),
                testTag = "metric_eta"
            )

            MetricCard(
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFFFF4081),
                title = "Kondisi Baterai",
                value = batteryData.health,
                subValue = batteryData.technology,
                modifier = Modifier.weight(1f),
                testTag = "metric_health"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SimulationChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CyanPrimary else DarkBackground)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF00363D) else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
