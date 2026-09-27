package com.yatrain.app.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yatrain.app.models.YatrainUser
import com.yatrain.app.services.AuthService
import com.yatrain.app.services.SavedDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val authService = AuthService()
    private val repo = SavedDataRepository.getInstance(app)

    val currentUser: StateFlow<YatrainUser?> = authService.currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    private val _deleteLoading = MutableStateFlow(false)
    val deleteLoading: StateFlow<Boolean> = _deleteLoading.asStateFlow()

    init {
        viewModelScope.launch { authService.initialize() }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            authService.signIn(email, password)
                .onSuccess { onSuccess() }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun signUp(email: String, password: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            authService.signUp(email, password, name)
                .onSuccess { onSuccess() }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun resetPassword(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            authService.sendPasswordReset(email)
                .onSuccess { onSuccess() }
                .onFailure {
                    val msg = it.message ?: "Failed to send reset link. Please try again."
                    _error.value = msg
                    onFailure(msg)
                }
            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
    fun clearDeleteError() { _deleteError.value = null }

    suspend fun checkHealth(): String? = authService.checkHealth()

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            val userId = currentUser.value?.id
            if (userId != null) {
                try { repo.clearAll(userId) } catch (_: Exception) {}
            }
            authService.signOut()
            onComplete()
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            _deleteLoading.value = true
            _deleteError.value = null
            val userId = currentUser.value?.id
            authService.deleteAccount()
                .onSuccess {
                    if (userId != null) try { repo.clearAll(userId) } catch (_: Exception) {}
                    onComplete()
                }
                .onFailure {
                    _deleteError.value = it.message ?: "Failed to delete account. Please try again."
                }
            _deleteLoading.value = false
        }
    }
}
