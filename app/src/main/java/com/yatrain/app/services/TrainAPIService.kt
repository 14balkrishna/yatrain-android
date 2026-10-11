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
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
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
    ): ApiResponse<Train>

    @GET("schedule/{trainNumber}")
    suspend fun getSchedule(
        @Path("trainNumber")        trainNumber: String,
        @Header("X-RapidAPI-Key")   apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("X-RapidAPI-Host")  host: String   = IRCTC_HOST
    ): ApiResponse<TrainSchedule>

    @GET("getPNRStatus/{pnr}")
    suspend fun getPNRStatus(
        @Path("pnr")                pnr: String,
        @Header("X-RapidAPI-Key")   apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("X-RapidAPI-Host")  host: String   = IRCTC_HOST
    ): ApiResponse<PNRStatus>

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
    ): ApiResponse<JsonElement>
}

interface RailRadarApi {
    @GET("api/v1/trains/{trainNumber}/liveStatus")
    suspend fun getLiveStatus(
        @Path("trainNumber") trainNumber: String,
        @Header("apikey")    apiKey: String = BuildConfig.RAILRADAR_KEY
    ): LiveStatus
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
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
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
        val resp = irctcApi.getTrainByNumber(trainNumber)
        if (resp.status && resp.data != null) listOf(resp.data)
        else throw Exception(resp.message.ifBlank { "Train not found" })
    }

    suspend fun getSchedule(trainNumber: String): Result<TrainSchedule> = runCatching {
        val resp = irctcApi.getSchedule(trainNumber)
        resp.data ?: throw Exception(resp.message.ifBlank { "Schedule not found" })
    }

    suspend fun getPNRStatus(pnr: String): Result<PNRStatus> = runCatching {
        val resp = irctcApi.getPNRStatus(pnr)
        resp.data ?: throw Exception(resp.message.ifBlank { "PNR not found" })
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
        val resp = irctcApi.getStationTrains(code)
        val element = resp.data ?: return@runCatching emptyList()
        // The API may return data as a direct array OR as an object with a trains/trainsList field
        val arr = extractArray(element, "trains", "trainsList", "train_list", "data", "stationTrains")
        arr.mapNotNull { runCatching { gson.fromJson(it, StationTrain::class.java) }.getOrNull() }
            .filter { it.number.isNotBlank() }
            .sortedBy { t ->
                val dep = t.departure.orEmpty()
                val arr2 = t.arrival.orEmpty()
                val raw = if (dep.isNotBlank()) dep else arr2
                raw.replace(".", ":")
            }
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
