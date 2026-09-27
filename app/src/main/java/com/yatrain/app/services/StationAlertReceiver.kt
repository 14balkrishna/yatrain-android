package com.yatrain.app.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yatrain.app.models.SavedStationAlert

class StationAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> rescheduleAllAlerts(context)
            else                         -> fireNotification(context, intent)
        }
    }

    // ── Station arrival notification ──────────────────────────────────────────

    private fun fireNotification(context: Context, intent: Intent) {
        val stationName   = intent.getStringExtra(EXTRA_STATION_NAME) ?: return
        val trainName     = intent.getStringExtra(EXTRA_TRAIN_NAME) ?: "Your train"
        val minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 15)
        val eta           = intent.getStringExtra(EXTRA_ETA) ?: ""

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmUri  = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(CHANNEL_ID, "Station Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
                setSound(alarmUri, audioAttr)
            }
            nm.createNotificationChannel(channel)
        }

        val bodyText = buildString {
            append("$stationName is $minutesBefore minutes away.")
            if (eta.isNotBlank()) append("\n$trainName · ETA $eta")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Arrival Alert 🔔")
            .setContentText("$stationName is $minutesBefore min away")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 400))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setAutoCancel(true)
            .build()

        try {
            nm.notify(stationName.hashCode(), notification)
        } catch (_: SecurityException) {}
    }

    // ── Boot: reschedule all saved alerts that haven't fired yet ──────────────

    private fun rescheduleAllAlerts(context: Context) {
        val prefs = context.getSharedPreferences("yatrain_saved_data", Context.MODE_PRIVATE)
        val gson  = Gson()
        val type  = object : TypeToken<List<SavedStationAlert>>() {}.type
        val now   = System.currentTimeMillis()

        prefs.all.forEach { (key, value) ->
            if (!key.endsWith("_alerts")) return@forEach
            val json = value as? String ?: return@forEach
            val alerts: List<SavedStationAlert> = try {
                gson.fromJson(json, type)
            } catch (_: Exception) { return@forEach }

            alerts.forEach { alert ->
                val triggerMs = arrivalTimeToMillis(alert.eta, alert.minutesBefore)
                if (triggerMs > now) scheduleAlarmIntent(context, alert, triggerMs)
            }
        }
    }

    private fun scheduleAlarmIntent(context: Context, alert: SavedStationAlert, triggerMs: Long) {
        val intent = Intent(context, StationAlertReceiver::class.java).apply {
            putExtra(EXTRA_STATION_NAME,   alert.stationName)
            putExtra(EXTRA_TRAIN_NAME,     alert.trainName.ifBlank { alert.trainNumber })
            putExtra(EXTRA_MINUTES_BEFORE, alert.minutesBefore)
            putExtra(EXTRA_ETA,            alert.eta)
        }
        val pi = PendingIntent.getBroadcast(
            context, alert.stationCode.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                    else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                }
                else -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            }
        } catch (_: SecurityException) { am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi) }
        catch (_: Exception) {}
    }

    private fun arrivalTimeToMillis(rawTime: String, minutesBefore: Int): Long {
        return try {
            val norm  = rawTime.replace(".", ":").replace(",", ":")
            val parts = norm.split(":").map { it.trim().toInt() }
            val cal   = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, parts[0])
                set(java.util.Calendar.MINUTE, parts.getOrElse(1) { 0 })
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (timeInMillis < System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
                add(java.util.Calendar.MINUTE, -minutesBefore)
            }
            cal.timeInMillis
        } catch (_: Exception) { System.currentTimeMillis() + 60_000L }
    }

    companion object {
        const val CHANNEL_ID           = "yatrain_station_alerts"
        const val EXTRA_STATION_NAME   = "station_name"
        const val EXTRA_TRAIN_NAME     = "train_name"
        const val EXTRA_MINUTES_BEFORE = "minutes_before"
        const val EXTRA_ETA            = "eta"
    }
}
