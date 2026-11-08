package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.example.data.model.BatteryData
import com.example.data.model.ChargingSpeedCategory
import com.example.widget.WattMeterWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class BatteryMonitor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    private val _batteryData = MutableStateFlow(readCurrentBatteryData())
    val batteryData: StateFlow<BatteryData> = _batteryData.asStateFlow()

    private var pollingJob: Job? = null
    private var simulatedPresetWatts: Float? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val updated = readBatteryFromIntent(intent)
            _batteryData.value = updated
            WattMeterWidgetProvider.updateAllWidgets(context, updated)

            if (updated.isCharging && pollingJob?.isActive != true) {
                startPolling()
            } else if (!updated.isCharging && pollingJob?.isActive == true) {
                stopPolling()
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(batteryReceiver, filter)

        if (_batteryData.value.isCharging) {
            startPolling()
        }
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                refresh()
                delay(1500)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setSimulationWattage(watts: Float?) {
        simulatedPresetWatts = watts
        refresh()
    }

    fun refresh(): BatteryData {
        val data = readCurrentBatteryData()
        _batteryData.value = data
        WattMeterWidgetProvider.updateAllWidgets(context, data)
        return data
    }

    fun readCurrentBatteryData(): BatteryData {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return readBatteryFromIntent(intent)
    }

    private fun readBatteryFromIntent(intent: Intent?): BatteryData {
        if (intent == null) return BatteryData()

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val isPlugged = plugged != 0

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val batteryPct = if (scale > 0) ((level / scale.toFloat()) * 100).toInt() else level

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000)
        val voltageV = if (voltageMv > 100) voltageMv / 1000.0f else voltageMv.toFloat()

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)
        val temperatureC = tempTenths / 10.0f

        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Sangat Baik"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Suhu Panas!"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Baterai Rusak"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Tegangan Berlebih"
            BatteryManager.BATTERY_HEALTH_COLD -> "Suhu Dingin"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Gangguan Sistem"
            else -> "Normal"
        }

        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        val plugType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Pengisi Daya AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "Port USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Pengisian Nirkabel (Qi)"
            else -> if (isCharging) "Pengisi Daya Eksternal" else "Menggunakan Baterai"
        }

        val statusDescription = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Sedang Mengisi Daya"
            BatteryManager.BATTERY_STATUS_FULL -> "Baterai Penuh (100%)"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Mengosongkan Baterai"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Tidak Mengisi"
            else -> "Status Baterai Siap"
        }

        // Calculate Current (mA)
        var rawCurrent = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        var currentMa = 0f
        var isSimulated = false

        if (simulatedPresetWatts != null) {
            val targetWatts = simulatedPresetWatts!!
            isSimulated = true
            currentMa = (targetWatts * 1000f) / voltageV.coerceAtLeast(3.7f)
        } else if (isCharging || isPlugged) {
            if (rawCurrent != 0) {
                // If in microamps, divide by 1000
                currentMa = if (abs(rawCurrent) > 10000) {
                    abs(rawCurrent) / 1000.0f
                } else {
                    abs(rawCurrent).toFloat()
                }
            } else {
                // Fallback for emulators/hardware without CURRENT_NOW sensor
                isSimulated = true
                currentMa = when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_AC -> 2200f // ~9.2W
                    BatteryManager.BATTERY_PLUGGED_USB -> 500f  // ~2.1W
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> 1200f // ~5.0W
                    else -> 1800f
                }
            }
        } else {
            // Discharging current
            if (rawCurrent != 0) {
                currentMa = if (abs(rawCurrent) > 10000) abs(rawCurrent) / 1000f else abs(rawCurrent).toFloat()
            }
        }

        val finalWatts = if (isCharging || isPlugged || simulatedPresetWatts != null) {
            (voltageV * currentMa) / 1000.0f
        } else {
            0.0f
        }

        var chargeTimeRemainingMs = -1L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && batteryManager != null) {
            val systemTime = batteryManager.computeChargeTimeRemaining()
            if (systemTime > 0) chargeTimeRemainingMs = systemTime
        }
        if (chargeTimeRemainingMs <= 0 && isCharging && batteryPct < 100 && currentMa > 50f) {
            val remainingMah = (100 - batteryPct) * 4500f / 100f
            val hours = remainingMah / currentMa
            chargeTimeRemainingMs = (hours * 3600 * 1000).toLong()
        }

        val speed = ChargingSpeedCategory.fromWatts(finalWatts, isCharging || isPlugged || simulatedPresetWatts != null)

        return BatteryData(
            isCharging = isCharging || isPlugged || simulatedPresetWatts != null,
            status = status,
            statusDescription = statusDescription,
            plugType = plugType,
            plugTypeId = plugged,
            level = batteryPct,
            voltageMv = voltageMv,
            voltageV = voltageV,
            currentMa = currentMa,
            powerWatts = finalWatts,
            temperatureC = temperatureC,
            health = health,
            technology = technology,
            chargeTimeRemainingMs = chargeTimeRemainingMs,
            isSimulated = isSimulated,
            speedCategory = speed,
            timestamp = System.currentTimeMillis()
        )
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        stopPolling()
    }
}
