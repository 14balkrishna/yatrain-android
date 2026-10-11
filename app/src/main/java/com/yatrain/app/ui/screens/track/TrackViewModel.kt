package com.yatrain.app.ui.screens.track

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
import com.yatrain.app.models.SavedTrain
import com.yatrain.app.models.Station
import com.yatrain.app.models.Train
import com.yatrain.app.ui.screens.track.allStationsForSearch
import com.yatrain.app.ui.screens.track.TrainRef
import com.yatrain.app.ui.screens.track.searchTrainsByName
import kotlinx.coroutines.withTimeoutOrNull
import com.yatrain.app.services.SavedDataRepository
import com.yatrain.app.services.StationAlertReceiver
import com.yatrain.app.services.TrainAPIService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TrackViewModel(app: Application) : AndroidViewModel(app) {

    private val api  = TrainAPIService()
    private val repo = SavedDataRepository.getInstance(app)

    private fun userId(): String = try {
        YatrainApplication.supabase.auth.currentUserOrNull()?.id ?: "guest"
    } catch (_: Exception) { "guest" }

    // ── Search state ──────────────────────────────────────────────────────────

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _trainResult = MutableStateFlow<Train?>(null)
    val trainResult: StateFlow<Train?> = _trainResult.asStateFlow()

    private val _schedule = MutableStateFlow<List<Station>>(emptyList())
    val schedule: StateFlow<List<Station>> = _schedule.asStateFlow()

    // Index of the station the train is currently at / most recently departed from
    private val _currentStopIndex = MutableStateFlow(0)
    val currentStopIndex: StateFlow<Int> = _currentStopIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Location / nearby trains state ────────────────────────────────────────

    private val _detectedStation = MutableStateFlow<StationInfo?>(null)
    val detectedStation: StateFlow<StationInfo?> = _detectedStation.asStateFlow()

    private val _liveTrains = MutableStateFlow<List<LiveTrainEntry>>(emptyList())
    val liveTrains: StateFlow<List<LiveTrainEntry>> = _liveTrains.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _showingPopularTrains = MutableStateFlow(false)
    val showingPopularTrains: StateFlow<Boolean> = _showingPopularTrains.asStateFlow()

    private val _stationSuggestions = MutableStateFlow<List<StationInfo>>(emptyList())
    val stationSuggestions: StateFlow<List<StationInfo>> = _stationSuggestions.asStateFlow()

    private val _trainSuggestions = MutableStateFlow<List<TrainRef>>(emptyList())
    val trainSuggestions: StateFlow<List<TrainRef>> = _trainSuggestions.asStateFlow()

    // ── Alert state ───────────────────────────────────────────────────────────

    private val _alertedStations = MutableStateFlow<Set<String>>(emptySet())
    val alertedStations: StateFlow<Set<String>> = _alertedStations.asStateFlow()

    private val _mapStation = MutableStateFlow<Station?>(null)
    val mapStation: StateFlow<Station?> = _mapStation.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isTrainSaved = MutableStateFlow(false)
    val isTrainSaved: StateFlow<Boolean> = _isTrainSaved.asStateFlow()

    private val _pnrContext = MutableStateFlow<PNRStatus?>(null)
    val pnrContext: StateFlow<PNRStatus?> = _pnrContext.asStateFlow()

    fun setPNRContext(pnr: PNRStatus?) { _pnrContext.value = pnr }

    private var searchJob: Job? = null

    private fun isTrainNumber(q: String) = q.length in 4..5 && q.all { it.isDigit() }

    // ── Location-based train loading ──────────────────────────────────────────

    /** Loads the curated popular-trains list into the live train table. */
    fun loadPopularTrains(entries: List<LiveTrainEntry>) {
        clearResult()
        _detectedStation.value = null
        _isLocating.value = false
        _showingPopularTrains.value = true
        _liveTrains.value = entries.sortedBy { sortKey(it) }
    }

    /** Called from the screen once location permission is confirmed and a fix is available. */
    fun onLocationResult(lat: Double, lng: Double) {
        val station = nearestStation(lat, lng) ?: return
        _showingPopularTrains.value = false
        _isLocating.value = true       // set before detectedStation so the spinner never flashes "no trains"
        _detectedStation.value = station
        _liveTrains.value = emptyList()
        _error.value = null
        viewModelScope.launch {
            withTimeoutOrNull(15_000L) {
                api.getStationTrains(station.code)
                    .onSuccess { trains -> _liveTrains.value = trains.map { mapApiTrain(it) } }
                    .onFailure { e -> _error.value = e.message ?: "Could not load trains. Check your connection." }
            }
            _isLocating.value = false
        }
    }

    /** Call before tryGetLocation() so the spinner appears immediately.
     *  Auto-cancels the spinner after 20 s if no location fix arrives (Samsung / no-GPS). */
    fun startLocating() {
        _isLocating.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(20_000L)
            // Only clear the spinner if still locating with no station found yet
            if (_isLocating.value && _detectedStation.value == null) {
                _isLocating.value = false
            }
        }
    }

    /** Called when the platform cannot provide a location fix (providers disabled / permission denied). */
    fun stopLocating() { _isLocating.value = false }

    /** Called by the "Live Trains" button — fetches fresh data from the API. */
    fun refreshLiveTrains() {
        val station = _detectedStation.value ?: return
        _error.value = null
        viewModelScope.launch {
            _isLocating.value = true
            withTimeoutOrNull(15_000L) {
                api.getStationTrains(station.code)
                    .onSuccess { trains ->
                        if (trains.isNotEmpty()) _liveTrains.value = trains.map { mapApiTrain(it) }
                    }
                    .onFailure { e -> _error.value = e.message ?: "Could not load trains. Check your connection." }
            }
            _isLocating.value = false
        }
    }

    private fun mapApiTrain(t: com.yatrain.app.models.StationTrain): LiveTrainEntry {
        val dep = t.departure.orEmpty().replace(".", ":").ifBlank { "Last" }
        val arr = t.arrival.orEmpty().replace(".", ":").ifBlank { "First" }
        return LiveTrainEntry(
            number   = t.number.orEmpty(),  name     = t.name.orEmpty(),
            fromCode = "",                  fromName = t.source.orEmpty(),
            toCode   = "",                  toName   = t.destination.orEmpty(),
            arrival  = arr,                 departure = dep,
            platform = t.platform.orEmpty().ifBlank { "–" },
            delayMin = t.delayMinutes
        )
    }

    /**
     * Sort key: minutes since midnight.
     * Uses departure time for most trains; arrival time for terminators ("Last" departure);
     * trains that originate here ("First" arrival) sort by their departure.
     * Unresolvable times sort to the end.
     */
    private fun sortKey(entry: LiveTrainEntry): Int {
        val timeStr = when {
            entry.departure == "Last" -> entry.arrival
            else -> entry.departure
        }
        return parseMinutes(timeStr)
    }

    private fun parseMinutes(timeStr: String): Int {
        if (timeStr == "First" || timeStr == "Last" || timeStr.isBlank()) return Int.MAX_VALUE
        return try {
            val parts = timeStr.replace(".", ":").split(":")
            parts[0].trim().toInt() * 60 + parts[1].trim().toInt()
        } catch (_: Exception) { Int.MAX_VALUE }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        val trimmed = newQuery.trim()

        if (trimmed.any { it.isLetter() }) {
            val q = trimmed.lowercase()
            if (trimmed.length >= 2) {
                _stationSuggestions.value = allStationsForSearch.filter { s ->
                    s.name.lowercase().contains(q) ||
                    s.city.lowercase().contains(q) ||
                    s.code.lowercase().startsWith(q)
                }.take(5)
                _trainSuggestions.value = searchTrainsByName(trimmed)
            } else {
                _stationSuggestions.value = emptyList()
                _trainSuggestions.value = emptyList()
            }
            _trainResult.value = null; _schedule.value = emptyList(); _error.value = null
            return
        }

        _stationSuggestions.value = emptyList()
        _trainSuggestions.value = emptyList()
        if (trimmed.length < 4) {
            _trainResult.value = null; _schedule.value = emptyList(); _error.value = null
        }
    }

    fun onStationSelected(station: StationInfo) {
        searchJob?.cancel()
        _query.value = station.name
        _stationSuggestions.value = emptyList()
        _trainResult.value = null; _schedule.value = emptyList(); _error.value = null
        _showingPopularTrains.value = false
        _isLocating.value = true
        _detectedStation.value = station
        _liveTrains.value = emptyList()
        viewModelScope.launch {
            withTimeoutOrNull(15_000L) {
                api.getStationTrains(station.code)
                    .onSuccess { trains ->
                        if (trains.isNotEmpty()) _liveTrains.value = trains.map { mapApiTrain(it) }
                    }
                    .onFailure { e -> _error.value = e.message ?: "Could not load trains for ${station.name}." }
            }
            _isLocating.value = false
        }
    }

    fun onTrainSuggestionSelected(train: TrainRef) {
        searchJob?.cancel()
        _query.value = train.number
        _stationSuggestions.value = emptyList()
        _trainSuggestions.value = emptyList()
        _trainResult.value = null; _schedule.value = emptyList(); _error.value = null
        searchJob = viewModelScope.launch { fetch(train.number) }
    }

    fun search() {
        val trimmed = _query.value.trim()
        if (trimmed.length >= 4) { searchJob?.cancel(); searchJob = viewModelScope.launch { fetch(trimmed) } }
    }

    /** Called when the user taps a row in the location-based live trains list. */
    fun onLiveTrainTapped(entry: LiveTrainEntry) {
        _query.value = entry.number
        searchJob?.cancel()
        searchJob = viewModelScope.launch { fetch(entry.number) }
    }

    private suspend fun fetch(trainNumber: String) {
        if (!isTrainNumber(trainNumber)) {
            _error.value = "Enter a 4–5 digit train number to track."; return
        }
        _isLoading.value = true; _error.value = null; _trainResult.value = null; _schedule.value = emptyList()

        api.searchTrains(trainNumber)
            .onSuccess { _trainResult.value = it.firstOrNull() }
            .onFailure { _error.value = "Train \"$trainNumber\" not found." }

        if (_trainResult.value != null) {
            api.getSchedule(trainNumber)
                .onSuccess {
                    _schedule.value = it.route
                    _currentStopIndex.value = computeCurrentStop(it.route)
                }
            _isTrainSaved.value = repo.isTrainSaved(userId(), trainNumber)
        }
        _isLoading.value = false
    }

    fun clearResult() {
        _query.value = ""; _trainResult.value = null; _schedule.value = emptyList()
        _error.value = null; _alertedStations.value = emptySet(); _currentStopIndex.value = 0
        _isTrainSaved.value = false; _pnrContext.value = null
        _stationSuggestions.value = emptyList(); _trainSuggestions.value = emptyList()
    }

    fun toggleSaveTrain() {
        val train = _trainResult.value ?: return
        val uid = userId()
        if (repo.isTrainSaved(uid, train.number)) {
            val record = repo.getTrains(uid).firstOrNull { it.trainNumber == train.number }
            if (record != null) repo.deleteTrain(uid, record.id)
            _isTrainSaved.value = false
            _snackbarMessage.value = "Train removed from saved"
        } else {
            repo.saveTrain(uid, SavedTrain(trainNumber = train.number, trainName = train.name, from = train.source, to = train.destination))
            _isTrainSaved.value = true
            _snackbarMessage.value = "Train saved to your watchlist"
        }
    }

    /** Finds the index of the station whose scheduled departure is the most recent one before now. */
    private fun computeCurrentStop(stations: List<Station>): Int {
        if (stations.isEmpty()) return 0
        val cal = java.util.Calendar.getInstance()
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        var best = 0
        for (i in stations.indices) {
            val dep = parseMinutes(stations[i].departure)
            if (dep == Int.MAX_VALUE) continue
            if (dep <= nowMin) best = i else break
        }
        return best
    }

    // ── Alert picker (bottom sheet) ───────────────────────────────────────────

    // Station for which the arrival-alert picker sheet is currently open
    private val _alertPickerStation = MutableStateFlow<Station?>(null)
    val alertPickerStation: StateFlow<Station?> = _alertPickerStation.asStateFlow()

    /** Bell tapped on a station that is NOT yet alerted → open the picker sheet. */
    fun showAlertPicker(station: Station) { _alertPickerStation.value = station }

    /** Close the picker without scheduling. */
    fun hideAlertPicker() { _alertPickerStation.value = null }

    /** Called when the user taps the already-lit bell → cancel the alarm. */
    fun cancelStationAlert(station: Station) {
        _alertedStations.value = _alertedStations.value - station.code
        cancelAlarmAndNotification(station)
        repo.deleteAlertByStation(userId(), station.code)
        _snackbarMessage.value = "Alert cancelled for ${station.name}"
    }

    /** Called from the picker sheet when user confirms a time. */
    fun setStationAlert(station: Station, minutesBefore: Int) {
        hideAlertPicker()
        _alertedStations.value = _alertedStations.value + station.code
        val arrivalTime = station.arrival.ifBlank { station.departure }
        scheduleAlarm(station, minutesBefore)
        repo.saveAlert(userId(), SavedStationAlert(
            stationCode   = station.code,
            stationName   = station.name,
            trainNumber   = _trainResult.value?.number ?: "",
            trainName     = _trainResult.value?.name ?: "",
            eta           = arrivalTime,
            minutesBefore = minutesBefore,
            savedAt       = System.currentTimeMillis()
        ))
        _snackbarMessage.value = "🔔 Alert set — $minutesBefore min before ${station.name}"
    }

    // ── Station Alerts — implementation ──────────────────────────────────────

    private fun scheduleAlarm(station: Station, minutesBefore: Int) {
        val app: Application = getApplication()

        // 1. Ensure notification channel exists
        safeCreateChannel(app)

        // 2. Play a soft confirmation sound
        try {
            RingtoneManager.getRingtone(
                app, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )?.play()
        } catch (_: Exception) {}

        // 3. Schedule the alarm (crash-safe for API 31+ SCHEDULE_EXACT_ALARM)
        val arrivalTime = station.arrival.ifBlank { station.departure }
        val triggerMs   = arrivalTimeToMillis(arrivalTime, minutesBefore)

        if (triggerMs > System.currentTimeMillis()) {
            val intent = Intent(app, StationAlertReceiver::class.java).apply {
                putExtra(StationAlertReceiver.EXTRA_STATION_NAME,  station.name)
                putExtra(StationAlertReceiver.EXTRA_TRAIN_NAME,    _trainResult.value?.name ?: "Your train")
                putExtra(StationAlertReceiver.EXTRA_MINUTES_BEFORE, minutesBefore)
                putExtra(StationAlertReceiver.EXTRA_ETA,           arrivalTime)
            }
            val pi = PendingIntent.getBroadcast(
                app, station.code.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        // API 31+: check canScheduleExactAlarms before calling setExact*
                        if (am.canScheduleExactAlarms()) {
                            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                        } else {
                            // Fall back to inexact but still wake-lock protected
                            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                        }
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                    }
                    else -> am.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                }
            } catch (_: SecurityException) {
                // Last-resort fallback — no permission granted at all
                am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            } catch (_: Exception) {}
        }

        // 4. Post an immediate "alert set" notification (best-effort — silently skipped if permission denied)
        postConfirmationNotification(app, station, minutesBefore, arrivalTime)
    }

    private fun cancelAlarmAndNotification(station: Station) {
        val app: Application = getApplication()
        try {
            val intent = Intent(app, StationAlertReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                app, station.code.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
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
                    val ch = NotificationChannel(
                        StationAlertReceiver.CHANNEL_ID,
                        "Station Arrival Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 400, 200, 400)
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }
                    nm.createNotificationChannel(ch)
                }
            } catch (_: Exception) {}
        }
    }

    private fun postConfirmationNotification(
        app: Application, station: Station, minutesBefore: Int, eta: String
    ) {
        try {
            val nm = NotificationManagerCompat.from(app)
            if (!nm.areNotificationsEnabled()) return
            val notification = NotificationCompat.Builder(app, StationAlertReceiver.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Arrival Alert 🔔")
                .setContentText("${station.name} is $minutesBefore minutes away. Train ${_trainResult.value?.number ?: ""} · ETA $eta")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("${station.name} is $minutesBefore minutes away.\nTrain ${_trainResult.value?.number ?: ""} · ETA $eta"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            nm.notify("confirm_${station.code}".hashCode(), notification)
        } catch (_: Exception) {}
    }

    /** Parse "HH:MM" arrival time, subtract minutesBefore, return epoch millis. */
    private fun arrivalTimeToMillis(rawTime: String, minutesBefore: Int): Long {
        return try {
            val norm  = rawTime.replace(".", ":").replace(",", ":")
            val parts = norm.split(":").map { it.trim().toInt() }
            val cal   = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, parts[0])
                set(java.util.Calendar.MINUTE, parts.getOrElse(1) { 0 })
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                // If that time already passed today, aim for tomorrow
                if (timeInMillis < System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
                add(java.util.Calendar.MINUTE, -minutesBefore)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            System.currentTimeMillis() + 60_000L
        }
    }

    // ── Platform Map ──────────────────────────────────────────────────────────

    fun showMap(station: Station) { _mapStation.value = station }
    fun hideMap() { _mapStation.value = null }

    // ── Snackbar ──────────────────────────────────────────────────────────────

    fun clearSnackbar() { _snackbarMessage.value = null }
}
