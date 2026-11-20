package com.yatrain.app.services

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.yatrain.app.BuildConfig
import com.yatrain.app.models.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.util.concurrent.TimeUnit

// Real endpoints verified against irctc-indian-railway-pnr-status.p.rapidapi.com:
//   GET /train/{trainNumber}      → single train info
//   GET /schedule/{trainNumber}   → full route/schedule
//   GET /getPNRStatus/{pnr}       → PNR passenger status
//   GET /station/{stationCode}    → station metadata
// Note: no search-by-name endpoint exists on this API — use train number or PNR.

private const val IRCTC_HOST = "irctc-indian-railway-pnr-status.p.rapidapi.com"
private const val IRCTC_BASE  = "https://$IRCTC_HOST/"

interface IRCTCApi {

    @GET("train/{trainNumber}")
    suspend fun getTrainByNumber(
        @Path("trainNumber")        trainNumber: String,
        @Header("X-RapidAPI-Key")   apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("X-RapidAPI-Host")  host: String   = IRCTC_HOST
    ): JsonElement   // raw — train object may sit at root or inside "data"

    @GET("schedule/{trainNumber}")
    suspend fun getSchedule(
        @Path("trainNumber")        trainNumber: String,
        @Header("X-RapidAPI-Key")   apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("X-RapidAPI-Host")  host: String   = IRCTC_HOST
    ): JsonElement   // raw — route may sit at root or inside "data"

    @GET("getPNRStatus/{pnr}")
    suspend fun getPNRStatus(
        @Path("pnr")                pnr: String,
        @Header("X-RapidAPI-Key")   apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("X-RapidAPI-Host")  host: String   = IRCTC_HOST
    ): JsonElement  // PNR fields are at root level, not inside a "data" wrapper

    @GET("autocomplete/station/{query}")
    suspend fun autocompleteStation(
        @Path("query")              query: String,
        @Query("limit")             limit: Int = 20,
        @Header("X-RapidAPI-Key")   apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("X-RapidAPI-Host")  host: String   = IRCTC_HOST
    ): ApiResponse<JsonElement>

    @GET("station/{code}/trains")
    suspend fun getStationTrains(
        @Path("code")               code: String,
        @Header("X-RapidAPI-Key")   apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("X-RapidAPI-Host")  host: String   = IRCTC_HOST
    ): JsonElement   // raw — API may return trains at root or inside a wrapper

    @GET("between/{fromCode}/{toCode}")
    suspend fun getBetweenTrains(
        @Path("fromCode")           fromCode: String,
        @Path("toCode")             toCode: String,
        @Header("X-RapidAPI-Key")   apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("X-RapidAPI-Host")  host: String   = IRCTC_HOST
    ): JsonElement   // raw — same reason
}

interface RailRadarApi {
    @GET("api/v1/trains/{trainNumber}/liveStatus")
    suspend fun getLiveStatus(
        @Path("trainNumber") trainNumber: String,
        @Header("apikey")    apiKey: String = BuildConfig.RAILRADAR_KEY
    ): LiveStatus
}

// Retries up to 2 times on timeout or connection reset — covers transient RapidAPI hangs.
private class RetryInterceptor(private val maxRetries: Int = 2) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastError: IOException? = null
        while (attempt <= maxRetries) {
            try {
                val req = chain.request().newBuilder().build()
                val resp = chain.proceed(req)
                if (resp.isSuccessful || attempt == maxRetries) return resp
                resp.close()
            } catch (e: IOException) {
                lastError = e
            }
            attempt++
        }
        throw lastError ?: IOException("Request failed after $maxRetries retries")
    }
}

// TypeAdapter (not JsonDeserializer) so null JSON values are also handled —
// JsonDeserializer is bypassed by Gson for null, TypeAdapter.read() is always called.
private class LenientStringAdapter : TypeAdapter<String>() {
    override fun write(out: JsonWriter, value: String?) {
        if (value == null) out.nullValue() else out.value(value)
    }
    override fun read(`in`: JsonReader): String = when (`in`.peek()) {
        JsonToken.NULL -> { `in`.nextNull(); "" }
        JsonToken.BEGIN_OBJECT, JsonToken.BEGIN_ARRAY -> { `in`.skipValue(); "" }
        else -> `in`.nextString()   // handles STRING, NUMBER, BOOLEAN
    }
}

class TrainAPIService {

    private val gson = GsonBuilder()
        .registerTypeAdapter(String::class.java, LenientStringAdapter())
        .create()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        // Force HTTP/1.1 — OkHttp's HTTP/2 multiplexing causes silent hangs with
        // some RapidAPI server configurations; iOS URLSession negotiates differently.
        .protocols(listOf(Protocol.HTTP_1_1))
        .addInterceptor(RetryInterceptor(maxRetries = 2))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BASIC
            else
                HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val irctcApi: IRCTCApi = Retrofit.Builder()
        .baseUrl(IRCTC_BASE)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(IRCTCApi::class.java)

    private val railRadarApi: RailRadarApi = Retrofit.Builder()
        .baseUrl("https://railradar.trainman.in/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RailRadarApi::class.java)

    // Returns single train wrapped in a list so callers can reuse list-based UI
    suspend fun searchTrains(trainNumber: String): Result<List<Train>> = runCatching {
        val raw = irctcApi.getTrainByNumber(trainNumber)
        val root = if (raw.isJsonObject) raw.asJsonObject else throw Exception("Train not found")

        // Check error envelope
        val ok = root.get("success")?.let { runCatching { it.asBoolean }.getOrNull() }
                 ?: root.get("status")?.let { runCatching { it.asBoolean }.getOrNull() }
        if (ok == false) {
            throw Exception(root.get("message")?.asString?.ifBlank { null } ?: "Train not found")
        }

        // Train object may be inside "data", "train", or directly at root
        val source = listOf("data", "train", "trainDetails")
            .mapNotNull { root.get(it) }
            .firstOrNull { it.isJsonObject }
            ?.asJsonObject ?: root

        val train = gson.fromJson(source, Train::class.java)
        if (train.number.isBlank()) throw Exception(
            root.get("message")?.asString?.ifBlank { null } ?: "Train \"$trainNumber\" not found"
        )
        listOf(train)
    }

    suspend fun getSchedule(trainNumber: String): Result<TrainSchedule> = runCatching {
        val raw = irctcApi.getSchedule(trainNumber)
        val root = if (raw.isJsonObject) raw.asJsonObject else throw Exception("Schedule not found")

        // Check error envelope
        val ok = root.get("success")?.let { runCatching { it.asBoolean }.getOrNull() }
                 ?: root.get("status")?.let { runCatching { it.asBoolean }.getOrNull() }
        if (ok == false) {
            throw Exception(root.get("message")?.asString?.ifBlank { null } ?: "Schedule not found")
        }

        // Schedule object may be inside "data" or at root; route/stations may be under various keys
        val source = root.get("data")?.takeIf { it.isJsonObject }?.asJsonObject ?: root
        val schedule = gson.fromJson(source, TrainSchedule::class.java)

        // If Gson couldn't map the route list, try extracting station array manually
        if (schedule.route.isEmpty()) {
            val arr = extractArray(source, "route", "stations", "stationList", "station_list",
                                   "routeList", "schedule", "stops")
            if (arr.size() > 0) {
                val stations = arr.mapNotNull {
                    runCatching { gson.fromJson(it, Station::class.java) }.getOrNull()
                }.filter { it.code.isNotBlank() }
                return@runCatching schedule.copy(route = stations)
            }
        }
        schedule
    }

    suspend fun getPNRStatus(pnr: String): Result<PNRStatus> = runCatching {
        val root = irctcApi.getPNRStatus(pnr).asJsonObject

        // Check error envelope
        val successFlag = root.get("success")?.let { runCatching { it.asBoolean }.getOrNull() }
        val statusFlag  = root.get("status") ?.let { runCatching { it.asBoolean }.getOrNull() }
        if (successFlag == false || statusFlag == false) {
            throw Exception(root.get("message")?.asString?.ifBlank { null } ?: "PNR not found")
        }

        // Some API variants wrap the PNR object inside a "data" field; others put it at root
        val dataEl = root.get("data")
        val source = if (dataEl != null && !dataEl.isJsonNull && dataEl.isJsonObject)
            dataEl.asJsonObject else root

        var status = gson.fromJson(source, PNRStatus::class.java)

        // If Gson still couldn't map the PNR number field, use the queried PNR as fallback
        // so the rest of the data (train number, passengers, etc.) is still usable
        if (status.pnrNumber.isBlank()) {
            status = status.copy(pnrNumber = pnr)
        }

        // Require at minimum a train number to proceed — otherwise the response is truly empty
        if (status.trainNumber.isBlank()) throw Exception("PNR details unavailable")
        status
    }

    suspend fun getLiveStatus(trainNumber: String): Result<LiveStatus> = runCatching {
        railRadarApi.getLiveStatus(trainNumber)
    }

    suspend fun autocompleteStation(query: String): Result<List<StationSearchResult>> = runCatching {
        val resp = irctcApi.autocompleteStation(query)
        val element = resp.data ?: return@runCatching emptyList()
        val arr = extractArray(element, "results", "stations", "data")
        arr.mapNotNull { runCatching { gson.fromJson(it, StationSearchResult::class.java) }.getOrNull() }
    }

    suspend fun getStationTrains(code: String): Result<List<StationTrain>> = runCatching {
        val raw = irctcApi.getStationTrains(code)
        // Check error envelope first
        if (raw.isJsonObject) {
            val obj = raw.asJsonObject
            val ok = obj.get("success")?.let { runCatching { it.asBoolean }.getOrNull() }
                ?: obj.get("status")?.let { runCatching { it.asBoolean }.getOrNull() }
            if (ok == false) return@runCatching emptyList()
        }
        // Trains may be at root, inside "data", or under various array keys
        val arr = extractArray(raw, "trains", "trainsList", "train_list", "data", "stationTrains", "train_schedule")
        arr.mapNotNull { runCatching { gson.fromJson(it, StationTrain::class.java) }.getOrNull() }
            .filter { it.number.isNotBlank() }
            .sortedBy { t ->
                val dep = t.departure.orEmpty()
                val raw2 = if (dep.isNotBlank()) dep else t.arrival.orEmpty()
                raw2.replace(".", ":")
            }
    }

    // The /station/{code}/trains endpoint is broken on this API (always returns "No trains found").
    // This method uses the working /between/{from}/{to} endpoint as a replacement: it queries
    // the two busiest rail terminals (NDLS + MMCT) in parallel and merges unique results.
    // from_time in the between response is the departure time FROM the detected station specifically.
    suspend fun getTrainsAtStation(stationCode: String): Result<List<StationTrain>> = runCatching {
        val terminals = listOf("NDLS", "MMCT").filter { it != stationCode }
        val seen   = mutableSetOf<String>()
        val result = mutableListOf<StationTrain>()
        coroutineScope {
            terminals.map { terminal ->
                async {
                    try {
                        val raw = irctcApi.getBetweenTrains(stationCode, terminal)
                        extractArray(raw, "trains", "train_list", "trainsList", "data")
                            .mapNotNull { parseBetweenAsStationTrain(it) }
                    } catch (_: Exception) { emptyList() }
                }
            }.awaitAll().flatten().forEach { t ->
                if (seen.add(t.number)) result.add(t)
            }
        }
        result.sortedBy { t -> normaliseTime(if (t.departure.isNotBlank()) t.departure else t.arrival) }
    }

    private fun parseBetweenAsStationTrain(el: JsonElement): StationTrain? {
        if (!el.isJsonObject) return null
        val obj = el.asJsonObject
        val number = obj.str("train_no", "trainNo", "number") ?: return null
        return StationTrain(
            number      = number,
            name        = obj.str("train_name", "trainName", "name") ?: "",
            // actual train origin/destination, not just the queried station pair
            source      = obj.str("source_stn_name", "from_stn_name", "source", "from") ?: "",
            destination = obj.str("dstn_stn_name",   "to_stn_name",   "destination", "to") ?: "",
            departure   = normaliseTime(obj.str("from_time", "departure_time", "std") ?: ""),
            arrival     = normaliseTime(obj.str("to_time",   "arrival_time",   "sta") ?: "")
        )
    }

    suspend fun getBetweenTrains(fromCode: String, toCode: String): Result<List<com.yatrain.app.models.BetweenTrain>> = runCatching {
        val raw = irctcApi.getBetweenTrains(fromCode, toCode)
        val arr = extractArray(raw, "trains", "train_list", "trainsList", "data")
        arr.mapNotNull { parseBetweenTrain(it) }.filter { it.number.isNotBlank() }
    }

    private fun parseBetweenTrain(el: JsonElement): com.yatrain.app.models.BetweenTrain? {
        if (!el.isJsonObject) return null
        val obj = el.asJsonObject
        val number = obj.str("train_no", "trainNo", "number", "train_number") ?: return null
        return com.yatrain.app.models.BetweenTrain(
            number          = number,
            name            = obj.str("train_name", "trainName", "name") ?: "",
            source          = obj.str("from_stn_name", "source", "from", "fromStnName", "from_station_name") ?: "",
            sourceCode      = obj.str("from_stn_code", "sourceCode", "fromStnCode", "from_station_code") ?: "",
            destination     = obj.str("to_stn_name", "destination", "to", "toStnName", "to_station_name") ?: "",
            destinationCode = obj.str("to_stn_code", "destinationCode", "toStnCode", "to_station_code") ?: "",
            departureTime   = normaliseTime(obj.str("from_time", "departure_time", "source_departure_time", "scheduled_departure", "std") ?: ""),
            arrivalTime     = normaliseTime(obj.str("to_time", "arrival_time", "dest_arrival_time", "scheduled_arrival", "sta") ?: ""),
            duration        = obj.str("travel_time", "duration", "travelTime") ?: "",
            runsOn          = parseRunningDays(obj)
        )
    }

    private fun normaliseTime(raw: String): String {
        if (raw.isBlank()) return ""
        // Normalise dot separator, strip "+1"/"+2" day offsets
        return raw.replace(".", ":").substringBefore("+").substringBefore(" ").trim().take(5)
    }

    private fun parseRunningDays(obj: com.google.gson.JsonObject): List<String> {
        val labels  = listOf("M", "T", "W", "Th", "F", "Sa", "Su")
        val dayKeys = listOf(
            listOf("mon", "monday"),
            listOf("tue", "tuesday"),
            listOf("wed", "wednesday"),
            listOf("thu", "thursday"),
            listOf("fri", "friday"),
            listOf("sat", "saturday"),
            listOf("sun", "sunday")
        )
        // Format 1 — running_days dict: {"mon": true, "tue": false, ...}
        val rdEl = obj.get("running_days")
        if (rdEl != null && !rdEl.isJsonNull && rdEl.isJsonObject) {
            val rd = rdEl.asJsonObject
            return dayKeys.mapIndexedNotNull { i, keys ->
                if (keys.any { rd.get(it)?.asBoolean == true }) labels[i] else null
            }
        }
        // Format 2 — bit string: {"running_days": "1010100"}
        if (rdEl != null && !rdEl.isJsonNull && rdEl.isJsonPrimitive) {
            val s = runCatching { rdEl.asString.trim() }.getOrNull() ?: ""
            if (s.length == 7 && s.all { it == '0' || it == '1' }) {
                return s.mapIndexedNotNull { i, c -> if (c == '1') labels[i] else null }
            }
        }
        // Format 3 — flat booleans at top level: {"mon": true, "tue": false, ...}
        return dayKeys.mapIndexedNotNull { i, keys ->
            if (keys.any { obj.get(it)?.asBoolean == true }) labels[i] else null
        }
    }

    private fun com.google.gson.JsonObject.str(vararg keys: String): String? {
        for (key in keys) {
            val el = get(key) ?: continue
            if (!el.isJsonPrimitive) continue
            val s = runCatching { el.asString }.getOrNull() ?: continue
            if (s.isNotBlank()) return s
        }
        return null
    }

    /** Extracts a JsonArray from an element — either a direct array, or an object
     *  containing an array under one of the given field names (tried in order).
     *  Falls back to the first JsonArray-valued key in the object, so unknown
     *  wrapper field names (e.g. API changes) are handled automatically. */
    private fun extractArray(element: JsonElement, vararg fieldNames: String): JsonArray {
        if (element.isJsonArray) return element.asJsonArray
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            // Try known names first
            for (name in fieldNames) {
                val child = obj.get(name) ?: continue
                if (child.isJsonArray) return child.asJsonArray
                // Also recurse one level in case it's { "data": { "trains": [...] } }
                if (child.isJsonObject) {
                    val inner = extractArray(child, *fieldNames)
                    if (inner.size() > 0) return inner
                }
            }
            // Last resort: return the first JsonArray value found in any key
            for (entry in obj.entrySet()) {
                if (entry.value.isJsonArray) return entry.value.asJsonArray
            }
        }
        return JsonArray()
    }
}
