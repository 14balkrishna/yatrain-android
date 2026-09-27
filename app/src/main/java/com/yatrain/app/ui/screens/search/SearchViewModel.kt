package com.yatrain.app.ui.screens.search

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yatrain.app.YatrainApplication
import io.github.jan.supabase.gotrue.auth
import com.yatrain.app.models.PNRStatus
import com.yatrain.app.models.SavedStationAlert
import com.yatrain.app.models.SavedTicket
import com.yatrain.app.models.Station
import com.yatrain.app.models.Train
import com.yatrain.app.services.SavedDataRepository
import com.yatrain.app.services.StationAlertReceiver
import com.yatrain.app.services.TrainAPIService
import com.yatrain.app.ui.screens.track.LiveTrainEntry
import com.yatrain.app.ui.screens.track.StationInfo
import com.yatrain.app.ui.screens.track.allStationsForSearch
import com.yatrain.app.ui.screens.track.stationSchedule
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Curated popular trains (mirrors iOS list) ─────────────────────────────────

internal val curatedPopularTrains = listOf(
    PopularTrain("12951","Mumbai Rajdhani",       "Mumbai Central","New Delhi",       "17:00","08:35"),
    PopularTrain("12952","Mumbai Rajdhani",       "New Delhi",     "Mumbai Central",  "16:55","08:15"),
    PopularTrain("12259","Sealdah Duronto",       "Sealdah",       "New Delhi",       "20:05","10:00"),
    PopularTrain("12260","Sealdah Duronto",       "New Delhi",     "Sealdah",         "12:30","05:20"),
    PopularTrain("12009","Mumbai Shatabdi",       "Mumbai Central","Ahmedabad",       "06:25","12:55"),
    PopularTrain("12010","Mumbai Shatabdi",       "Ahmedabad",     "Mumbai Central",  "14:30","21:00"),
    PopularTrain("12301","Howrah Rajdhani",       "Howrah",        "New Delhi",       "14:05","10:00"),
    PopularTrain("12302","Howrah Rajdhani",       "New Delhi",     "Howrah",          "17:00","13:05"),
    PopularTrain("22229","Vande Bharat Express",  "New Delhi",     "Chhatarpur",      "06:00","14:00"),
    PopularTrain("22230","Vande Bharat Express",  "Chhatarpur",    "New Delhi",       "15:00","23:00"),
    PopularTrain("12001","Bhopal Shatabdi",       "New Delhi",     "Bhopal",          "06:00","14:00"),
    PopularTrain("12002","Bhopal Shatabdi",       "Bhopal",        "New Delhi",       "15:00","23:00"),
    PopularTrain("12431","Trivandrum Rajdhani",   "New Delhi",     "Trivandrum",      "19:55","04:30"),
    PopularTrain("12432","Trivandrum Rajdhani",   "Trivandrum",    "New Delhi",       "11:55","10:30"),
    PopularTrain("12621","Tamil Nadu SF Express", "Chennai",       "New Delhi",       "22:00","07:40"),
    PopularTrain("12622","Tamil Nadu SF Express", "New Delhi",     "Chennai",         "22:30","08:00"),
    PopularTrain("12627","Karnataka Express",     "Bengaluru",     "New Delhi",       "20:15","11:15"),
    PopularTrain("12628","Karnataka Express",     "New Delhi",     "Bengaluru",       "22:30","14:00"),
    PopularTrain("12025","Pune Shatabdi",         "New Delhi",     "Pune",            "06:15","23:45"),
    PopularTrain("12026","Pune Shatabdi",         "Pune",          "New Delhi",       "00:30","18:05"),
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val api  = TrainAPIService()
    private val repo = SavedDataRepository.getInstance(app)

    private fun userId(): String = try {
        YatrainApplication.supabase.auth.currentUserOrNull()?.id ?: "guest"
    } catch (_: Exception) { "guest" }

    // ── Main search state ─────────────────────────────────────────────────────

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Train>>(emptyList())
    val results: StateFlow<List<Train>> = _results.asStateFlow()

    private val _pnrResult = MutableStateFlow<PNRStatus?>(null)
    val pnrResult: StateFlow<PNRStatus?> = _pnrResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Route detail state ────────────────────────────────────────────────────

    private val _selectedTrain = MutableStateFlow<Train?>(null)
    val selectedTrain: StateFlow<Train?> = _selectedTrain.asStateFlow()

    private val _schedule = MutableStateFlow<List<Station>>(emptyList())
    val schedule: StateFlow<List<Station>> = _schedule.asStateFlow()

    private val _scheduleLoading = MutableStateFlow(false)
    val scheduleLoading: StateFlow<Boolean> = _scheduleLoading.asStateFlow()

    private val _currentStopIndex = MutableStateFlow(0)
    val currentStopIndex: StateFlow<Int> = _currentStopIndex.asStateFlow()

    // ── Coach-finder PNR (separate from main search) ──────────────────────────

    private val _coachPnrInput = MutableStateFlow("")
    val coachPnrInput: StateFlow<String> = _coachPnrInput.asStateFlow()

    private val _coachPnrResult = MutableStateFlow<PNRStatus?>(null)
    val coachPnrResult: StateFlow<PNRStatus?> = _coachPnrResult.asStateFlow()

    private val _coachPnrLoading = MutableStateFlow(false)
    val coachPnrLoading: StateFlow<Boolean> = _coachPnrLoading.asStateFlow()

    // ── Alert state ───────────────────────────────────────────────────────────

    private val _alertedStations = MutableStateFlow<Set<String>>(emptySet())
    val alertedStations: StateFlow<Set<String>> = _alertedStations.asStateFlow()

    private val _alertPickerStation = MutableStateFlow<Station?>(null)
    val alertPickerStation: StateFlow<Station?> = _alertPickerStation.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isTicketSaved = MutableStateFlow(false)
    val isTicketSaved: StateFlow<Boolean> = _isTicketSaved.asStateFlow()

    // ── Popular trains state ──────────────────────────────────────────────────

    private val _popularTrains = MutableStateFlow<List<PopularTrain>>(curatedPopularTrains)
    val popularTrains: StateFlow<List<PopularTrain>> = _popularTrains.asStateFlow()

    private val _popularLoading = MutableStateFlow(false)
    val popularLoading: StateFlow<Boolean> = _popularLoading.asStateFlow()

    // ── Station name search state ─────────────────────────────────────────────

    private val _stationSuggestions = MutableStateFlow<List<StationInfo>>(emptyList())
    val stationSuggestions: StateFlow<List<StationInfo>> = _stationSuggestions.asStateFlow()

    private val _selectedStation = MutableStateFlow<StationInfo?>(null)
    val selectedStation: StateFlow<StationInfo?> = _selectedStation.asStateFlow()

    private val _stationTrains = MutableStateFlow<List<LiveTrainEntry>>(emptyList())
    val stationTrains: StateFlow<List<LiveTrainEntry>> = _stationTrains.asStateFlow()

    private val _stationTrainsLoading = MutableStateFlow(false)
    val stationTrainsLoading: StateFlow<Boolean> = _stationTrainsLoading.asStateFlow()

    private var searchJob: Job? = null

    private fun isPNR(q: String)         = q.length == 10 && q.all { it.isDigit() }
    private fun isTrainNumber(q: String) = q.length in 4..5 && q.all { it.isDigit() }

    // Popular trains use the static curated list only — no auto-refresh on load

    // ── Main search ───────────────────────────────────────────────────────────

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()

        // Clear any active station selection when user edits the query
        if (_selectedStation.value != null) {
            _selectedStation.value = null
            _stationTrains.value = emptyList()
        }

        val trimmed = newQuery.trim()

        if (trimmed.isEmpty()) {
            _results.value = emptyList()
            _pnrResult.value = null
            _error.value = null
            _stationSuggestions.value = emptyList()
            return
        }

        // Station name typeahead: any letter character triggers station search
        if (trimmed.any { it.isLetter() }) {
            val q = trimmed.lowercase()
            // Show local matches immediately so there's no blank flash
            _stationSuggestions.value = allStationsForSearch.filter { s ->
                s.name.lowercase().contains(q) ||
                s.city.lowercase().contains(q) ||
                s.code.lowercase().startsWith(q)
            }.take(8)
            _results.value = emptyList()
            _pnrResult.value = null
            _error.value = null

            // Also fire live autocomplete after 300ms debounce to get full station list
            if (trimmed.length >= 2) {
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    delay(300)
                    api.autocompleteStation(trimmed).onSuccess { list ->
                        if (list.isNotEmpty() && _selectedStation.value == null) {
                            _stationSuggestions.value = list.map { r ->
                                val local = allStationsForSearch.firstOrNull {
                                    it.code.equals(r.code, ignoreCase = true)
                                }
                                StationInfo(r.code, r.name, local?.city ?: "", local?.lat ?: 0.0, local?.lng ?: 0.0)
                            }.take(8)
                        }
                    }
                }
            }
            return
        }

        _stationSuggestions.value = emptyList()
        if (trimmed.length < 4) {
            _results.value = emptyList(); _pnrResult.value = null; _error.value = null
        }
        // No auto-search on keystroke — search fires only on explicit search() call
    }

    fun search() {
        val trimmed = _query.value.trim()
        // Letter-containing query or active station mode — never run train/PNR lookup
        if (trimmed.any { it.isLetter() } || _stationSuggestions.value.isNotEmpty() || _selectedStation.value != null) return
        if (trimmed.length >= 4) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                _isLoading.value = true; _error.value = null
                doSearch(trimmed)
                _isLoading.value = false
            }
        }
    }

    private suspend fun doSearch(trimmed: String) {
        when {
            isPNR(trimmed) -> {
                _results.value = emptyList()
                _isTicketSaved.value = false
                api.getPNRStatus(trimmed)
                    .onSuccess {
                        _pnrResult.value = it
                        _isTicketSaved.value = repo.isTicketSaved(userId(), it.pnrNumber)
                    }
                    .onFailure { _error.value = "PNR not found — check the number and try again." }
            }
            isTrainNumber(trimmed) -> {
                _pnrResult.value = null
                api.searchTrains(trimmed)
                    .onSuccess { trains ->
                        _results.value = trains
                        trains.firstOrNull()?.let { loadScheduleFor(it) }
                    }
                    .onFailure { _error.value = "Train \"$trimmed\" not found." }
            }
            else -> {
                _pnrResult.value = null; _results.value = emptyList()
                _error.value = "Enter a 4–5 digit train number or 10-digit PNR number."
            }
        }
    }

    // ── Coach-finder PNR lookup ───────────────────────────────────────────────

    fun onCoachPnrChange(pnr: String) {
        _coachPnrInput.value = pnr
        if (pnr.isEmpty()) { _coachPnrResult.value = null; return }
        if (pnr.length == 10 && pnr.all { it.isDigit() }) {
            viewModelScope.launch { fetchCoachPnr(pnr) }
        }
    }

    fun searchCoachPnr() {
        val pnr = _coachPnrInput.value
        if (pnr.length == 10 && pnr.all { it.isDigit() }) {
            viewModelScope.launch { fetchCoachPnr(pnr) }
        }
    }

    private suspend fun fetchCoachPnr(pnr: String) {
        _coachPnrLoading.value = true
        api.getPNRStatus(pnr)
            .onSuccess { _coachPnrResult.value = it }
            .onFailure { _coachPnrResult.value = null }
        _coachPnrLoading.value = false
    }

    // ── Station search ────────────────────────────────────────────────────────

    fun onStationSelected(station: StationInfo) {
        searchJob?.cancel()
        _query.value = station.name
        _stationSuggestions.value = emptyList()
        _selectedStation.value = station
        _stationTrains.value = emptyList()
        _pnrResult.value = null
        _results.value = emptyList()
        _error.value = null

        viewModelScope.launch {
            _stationTrainsLoading.value = true
            api.getStationTrains(station.code)
                .onSuccess { trains ->
                    _stationTrains.value = trains.map { t ->
                        val dep = t.departure.replace(".", ":").ifBlank { "Last" }
                        val arr = t.arrival.replace(".", ":").ifBlank { "First" }
                        LiveTrainEntry(
                            number    = t.number,
                            name      = t.name,
                            fromCode  = "",
                            fromName  = t.source,
                            toCode    = "",
                            toName    = t.destination,
                            arrival   = arr,
                            departure = dep,
                            platform  = t.platform.ifBlank { "–" },
                            delayMin  = t.delayMinutes
                        )
                    }
                }
                .onFailure {
                    // Fall back to local static schedule for major stations
                    _stationTrains.value = stationSchedule(station.code)
                }
            _stationTrainsLoading.value = false
        }
    }

    fun clearStationSelection() {
        _selectedStation.value = null
        _stationTrains.value = emptyList()
        _stationSuggestions.value = emptyList()
        _query.value = ""
        _results.value = emptyList()
        _pnrResult.value = null
        _error.value = null
    }

    // ── Route detail ──────────────────────────────────────────────────────────

    fun onTrainCardTapped(train: Train) {
        _selectedTrain.value = train
        viewModelScope.launch { loadScheduleFor(train) }
    }

    private suspend fun loadScheduleFor(train: Train) {
        _scheduleLoading.value = true
        api.getSchedule(train.number)
            .onSuccess { ts ->
                _schedule.value = ts.route
                _currentStopIndex.value = computeCurrentStop(ts.route)
                _selectedTrain.value = train
            }
            .onFailure { _schedule.value = emptyList() }
        _scheduleLoading.value = false
    }

    fun clearSelection() {
        _selectedTrain.value = null
        _schedule.value = emptyList()
        _currentStopIndex.value = 0
        _alertedStations.value = emptySet()
    }

    // ── Alert picker ──────────────────────────────────────────────────────────

    fun showAlertPicker(station: Station)  { _alertPickerStation.value = station }
    fun hideAlertPicker()                  { _alertPickerStation.value = null }

    fun cancelStationAlert(station: Station) {
        _alertedStations.value = _alertedStations.value - station.code
        cancelAlarmAndNotification(station)
        repo.deleteAlertByStation(userId(), station.code)
        _snackbarMessage.value = "Alert cancelled for ${station.name}"
    }

    fun setStationAlert(station: Station, minutesBefore: Int) {
        hideAlertPicker()
        _alertedStations.value = _alertedStations.value + station.code
        val arrivalTime = station.arrival.ifBlank { station.departure }
        scheduleAlarm(station, minutesBefore)
        repo.saveAlert(userId(), SavedStationAlert(
            stationCode   = station.code,
            stationName   = station.name,
            trainNumber   = _selectedTrain.value?.number ?: "",
            trainName     = _selectedTrain.value?.name ?: "",
            eta           = arrivalTime,
            minutesBefore = minutesBefore,
            savedAt       = System.currentTimeMillis()
        ))
        _snackbarMessage.value = "🔔 Alert set — $minutesBefore min before ${station.name}"
    }

    fun clearSnackbar() { _snackbarMessage.value = null }

    fun saveTicket() {
        val pnr = _pnrResult.value ?: return
        val uid = userId()
        if (!repo.isTicketSaved(uid, pnr.pnrNumber)) {
            repo.saveTicket(uid, SavedTicket(
                pnrNumber     = pnr.pnrNumber,
                trainNumber   = pnr.trainNumber,
                trainName     = pnr.trainName,
                from          = pnr.from,
                to            = pnr.to,
                currentStatus = pnr.currentStatus,
                coach         = pnr.coach,
                berth         = pnr.berth
            ))
            _isTicketSaved.value = true
            _snackbarMessage.value = "Ticket saved"
        } else {
            _snackbarMessage.value = "Already saved"
        }
    }

    // ── Alarm scheduling (crash-safe for API 31+) ─────────────────────────────

    private fun scheduleAlarm(station: Station, minutesBefore: Int) {
        val app: Application = getApplication()
        safeCreateChannel(app)
        try {
            RingtoneManager.getRingtone(app, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
        } catch (_: Exception) {}

        val arrivalTime = station.arrival.ifBlank { station.departure }
        val triggerMs   = arrivalTimeToMillis(arrivalTime, minutesBefore)

        if (triggerMs > System.currentTimeMillis()) {
            val intent = Intent(app, StationAlertReceiver::class.java).apply {
                putExtra(StationAlertReceiver.EXTRA_STATION_NAME,   station.name)
                putExtra(StationAlertReceiver.EXTRA_TRAIN_NAME,     _selectedTrain.value?.name ?: "Your train")
                putExtra(StationAlertReceiver.EXTRA_MINUTES_BEFORE, minutesBefore)
                putExtra(StationAlertReceiver.EXTRA_ETA,            arrivalTime)
            }
            val pi = PendingIntent.getBroadcast(
                app, station.code.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                    else -> am.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                }
            } catch (_: SecurityException) { am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi) }
            catch (_: Exception) {}
        }
        postConfirmationNotification(app, station, minutesBefore, arrivalTime)
    }

    private fun cancelAlarmAndNotification(station: Station) {
        val app: Application = getApplication()
        try {
            val pi = PendingIntent.getBroadcast(app, station.code.hashCode(),
                Intent(app, StationAlertReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            (app.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
        } catch (_: Exception) {}
        try { NotificationManagerCompat.from(app).cancel(station.code.hashCode()) } catch (_: Exception) {}
        try { NotificationManagerCompat.from(app).cancel("confirm_${station.code}".hashCode()) } catch (_: Exception) {}
    }

    private fun safeCreateChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(StationAlertReceiver.CHANNEL_ID) == null) {
                    nm.createNotificationChannel(NotificationChannel(
                        StationAlertReceiver.CHANNEL_ID, "Station Arrival Alerts", NotificationManager.IMPORTANCE_HIGH
                    ).apply { enableVibration(true); vibrationPattern = longArrayOf(0, 400, 200, 400) })
                }
            } catch (_: Exception) {}
        }
    }

    private fun postConfirmationNotification(app: Application, station: Station, minutesBefore: Int, eta: String) {
        try {
            val nm = NotificationManagerCompat.from(app)
            if (!nm.areNotificationsEnabled()) return
            nm.notify("confirm_${station.code}".hashCode(),
                NotificationCompat.Builder(app, StationAlertReceiver.CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Arrival Alert 🔔")
                    .setContentText("${station.name} is $minutesBefore min away. Train ${_selectedTrain.value?.number ?: ""} · ETA $eta")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build())
        } catch (_: Exception) {}
    }

    private fun arrivalTimeToMillis(rawTime: String, minutesBefore: Int): Long {
        return try {
            val norm  = rawTime.replace(".", ":").replace(",", ":")
            val parts = norm.split(":").map { it.trim().toInt() }
            val cal   = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, parts[0])
                set(java.util.Calendar.MINUTE, parts.getOrElse(1) { 0 })
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                if (timeInMillis < System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
                add(java.util.Calendar.MINUTE, -minutesBefore)
            }
            cal.timeInMillis
        } catch (_: Exception) { System.currentTimeMillis() + 60_000L }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun computeCurrentStop(stations: List<Station>): Int {
        if (stations.isEmpty()) return 0
        val cal   = java.util.Calendar.getInstance()
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        var best  = 0
        for (i in stations.indices) {
            val dep = parseMinutes(stations[i].departure)
            if (dep == Int.MAX_VALUE) continue
            if (dep <= nowMin) best = i else break
        }
        return best
    }

    private fun parseMinutes(timeStr: String): Int {
        if (timeStr.isBlank() || timeStr == "First" || timeStr == "Last") return Int.MAX_VALUE
        return try {
            val parts = timeStr.replace(".", ":").split(":")
            parts[0].trim().toInt() * 60 + parts[1].trim().toInt()
        } catch (_: Exception) { Int.MAX_VALUE }
    }
}
