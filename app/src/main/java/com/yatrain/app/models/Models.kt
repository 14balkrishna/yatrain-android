package com.yatrain.app.models

import com.google.gson.annotations.SerializedName

// ── Subscription ──────────────────────────────────────────────────────────────

enum class SubscriptionTier { FREE, PRO }

// ── User ──────────────────────────────────────────────────────────────────────

data class YatrainUser(
    val id: String,
    val email: String?,
    val name: String?
)

// ── Train (matches GET /train/{number} → data field) ─────────────────────────

data class Train(
    @SerializedName(value = "train_no",    alternate = ["trainNo", "number", "train_number"])
    val number: String,
    @SerializedName(value = "train_name",  alternate = ["trainName", "name"])
    val name: String,
    @SerializedName(value = "from_stn_name", alternate = ["source", "fromStnName", "from_station_name", "from"])
    val source: String = "",
    @SerializedName(value = "from_stn_code", alternate = ["sourceCode", "fromStnCode", "from_station_code"])
    val sourceCode: String = "",
    @SerializedName(value = "to_stn_name",   alternate = ["destination", "toStnName", "to_station_name", "to"])
    val destination: String = "",
    @SerializedName(value = "to_stn_code",   alternate = ["destinationCode", "toStnCode", "to_station_code"])
    val destinationCode: String = "",
    @SerializedName(value = "from_time",   alternate = ["departureTime", "departure", "dep_time", "fromTime"])
    val departureTime: String = "",
    @SerializedName(value = "to_time",     alternate = ["arrivalTime", "arrival", "arr_time", "toTime"])
    val arrivalTime: String = "",
    @SerializedName(value = "travel_time", alternate = ["duration", "travelTime"])
    val duration: String = "",
    @SerializedName(value = "distance_km", alternate = ["distance"])
    val distance: String = "",
    @SerializedName(value = "runs_on",     alternate = ["runsOn"])
    val runsOn: List<String> = emptyList(),
    @SerializedName(value = "available_classes", alternate = ["classes", "availableClasses"])
    val classes: List<String> = emptyList(),
    val type: String = ""
)

// ── Station (matches route items inside GET /schedule/{number}) ───────────────

data class Station(
    @SerializedName(value = "station_code", alternate = ["stationCode", "code", "stnCode"])
    val code: String,
    @SerializedName(value = "station_name", alternate = ["stationName", "name", "stnName"])
    val name: String,
    @SerializedName(value = "arrival",      alternate = ["arrivalTime", "arr_time", "arrTime", "scheduled_arrival"])
    val arrival: String = "",
    @SerializedName(value = "departure",    alternate = ["departureTime", "dep_time", "depTime", "scheduled_departure"])
    val departure: String = "",
    @SerializedName(value = "halt_minutes", alternate = ["halt", "haltMinutes", "haltDuration"])
    val halt: Int = 0,
    @SerializedName(value = "distance_km",  alternate = ["distance", "distanceKm"])
    val distance: String = "",
    val platform: String = "",
    @SerializedName(value = "day",          alternate = ["dayNo", "dayNumber"])
    val day: String = "1"
)

// ── Train Schedule (matches GET /schedule/{number} → data field) ──────────────

data class TrainSchedule(
    @SerializedName(value = "train_no",        alternate = ["trainNo"])
    val trainNo: String,
    @SerializedName(value = "train_name",      alternate = ["trainName"])
    val trainName: String,
    @SerializedName(value = "from_stn_code",   alternate = ["fromCode"])
    val fromCode: String = "",
    @SerializedName(value = "to_stn_code",     alternate = ["toCode"])
    val toCode: String = "",
    @SerializedName(value = "total_stations",  alternate = ["totalStations"])
    val totalStations: Int = 0,
    @SerializedName(value = "route",           alternate = ["stationList", "stations", "station_list", "routeList"])
    val route: List<Station> = emptyList()
)

// ── LiveStatus ────────────────────────────────────────────────────────────────

data class LiveStatus(
    @SerializedName("train_number")          val trainNumber: String,
    @SerializedName("train_name")            val trainName: String,
    @SerializedName("current_station_code")  val currentStationCode: String = "",
    @SerializedName("current_station_name")  val currentStationName: String = "",
    val delay: Int = 0,
    val speed: Int = 0,
    @SerializedName("last_updated")          val lastUpdated: String = "",
    val stations: List<Station> = emptyList()
)

// ── PNR ───────────────────────────────────────────────────────────────────────

data class PNRStatus(
    @SerializedName("pnr_number", alternate = ["PnrNumber", "pnrNumber"])
    val pnrNumber: String = "",
    @SerializedName("train_number", alternate = ["TrainNo", "trainNo", "trainNumber"])
    val trainNumber: String = "",
    @SerializedName("train_name", alternate = ["TrainName", "trainName"])
    val trainName: String = "",
    // API returns sourceStation / destinationStation
    @SerializedName("from", alternate = ["From", "from_station", "sourceStation"])
    val from: String = "",
    @SerializedName("to", alternate = ["To", "to_station", "destinationStation"])
    val to: String = "",
    @SerializedName("boarding_point", alternate = ["BoardingPoint", "boardingPoint"])
    val boardingPoint: String = "",
    @SerializedName("reservation_upto", alternate = ["reservationUpto"])
    val reservationUpto: String = "",
    @SerializedName("booking_status", alternate = ["BookingStatus"])
    val bookingStatus: String = "",
    @SerializedName("current_status", alternate = ["CurrentStatus"])
    val currentStatus: String = "",
    @SerializedName("coach", alternate = ["Coach"])
    val coach: String = "",
    @SerializedName("berth", alternate = ["Berth"])
    val berth: String = "",
    // API returns passengerList
    @SerializedName("passengers", alternate = ["Passengers", "passengerList"])
    val passengers: List<Passenger> = emptyList(),
    // Journey metadata
    @SerializedName("date_of_journey", alternate = ["dateOfJourney"])
    val dateOfJourney: String = "",
    @SerializedName("journey_class", alternate = ["journeyClass"])
    val journeyClass: String = "",
    @SerializedName("chart_status", alternate = ["chartStatus"])
    val chartStatus: String = "",
    @SerializedName("quota", alternate = ["Quota"])
    val quota: String = "",
    @SerializedName("number_of_passenger", alternate = ["numberOfpassenger", "numberOfPassenger"])
    val numberOfPassenger: Int = 0,
    @SerializedName("booking_fare", alternate = ["bookingFare"])
    val bookingFare: Int = 0,
    @SerializedName("ticket_fare", alternate = ["ticketFare"])
    val ticketFare: Int = 0,
    @SerializedName("information_message", alternate = ["informationMessage"])
    val informationMessage: List<String> = emptyList(),
    @SerializedName("arrival_date", alternate = ["arrivalDate"])
    val arrivalDate: String = "",
    @SerializedName("distance", alternate = ["Distance"])
    val distance: Int = 0,
    @SerializedName("is_wl", alternate = ["isWL"])
    val isWL: String = "N"
)

data class Passenger(
    @SerializedName("passenger_serial_number", alternate = ["passengerSerialNumber", "serial_number", "serialNumber", "Number"])
    val serialNumber: Int = 0,
    @SerializedName("booking_status", alternate = ["BookingStatus", "bookingStatus"])
    val bookingStatus: String = "",
    @SerializedName("current_status", alternate = ["CurrentStatus", "currentStatus"])
    val currentStatus: String = "",
    // Coach & berth (booking)
    @SerializedName("booking_coach_id", alternate = ["bookingCoachId"])
    val bookingCoachId: String = "",
    @SerializedName("booking_berth_no", alternate = ["bookingBerthNo"])
    val bookingBerthNo: Int = 0,
    @SerializedName("booking_berth_code", alternate = ["bookingBerthCode"])
    val bookingBerthCode: String = "",
    @SerializedName("booking_status_details", alternate = ["bookingStatusDetails"])
    val bookingStatusDetails: String = "",
    // Coach & berth (current — may differ after vikalp)
    @SerializedName("current_coach_id", alternate = ["currentCoachId"])
    val currentCoachId: String = "",
    @SerializedName("current_berth_no", alternate = ["currentBerthNo"])
    val currentBerthNo: Int = 0,
    @SerializedName("current_berth_code", alternate = ["currentBerthCode"])
    val currentBerthCode: String = "",
    @SerializedName("current_status_details", alternate = ["currentStatusDetails"])
    val currentStatusDetails: String = "",
    @SerializedName("passenger_quota", alternate = ["passengerQuota"])
    val passengerQuota: String = ""
)

// ── Saved Train ───────────────────────────────────────────────────────────────

data class SavedTrain(
    val id: String = "",
    val trainNumber: String,
    val trainName: String,
    val from: String = "",
    val to: String = "",
    val userId: String = "",
    val savedAt: Long = 0L
)

// ── Saved Ticket ──────────────────────────────────────────────────────────────

data class SavedTicket(
    val id: String = "",
    val pnrNumber: String,
    val trainNumber: String,
    val trainName: String,
    val from: String = "",
    val to: String = "",
    val currentStatus: String = "",
    val coach: String = "",
    val berth: String = "",
    val savedAt: Long = 0L
)

// ── Saved Station Alert ───────────────────────────────────────────────────────

data class SavedStationAlert(
    val id: String = "",
    val stationCode: String,
    val stationName: String,
    val trainNumber: String,
    val trainName: String,
    val eta: String = "",
    val minutesBefore: Int = 15,
    val savedAt: Long = 0L
)

// ── Station Autocomplete (GET /autocomplete/station/{q}) ──────────────────────

data class StationSearchResult(
    @SerializedName(value = "station_code", alternate = ["code", "stationCode"])
    val code: String,
    @SerializedName(value = "station_name", alternate = ["name", "stationName"])
    val name: String
)

// ── Station Trains (GET /station/{code}/trains) ────────────────────────────────

data class StationTrain(
    @SerializedName(value = "train_no",   alternate = ["trainNo", "number", "train_number"])
    val number: String = "",
    @SerializedName(value = "train_name", alternate = ["trainName", "name"])
    val name: String = "",
    val type: String = "",
    @SerializedName(value = "source",       alternate = ["from_stn_name", "from", "fromStnName", "from_station"])
    val source: String = "",
    @SerializedName(value = "destination",  alternate = ["to_stn_name",   "to",   "toStnName",   "to_station"])
    val destination: String = "",
    @SerializedName(value = "scheduled_departure", alternate = ["departure", "dep_time", "departureTime", "from_time"])
    val departure: String = "",
    @SerializedName(value = "scheduled_arrival",   alternate = ["arrival",   "arr_time", "arrivalTime",   "to_time"])
    val arrival: String = "",
    val platform: String = "",
    @SerializedName(value = "delay_minutes", alternate = ["delay", "delayMinutes"])
    val delayMinutes: Int = 0,
    val status: String = "",
    @SerializedName(value = "originates_here", alternate = ["originatesHere"])
    val originatesHere: Boolean = false,
    @SerializedName(value = "terminates_here", alternate = ["terminatesHere"])
    val terminatesHere: Boolean = false
)

// ── API Response wrapper ──────────────────────────────────────────────────────

data class ApiResponse<T>(
    @SerializedName(value = "status", alternate = ["success"])
    val status: Boolean = false,
    val message: String = "",
    val data: T? = null
)
