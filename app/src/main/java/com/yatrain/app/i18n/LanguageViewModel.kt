package com.yatrain.app.i18n

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val android.app.Application.dataStore by preferencesDataStore("yatrain_prefs")
private val KEY_LANGUAGE = stringPreferencesKey("language_code")

class LanguageViewModel(app: Application) : AndroidViewModel(app) {

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    /** Mirrors MlKitTranslator.state so any composable can observe download progress. */
    val translatorState: StateFlow<MlKitTranslator.State> = MlKitTranslator.state

    init {
        viewModelScope.launch {
            app.dataStore.data
                .map { it[KEY_LANGUAGE] ?: "en" }
                .collect { code ->
                    _languageCode.value = code
                    MlKitTranslator.prepare(code)   // download model on startup if needed
                }
        }
    }

    fun setLanguage(code: String) {
        _languageCode.value = code
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[KEY_LANGUAGE] = code
            }
            MlKitTranslator.prepare(code)           // download model when user picks a language
        }
    }
}
