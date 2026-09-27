package com.yatrain.app.services

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yatrain.app.models.SavedStationAlert
import com.yatrain.app.models.SavedTicket
import com.yatrain.app.models.SavedTrain
import java.util.UUID

class SavedDataRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("yatrain_saved_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        @Volatile private var instance: SavedDataRepository? = null
        fun getInstance(context: Context): SavedDataRepository =
            instance ?: synchronized(this) {
                instance ?: SavedDataRepository(context).also { instance = it }
            }
    }

    // ── Trains ───────────────────────────────────────────────────────────────

    private fun trainKey(userId: String) = "${userId}_trains"

    fun getTrains(userId: String): List<SavedTrain> {
        val json = prefs.getString(trainKey(userId), null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<SavedTrain>>() {}.type)
        } catch (_: Exception) { emptyList() }
    }

    fun saveTrain(userId: String, train: SavedTrain): SavedTrain {
        val list = getTrains(userId).toMutableList()
        val existing = list.firstOrNull { it.trainNumber == train.trainNumber }
        if (existing != null) return existing
        val record = train.copy(id = UUID.randomUUID().toString(), userId = userId, savedAt = System.currentTimeMillis())
        list.add(0, record)
        prefs.edit().putString(trainKey(userId), gson.toJson(list)).apply()
        return record
    }

    fun deleteTrain(userId: String, id: String) {
        val list = getTrains(userId).filter { it.id != id }
        prefs.edit().putString(trainKey(userId), gson.toJson(list)).apply()
    }

    fun isTrainSaved(userId: String, trainNumber: String): Boolean =
        getTrains(userId).any { it.trainNumber == trainNumber }

    // ── Tickets ──────────────────────────────────────────────────────────────

    private fun ticketKey(userId: String) = "${userId}_tickets"

    fun getTickets(userId: String): List<SavedTicket> {
        val json = prefs.getString(ticketKey(userId), null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<SavedTicket>>() {}.type)
        } catch (_: Exception) { emptyList() }
    }

    fun saveTicket(userId: String, ticket: SavedTicket) {
        val list = getTickets(userId).toMutableList()
        if (list.none { it.pnrNumber == ticket.pnrNumber }) {
            val record = ticket.copy(id = UUID.randomUUID().toString(), savedAt = System.currentTimeMillis())
            list.add(0, record)
            prefs.edit().putString(ticketKey(userId), gson.toJson(list)).apply()
        }
    }

    fun deleteTicket(userId: String, id: String) {
        val list = getTickets(userId).filter { it.id != id }
        prefs.edit().putString(ticketKey(userId), gson.toJson(list)).apply()
    }

    fun isTicketSaved(userId: String, pnrNumber: String): Boolean =
        getTickets(userId).any { it.pnrNumber == pnrNumber }

    // ── Alerts ───────────────────────────────────────────────────────────────

    private fun alertKey(userId: String) = "${userId}_alerts"

    fun getAlerts(userId: String): List<SavedStationAlert> {
        val json = prefs.getString(alertKey(userId), null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<SavedStationAlert>>() {}.type)
        } catch (_: Exception) { emptyList() }
    }

    fun saveAlert(userId: String, alert: SavedStationAlert) {
        val list = getAlerts(userId).toMutableList()
        val idx = list.indexOfFirst { it.stationCode == alert.stationCode && it.trainNumber == alert.trainNumber }
        val record = alert.copy(id = if (alert.id.isBlank()) UUID.randomUUID().toString() else alert.id)
        if (idx >= 0) list[idx] = record else list.add(0, record)
        prefs.edit().putString(alertKey(userId), gson.toJson(list)).apply()
    }

    fun deleteAlert(userId: String, id: String) {
        val list = getAlerts(userId).filter { it.id != id }
        prefs.edit().putString(alertKey(userId), gson.toJson(list)).apply()
    }

    fun deleteAlertByStation(userId: String, stationCode: String) {
        val list = getAlerts(userId).filter { it.stationCode != stationCode }
        prefs.edit().putString(alertKey(userId), gson.toJson(list)).apply()
    }

    // ── Clear all data for a user ─────────────────────────────────────────────

    fun clearAll(userId: String) {
        prefs.edit()
            .remove(trainKey(userId))
            .remove(ticketKey(userId))
            .remove(alertKey(userId))
            .apply()
    }
}
