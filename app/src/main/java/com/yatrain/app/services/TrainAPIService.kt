package com.yatrain.app.services

import com.yatrain.app.BuildConfig
import com.yatrain.app.models.*
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

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
    ): ApiResponse<StationAutocompleteData>

    @GET("station/{code}/trains")
    suspend fun getStationTrains(
        @Path("code")               code: String,
        @Header("X-RapidAPI-Key")   apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("X-RapidAPI-Host")  host: String   = IRCTC_HOST
    ): ApiResponse<StationTrainsData>
}

interface RailRadarApi {
    @GET("api/v1/trains/{trainNumber}/liveStatus")
    suspend fun getLiveStatus(
        @Path("trainNumber") trainNumber: String,
        @Header("apikey")    apiKey: String = BuildConfig.RAILRADAR_KEY
    ): LiveStatus
}

class TrainAPIService {

    private val okHttpClient = OkHttpClient.Builder()
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
        .addConverterFactory(GsonConverterFactory.create())
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
        resp.data?.results ?: emptyList()
    }

    suspend fun getStationTrains(code: String): Result<List<StationTrain>> = runCatching {
        val resp = irctcApi.getStationTrains(code)
        val trains = resp.data?.trains ?: emptyList()
        trains.sortedBy { t ->
            val raw = if (t.departure.isNotBlank()) t.departure else t.arrival
            raw.replace(".", ":")
        }
    }
}
