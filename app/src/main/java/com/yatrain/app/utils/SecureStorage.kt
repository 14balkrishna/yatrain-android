package com.yatrain.app.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Android equivalent of iOS Keychain
class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "yatrain_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(key: String, value: String) = prefs.edit().putString(key, value).apply()

    fun get(key: String): String? = prefs.getString(key, null)

    fun delete(key: String) = prefs.edit().remove(key).apply()

    fun clear() = prefs.edit().clear().apply()

    companion object {
        const val KEY_ACCESS_TOKEN  = "yatrain_access_token"
        const val KEY_REFRESH_TOKEN = "yatrain_refresh_token"
        const val KEY_USER_ID       = "yatrain_user_id"
    }
}
