package com.yatrain.app.ui.screens.alerts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.yatrain.app.YatrainApplication
import io.github.jan.supabase.gotrue.auth
import com.yatrain.app.models.SavedStationAlert
import com.yatrain.app.models.SavedTicket
import com.yatrain.app.models.SavedTrain
import com.yatrain.app.services.SavedDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SavedDataViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SavedDataRepository.getInstance(app)

    private val _trains  = MutableStateFlow<List<SavedTrain>>(emptyList())
    val trains: StateFlow<List<SavedTrain>> = _trains.asStateFlow()

    private val _tickets = MutableStateFlow<List<SavedTicket>>(emptyList())
    val tickets: StateFlow<List<SavedTicket>> = _tickets.asStateFlow()

    private val _alerts  = MutableStateFlow<List<SavedStationAlert>>(emptyList())
    val alerts: StateFlow<List<SavedStationAlert>> = _alerts.asStateFlow()

    private fun userId(): String = try {
        YatrainApplication.supabase.auth.currentUserOrNull()?.id ?: "guest"
    } catch (_: Exception) { "guest" }

    fun reload() {
        val uid = userId()
        _trains.value  = repo.getTrains(uid)
        _tickets.value = repo.getTickets(uid)
        _alerts.value  = repo.getAlerts(uid)
    }

    fun deleteTrain(id: String) {
        val uid = userId()
        repo.deleteTrain(uid, id)
        _trains.value = repo.getTrains(uid)
    }

    fun deleteTicket(id: String) {
        val uid = userId()
        repo.deleteTicket(uid, id)
        _tickets.value = repo.getTickets(uid)
    }

    fun deleteAlert(id: String) {
        val uid = userId()
        repo.deleteAlert(uid, id)
        _alerts.value = repo.getAlerts(uid)
    }
}
