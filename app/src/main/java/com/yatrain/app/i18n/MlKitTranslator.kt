package com.yatrain.app.i18n

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.yatrain.app.models.PNRStatus
import com.yatrain.app.models.Station
import com.yatrain.app.models.Train
import com.yatrain.app.ui.screens.track.LiveTrainEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// ── Singleton translation engine ──────────────────────────────────────────────
// Uses Google ML Kit on-device translation — free, no API key, works offline
// after a one-time ~25–40 MB language-model download per language.

object MlKitTranslator {

    sealed class State {
        object Idle        : State()
        object Downloading : State()
        object Ready       : State()
        data class Failed(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    var currentLang: String = "en"
        private set

    val isEnglish: Boolean get() = currentLang == "en"

    private var translator: Translator? = null

    /**
     * Call whenever the app language changes.
     * Downloads the EN→[langCode] model if not already on device (one-time, ~25–40 MB).
     * Safe to call multiple times for the same language — no-ops after first download.
     */
    suspend fun prepare(langCode: String) {
        if (langCode == currentLang && (_state.value is State.Ready || langCode == "en")) return
        currentLang = langCode

        if (langCode == "en") {
            translator?.close(); translator = null
            _state.value = State.Ready
            return
        }

        _state.value = State.Downloading
        withContext(Dispatchers.IO) {
            runCatching {
                translator?.close()
                val mlCode = TranslateLanguage.fromLanguageTag(langCode)
                    ?: TranslateLanguage.ENGLISH
                val opts = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(mlCode)
                    .build()
                val t = Translation.getClient(opts)
                t.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                translator = t
                _state.value = State.Ready
            }.onFailure { e ->
                _state.value = State.Failed(e.message ?: "Language model download failed")
            }
        }
    }

    /** Translates a single English string. Falls back to the original on any error. */
    suspend fun translate(text: String): String {
        if (isEnglish || text.isBlank()) return text
        val t = translator ?: return text
        return runCatching {
            withContext(Dispatchers.IO) { t.translate(text).await() }
        }.getOrDefault(text)
    }
}

// ── Typed batch helpers ───────────────────────────────────────────────────────
// All return the original list unchanged when language is English (zero overhead).
// Parallel async so a list of 50 items translates in ~200 ms rather than ~1 s.

@JvmName("translateStationList")
suspend fun List<Station>.withTranslatedNames(): List<Station> {
    if (MlKitTranslator.isEnglish) return this
    return coroutineScope {
        map { s -> async { s.copy(name = MlKitTranslator.translate(s.name)) } }.awaitAll()
    }
}

@JvmName("translateLiveTrainList")
suspend fun List<LiveTrainEntry>.withTranslatedNames(): List<LiveTrainEntry> {
    if (MlKitTranslator.isEnglish) return this
    return coroutineScope {
        map { e ->
            async {
                e.copy(
                    name     = MlKitTranslator.translate(e.name),
                    fromName = MlKitTranslator.translate(e.fromName),
                    toName   = MlKitTranslator.translate(e.toName)
                )
            }
        }.awaitAll()
    }
}

@JvmName("translateTrainList")
suspend fun List<Train>.withTranslatedNames(): List<Train> {
    if (MlKitTranslator.isEnglish) return this
    return coroutineScope {
        map { t ->
            async {
                t.copy(
                    name        = MlKitTranslator.translate(t.name),
                    source      = MlKitTranslator.translate(t.source),
                    destination = MlKitTranslator.translate(t.destination)
                )
            }
        }.awaitAll()
    }
}

suspend fun PNRStatus.withTranslatedNames(): PNRStatus {
    if (MlKitTranslator.isEnglish) return this
    return copy(
        trainName       = MlKitTranslator.translate(trainName),
        from            = MlKitTranslator.translate(from),
        to              = MlKitTranslator.translate(to),
        boardingPoint   = MlKitTranslator.translate(boardingPoint),
        reservationUpto = MlKitTranslator.translate(reservationUpto)
    )
}
