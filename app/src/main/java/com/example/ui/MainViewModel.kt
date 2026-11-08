package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.BatteryMonitor
import com.example.data.db.AppDatabase
import com.example.data.db.ChargingSession
import com.example.data.model.BatteryData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val sessionDao = database.chargingSessionDao()
    private val batteryMonitor = BatteryMonitor(application)

    val batteryData: StateFlow<BatteryData> = batteryMonitor.batteryData

    val chargingSessions: StateFlow<List<ChargingSession>> = sessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time watt history for live chart (max 30 points)
    private val _wattHistory = MutableStateFlow<List<Float>>(listOf(0f))
    val wattHistory: StateFlow<List<Float>> = _wattHistory.asStateFlow()

    // Battery protection settings
    private val _targetPercentage = MutableStateFlow(80)
    val targetPercentage: StateFlow<Int> = _targetPercentage.asStateFlow()

    private val _isAlarmEnabled = MutableStateFlow(true)
    val isAlarmEnabled: StateFlow<Boolean> = _isAlarmEnabled.asStateFlow()

    private val _isVibrationEnabled = MutableStateFlow(true)
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    private val _tempWarningLimit = MutableStateFlow(42f)
    val tempWarningLimit: StateFlow<Float> = _tempWarningLimit.asStateFlow()

    // Simulation wattage (null = real device sensors)
    private val _simulatedWatts = MutableStateFlow<Float?>(null)
    val simulatedWatts: StateFlow<Float?> = _simulatedWatts.asStateFlow()

    // Tracking active session
    private var sessionStartTime: Long = 0L
    private var sessionStartLevel: Int = 0
    private var sessionMaxWatts: Float = 0f
    private var sessionWattSum: Float = 0f
    private var sessionSampleCount: Int = 0
    private var hasAlarmTriggeredForCurrentSession = false

    init {
        createNotificationChannel()

        viewModelScope.launch {
            batteryData.collect { data ->
                updateLiveChart(data.powerWatts)
                handleChargingSessionTracking(data)
                checkBatteryProtectionAlerts(data)
            }
        }
    }

    private fun updateLiveChart(watts: Float) {
        val currentList = _wattHistory.value.toMutableList()
        currentList.add(watts)
        if (currentList.size > 30) {
            currentList.removeAt(0)
        }
        _wattHistory.value = currentList
    }

    private fun handleChargingSessionTracking(data: BatteryData) {
        if (data.isCharging) {
            if (sessionStartTime == 0L) {
                // Charging started
                sessionStartTime = System.currentTimeMillis()
                sessionStartLevel = data.level
                sessionMaxWatts = data.powerWatts
                sessionWattSum = data.powerWatts
                sessionSampleCount = 1
                hasAlarmTriggeredForCurrentSession = false
            } else {
                sessionMaxWatts = max(sessionMaxWatts, data.powerWatts)
                sessionWattSum += data.powerWatts
                sessionSampleCount++
            }
        } else {
            if (sessionStartTime != 0L) {
                // Charging finished, save session
                val endTime = System.currentTimeMillis()
                val durationMin = max(1, ((endTime - sessionStartTime) / 60000).toInt())
                val avgWatts = if (sessionSampleCount > 0) sessionWattSum / sessionSampleCount else 0f

                val session = ChargingSession(
                    startTime = sessionStartTime,
                    endTime = endTime,
                    startLevel = sessionStartLevel,
                    endLevel = data.level,
                    maxWatts = sessionMaxWatts,
                    avgWatts = avgWatts,
                    plugType = data.plugType,
                    durationMinutes = durationMin
                )

                viewModelScope.launch {
                    sessionDao.insertSession(session)
                }

                // Reset
                sessionStartTime = 0L
                sessionMaxWatts = 0f
                sessionWattSum = 0f
                sessionSampleCount = 0
                hasAlarmTriggeredForCurrentSession = false
            }
        }
    }

    private fun checkBatteryProtectionAlerts(data: BatteryData) {
        if (!data.isCharging || !_isAlarmEnabled.value) return

        // Check target percentage alert
        if (data.level >= _targetPercentage.value && !hasAlarmTriggeredForCurrentSession) {
            hasAlarmTriggeredForCurrentSession = true
            triggerAlert(
                title = "⚡ Target Baterai Tercapai (${data.level}%)",
                message = "Baterai telah mencapai target ${_targetPercentage.value}%. Cabut charger untuk memperpanjang usia baterai!"
            )
        }

        // Check overheat alert
        if (data.temperatureC >= _tempWarningLimit.value) {
            triggerAlert(
                title = "⚠️ Peringatan Suhu Baterai Panas!",
                message = "Suhu baterai mencapai ${data.temperatureC}°C saat pengisian daya. Istirahatkan perangkat!"
            )
        }
    }

    private fun triggerAlert(title: String, message: String) {
        val context = getApplication<Application>()

        if (_isVibrationEnabled.value) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(400)
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_bolt)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager?.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = getApplication<Application>()
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Peringatan Proteksi Baterai WattMeter",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi batas pengisian daya dan suhu baterai"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun setSimulationWatts(watts: Float?) {
        _simulatedWatts.value = watts
        batteryMonitor.setSimulationWattage(watts)
    }

    fun setTargetPercentage(pct: Int) {
        _targetPercentage.value = pct
        hasAlarmTriggeredForCurrentSession = false
    }

    fun toggleAlarm(enabled: Boolean) {
        _isAlarmEnabled.value = enabled
    }

    fun toggleVibration(enabled: Boolean) {
        _isVibrationEnabled.value = enabled
    }

    fun setTempWarningLimit(temp: Float) {
        _tempWarningLimit.value = temp
    }

    fun refresh() {
        batteryMonitor.refresh()
    }

    fun saveCurrentSessionManually() {
        val data = batteryData.value
        val startTime = if (sessionStartTime > 0) sessionStartTime else System.currentTimeMillis() - 600000
        val duration = max(1, ((System.currentTimeMillis() - startTime) / 60000).toInt())
        val session = ChargingSession(
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            startLevel = max(1, data.level - 12),
            endLevel = data.level,
            maxWatts = if (data.powerWatts > 0) data.powerWatts else 18.5f,
            avgWatts = if (data.powerWatts > 0) data.powerWatts * 0.9f else 15.2f,
            plugType = data.plugType,
            durationMinutes = duration
        )
        viewModelScope.launch {
            sessionDao.insertSession(session)
        }
    }

    fun deleteSession(session: ChargingSession) {
        viewModelScope.launch {
            sessionDao.deleteSession(session)
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            sessionDao.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        batteryMonitor.cleanup()
    }

    companion object {
        private const val CHANNEL_ID = "wattmeter_protection_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
