package com.yatrain.app.ui.screens.subscription

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yatrain.app.models.SubscriptionTier
import com.yatrain.app.services.SubscriptionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionViewModel(app: Application) : AndroidViewModel(app) {

    val manager = SubscriptionManager(app)

    // ── Tier + derived isPro ──────────────────────────────────────────────────

    val tier: StateFlow<SubscriptionTier> = manager.tier
        .stateIn(viewModelScope, SharingStarted.Eagerly, SubscriptionTier.FREE)

    val isPro: StateFlow<Boolean> = manager.tier
        .map { it == SubscriptionTier.PRO }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── UI feedback ───────────────────────────────────────────────────────────

    val purchaseLoading: StateFlow<Boolean> = manager.purchaseLoading
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val purchaseError: StateFlow<String?> = manager.purchaseError
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Real prices (from Play Billing; fall back to ₹49 / ₹599) ─────────────

    val monthlyPrice: StateFlow<String> = manager.tier
        .map { manager.getMonthlyPrice() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "₹49")

    val annualPrice: StateFlow<String> = manager.tier
        .map { manager.getAnnualPrice() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "₹599")

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch { manager.start() }
    }

    override fun onCleared() {
        super.onCleared()
        manager.destroy()
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun purchaseMonthly(activity: Activity) = manager.purchaseMonthly(activity)
    fun purchaseAnnual(activity: Activity)  = manager.purchaseAnnual(activity)

    fun restore() {
        viewModelScope.launch { manager.refreshEntitlements() }
    }

    fun clearError() = manager.clearError()

    fun manageSubscription(context: Context) {
        val uri = Uri.parse(
            "https://play.google.com/store/account/subscriptions" +
            "?sku=${com.yatrain.app.utils.Constants.MONTHLY_PRODUCT_ID}" +
            "&package=${context.packageName}"
        )
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
