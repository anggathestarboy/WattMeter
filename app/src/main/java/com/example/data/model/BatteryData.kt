package com.example.data.model

enum class ChargingSpeedCategory(
    val label: String,
    val description: String,
    val minWatts: Float,
    val colorHex: Long
) {
    SUPER_FAST("Super Fast Charging", "Daya sangat tinggi (> 25W)", 25.0f, 0xFF00E5FF),
    FAST("Fast Charging", "Pengisian cepat (15W - 25W)", 15.0f, 0xFF00E676),
    NORMAL("Normal Charging", "Standar AC (5W - 15W)", 5.0f, 0xFFFFAB00),
    SLOW("Slow Charging", "Pengisian lambat / USB (< 5W)", 0.0f, 0xFF94A3B8),
    NOT_CHARGING("Tidak Mengisi", "Perangkat tidak terhubung ke daya", 0.0f, 0xFF64748B);

    companion object {
        fun fromWatts(watts: Float, isCharging: Boolean): ChargingSpeedCategory {
            if (!isCharging || watts <= 0.05f) return NOT_CHARGING
            return when {
                watts >= 25.0f -> SUPER_FAST
                watts >= 15.0f -> FAST
                watts >= 5.0f -> NORMAL
                else -> SLOW
            }
        }
    }
}

data class BatteryData(
    val isCharging: Boolean = false,
    val status: Int = 0,
    val statusDescription: String = "Tidak Mengisi",
    val plugType: String = "Baterai",
    val plugTypeId: Int = 0,
    val level: Int = 50,
    val voltageMv: Int = 4000,
    val voltageV: Float = 4.0f,
    val currentMa: Float = 0f,
    val powerWatts: Float = 0f,
    val temperatureC: Float = 28.0f,
    val health: String = "Baik",
    val technology: String = "Li-ion",
    val chargeTimeRemainingMs: Long = -1L,
    val isSimulated: Boolean = false,
    val speedCategory: ChargingSpeedCategory = ChargingSpeedCategory.NOT_CHARGING,
    val timestamp: Long = System.currentTimeMillis()
)
