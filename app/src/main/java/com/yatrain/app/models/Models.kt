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
    @SerializedName("train_no")       val number: String,
    @SerializedName("train_name")     val name: String,
    @SerializedName("from_stn_name")  val source: String = "",
    @SerializedName("from_stn_code")  val sourceCode: String = "",
    @SerializedName("to_stn_name")    val destination: String = "",
    @SerializedName("to_stn_code")    val destinationCode: String = "",
    @SerializedName("from_time")      val departureTime: String = "",
    @SerializedName("to_time")        val arrivalTime: String = "",
    @SerializedName("travel_time")    val duration: String = "",
    @SerializedName("distance_km")    val distance: String = "",
    @SerializedName("runs_on")        val runsOn: List<String> = emptyList(),
    @SerializedName("available_classes") val classes: List<String> = emptyList(),
    val type: String = ""
)

// ── Station (matches route items inside GET /schedule/{number}) ───────────────

data class Station(
    @SerializedName("station_code")  val code: String,
    @SerializedName("station_name")  val name: String,
    val arrival: String = "",
    val departure: String = "",
    @SerializedName("halt_minutes")  val halt: Int = 0,
    @SerializedName("distance_km")   val distance: String = "",
    val platform: String = "",
    val day: String = "1"
)

// ── Train Schedule (matches GET /schedule/{number} → data field) ──────────────

data class TrainSchedule(
    @SerializedName("train_no")       val trainNo: String,
    @SerializedName("train_name")     val trainName: String,
    @SerializedName("from_stn_code")  val fromCode: String = "",
    @SerializedName("to_stn_code")    val toCode: String = "",
    @SerializedName("total_stations") val totalStations: Int = 0,
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
    @SerializedName("station_code") val code: String,
    @SerializedName("station_name") val name: String
)

data class StationAutocompleteData(
    val results: List<StationSearchResult> = emptyList()
)

// ── Station Trains (GET /station/{code}/trains) ────────────────────────────────

data class StationTrain(
    @SerializedName("train_no")           val number: String,
    @SerializedName("train_name")         val name: String,
    val type: String = "",
    val source: String = "",
    val destination: String = "",
    @SerializedName("scheduled_departure") val departure: String = "",
    @SerializedName("scheduled_arrival")   val arrival: String = "",
    val platform: String = "",
    @SerializedName("delay_minutes")       val delayMinutes: Int = 0,
    val status: String = "",
    @SerializedName("originates_here")     val originatesHere: Boolean = false,
    @SerializedName("terminates_here")     val terminatesHere: Boolean = false
)

data class StationTrainsData(
    val trains: List<StationTrain> = emptyList()
)

// ── API Response wrapper (API returns "success" bool) ─────────────────────────

data class ApiResponse<T>(
    @SerializedName("success") val status: Boolean,
    val message: String = "",
    val data: T? = null
)
