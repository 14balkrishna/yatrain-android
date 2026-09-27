package com.yatrain.app.services

import com.yatrain.app.YatrainApplication
import com.yatrain.app.models.YatrainUser
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AuthService {

    private val supabase = YatrainApplication.supabase

    private val _currentUser = MutableStateFlow<YatrainUser?>(null)
    val currentUser: StateFlow<YatrainUser?> = _currentUser.asStateFlow()

    val isSignedIn: Boolean get() = _currentUser.value != null

    suspend fun initialize() {
        try {
            supabase.auth.awaitInitialization()
            refreshUser()
        } catch (_: Exception) {}
    }

    suspend fun signIn(email: String, password: String): Result<YatrainUser> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        refreshUser() ?: throw Exception("Sign-in succeeded but user data unavailable")
    }

    suspend fun signUp(email: String, password: String, name: String): Result<YatrainUser> = runCatching {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = kotlinx.serialization.json.buildJsonObject {
                put("full_name", kotlinx.serialization.json.JsonPrimitive(name))
            }
        }
        refreshUser() ?: throw Exception("Sign-up succeeded but user data unavailable")
    }

    suspend fun signInWithGoogle(): Result<Unit> = runCatching {
        supabase.auth.signInWith(Google, redirectUrl = "yatrain://auth-callback")
    }

    suspend fun signOut() {
        runCatching { supabase.auth.signOut() }
        _currentUser.value = null
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        supabase.auth.resetPasswordForEmail(email, redirectUrl = "yatrain://reset-password")
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        supabase.auth.updateUser {
            password = newPassword
        }
    }

    suspend fun checkHealth(): String? = withContext(Dispatchers.IO) {
        try {
            val urlStr = "${YatrainApplication.supabase.supabaseUrl}/auth/v1/health"
            val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("apikey", YatrainApplication.supabase.supabaseKey)
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            when (conn.responseCode) {
                200      -> null
                503, 540 -> "Sign-in service appears paused. Visit supabase.com and restore the project, then try again."
                else     -> "Sign-in service returned HTTP ${conn.responseCode}. Check project status at supabase.com."
            }
        } catch (_: java.net.UnknownHostException) {
            "Cannot reach sign-in service. The Supabase project may be paused or its URL may have changed."
        } catch (_: Exception) { null }
    }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val token = supabase.auth.currentAccessTokenOrNull()
            ?: throw Exception("Not signed in")
        supabase.functions.invoke("delete-account")
        signOut()
    }

    private suspend fun refreshUser(): YatrainUser? {
        val info: UserInfo? = runCatching { supabase.auth.retrieveUserForCurrentSession() }.getOrNull()
        val user = info?.let {
            YatrainUser(
                id    = it.id,
                email = it.email,
                name  = it.userMetadata?.get("full_name")?.toString()?.trim('"')
            )
        }
        _currentUser.value = user
        return user
    }
}
