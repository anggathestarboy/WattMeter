package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.model.BatteryData
import com.example.data.model.ChargingSpeedCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class WattMeterWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val data = getQuickBatteryData(context)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, data)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH_WIDGET,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED -> {
                val data = getQuickBatteryData(context)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, WattMeterWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, data)
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.ACTION_REFRESH_WIDGET"

        fun updateAllWidgets(context: Context, data: BatteryData? = null) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, WattMeterWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
            if (appWidgetIds.isEmpty()) return

            val batteryData = data ?: getQuickBatteryData(context)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, batteryData)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            data: BatteryData
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_watt_meter)

            // Click root to open app
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Click refresh icon to refresh widget
            val refreshIntent = Intent(context, WattMeterWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            // Battery Level
            views.setTextViewText(R.id.widget_battery_level, "${data.level}%")

            // Watt Value and Status Text
            if (data.isCharging) {
                val wattStr = String.format(Locale.getDefault(), "%.1f W", data.powerWatts)
                views.setTextViewText(R.id.widget_watt_value, wattStr)
                views.setTextColor(R.id.widget_watt_value, data.speedCategory.colorHex.toInt())

                val statusDetail = if (data.isSimulated) {
                    "⚡ ${data.speedCategory.label} (Simulasi)"
                } else {
                    "⚡ ${data.speedCategory.label} • ${data.plugType}"
                }
                views.setTextViewText(R.id.widget_status_text, statusDetail)
            } else {
                views.setTextViewText(R.id.widget_watt_value, "Tidak Mengisi")
                views.setTextColor(R.id.widget_watt_value, 0xFF94A3B8.toInt())
                views.setTextViewText(R.id.widget_status_text, "Menggunakan Baterai (${data.level}%)")
            }

            // Metrics row
            views.setTextViewText(
                R.id.widget_voltage_text,
                String.format(Locale.getDefault(), "%.2f V", data.voltageV)
            )

            val currentText = if (data.isCharging) {
                String.format(Locale.getDefault(), "+%.0f mA", data.currentMa)
            } else if (data.currentMa > 0) {
                String.format(Locale.getDefault(), "-%.0f mA", data.currentMa)
            } else {
                "0 mA"
            }
            views.setTextViewText(R.id.widget_current_text, currentText)

            views.setTextViewText(
                R.id.widget_temp_text,
                String.format(Locale.getDefault(), "%.1f°C", data.temperatureC)
            )

            // Update Time
            val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val timeStr = timeFormatter.format(Date(data.timestamp))
            val chargingNotice = if (data.isCharging) "Daya Masuk Terpantau" else "Ketuk untuk Membuka"
            views.setTextViewText(R.id.widget_update_time, "Diperbarui $timeStr • $chargingNotice")

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getQuickBatteryData(context: Context): BatteryData {
            val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

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

            val rawCurrent = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
            var currentMa = 0f
            var isSimulated = false

            if (isCharging || isPlugged) {
                if (rawCurrent != 0) {
                    currentMa = if (abs(rawCurrent) > 10000) abs(rawCurrent) / 1000f else abs(rawCurrent).toFloat()
                } else {
                    isSimulated = true
                    currentMa = when (plugged) {
                        BatteryManager.BATTERY_PLUGGED_AC -> 2200f
                        BatteryManager.BATTERY_PLUGGED_USB -> 500f
                        BatteryManager.BATTERY_PLUGGED_WIRELESS -> 1200f
                        else -> 1800f
                    }
                }
            } else if (rawCurrent != 0) {
                currentMa = if (abs(rawCurrent) > 10000) abs(rawCurrent) / 1000f else abs(rawCurrent).toFloat()
            }

            val finalWatts = if (isCharging || isPlugged) (voltageV * currentMa) / 1000.0f else 0.0f
            val speed = ChargingSpeedCategory.fromWatts(finalWatts, isCharging || isPlugged)

            val plugType = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "Pengisi Daya AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "Port USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Nirkabel"
                else -> if (isCharging) "Charger Eksternal" else "Baterai"
            }

            return BatteryData(
                isCharging = isCharging || isPlugged,
                status = status,
                plugType = plugType,
                plugTypeId = plugged,
                level = batteryPct,
                voltageMv = voltageMv,
                voltageV = voltageV,
                currentMa = currentMa,
                powerWatts = finalWatts,
                temperatureC = temperatureC,
                isSimulated = isSimulated,
                speedCategory = speed,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
