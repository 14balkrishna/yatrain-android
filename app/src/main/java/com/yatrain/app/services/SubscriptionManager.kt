package com.yatrain.app.services

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.yatrain.app.BuildConfig
import com.yatrain.app.models.SubscriptionTier
import com.yatrain.app.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

// Google Play Billing — Production implementation
//
// Play Console setup (one-time):
//   Monetise → Subscriptions → create two subscriptions:
//     Product ID : com.yatrain.app.pro.monthly  (base plan: 1 month,  ₹49)
//     Product ID : com.yatrain.app.pro.annual   (base plan: 1 year,  ₹599)
//   Each subscription needs at least one base plan and one offer activated.
//   App must be published to at least Internal Testing track before purchases work.

class SubscriptionManager(context: Context) : PurchasesUpdatedListener {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _tier = MutableStateFlow(SubscriptionTier.FREE)
    val tier: StateFlow<SubscriptionTier> = _tier.asStateFlow()

    private val _purchaseLoading = MutableStateFlow(false)
    val purchaseLoading: StateFlow<Boolean> = _purchaseLoading.asStateFlow()

    private val _purchaseError = MutableStateFlow<String?>(null)
    val purchaseError: StateFlow<String?> = _purchaseError.asStateFlow()

    private var monthlyProduct: ProductDetails? = null
    private var annualProduct:  ProductDetails? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Billing client ────────────────────────────────────────────────────────

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    suspend fun start() {
        if (BuildConfig.DEBUG_FORCE_PRO) {
            _tier.value = SubscriptionTier.PRO
            return
        }
        if (connectWithRetry()) {
            loadProducts()
            refreshEntitlements()
        }
    }

    private suspend fun connectWithRetry(maxAttempts: Int = 3): Boolean {
        repeat(maxAttempts) { attempt ->
            val connected = suspendCancellableCoroutine { cont ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                    override fun onBillingServiceDisconnected() {
                        cont.resume(false)
                    }
                })
            }
            if (connected) return true
            if (attempt < maxAttempts - 1) delay(1000L * (attempt + 1))
        }
        return false
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return connectWithRetry()
    }

    fun destroy() = billingClient.endConnection()

    // ── Products ──────────────────────────────────────────────────────────────

    private suspend fun loadProducts() {
        if (!ensureConnected()) return

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(Constants.MONTHLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(Constants.ANNUAL_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            monthlyProduct = result.productDetailsList?.firstOrNull {
                it.productId == Constants.MONTHLY_PRODUCT_ID
            }
            annualProduct = result.productDetailsList?.firstOrNull {
                it.productId == Constants.ANNUAL_PRODUCT_ID
            }
        }
    }

    // Returns the cheapest (base) offer token for a product
    private fun ProductDetails.baseOfferToken(): String? =
        subscriptionOfferDetails
            ?.sortedBy { it.pricingPhases.pricingPhaseList.firstOrNull()?.priceAmountMicros ?: Long.MAX_VALUE }
            ?.firstOrNull()
            ?.offerToken

    fun getMonthlyPrice(): String =
        monthlyProduct?.subscriptionOfferDetails
            ?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.formattedPrice ?: "₹49"

    fun getAnnualPrice(): String =
        annualProduct?.subscriptionOfferDetails
            ?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.formattedPrice ?: "₹599"

    // ── Purchase ──────────────────────────────────────────────────────────────

    fun purchaseMonthly(activity: Activity) {
        if (BuildConfig.DEBUG) { simulatePurchase(); return }
        launchBillingFlow(activity, monthlyProduct)
    }
    fun purchaseAnnual(activity: Activity) {
        if (BuildConfig.DEBUG) { simulatePurchase(); return }
        launchBillingFlow(activity, annualProduct)
    }

    // Debug-only: simulates a successful purchase without Play Console
    private fun simulatePurchase() {
        _purchaseLoading.value = true
        _purchaseError.value   = null
        scope.launch {
            delay(900)                          // mimic network round-trip
            _tier.value            = SubscriptionTier.PRO
            _purchaseLoading.value = false
        }
    }

    private fun launchBillingFlow(activity: Activity, product: ProductDetails?) {
        if (product == null) {
            _purchaseError.value = "Product unavailable. Check your internet connection."
            return
        }
        val offerToken = product.baseOfferToken()
        if (offerToken == null) {
            _purchaseError.value = "No active offer found for this product."
            return
        }
        _purchaseLoading.value = true
        _purchaseError.value   = null

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build()
                )
            ).build()

        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseLoading.value = false
            _purchaseError.value = billingErrorMessage(result.responseCode)
        }
        // Loading stays true until onPurchasesUpdated fires
    }

    // ── PurchasesUpdatedListener ──────────────────────────────────────────────

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        _purchaseLoading.value = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // Silent — user dismissed the sheet
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // User already subscribed; refresh to surface the entitlement
                scope.launch { refreshEntitlements() }
            }
            else -> {
                _purchaseError.value = billingErrorMessage(result.responseCode)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                val isYatrainPro = purchase.products.any {
                    it == Constants.MONTHLY_PRODUCT_ID || it == Constants.ANNUAL_PRODUCT_ID
                }
                if (isYatrainPro) {
                    _tier.value = SubscriptionTier.PRO
                    acknowledgeIfNeeded(purchase)
                }
            }
            Purchase.PurchaseState.PENDING -> {
                // Payment pending (e.g., cash at kiosk) — keep Free until confirmed
                _purchaseError.value = "Payment pending. Your subscription will activate once payment is confirmed."
            }
            else -> Unit
        }
    }

    // ── Entitlements ──────────────────────────────────────────────────────────

    suspend fun refreshEntitlements() {
        if (!ensureConnected()) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = billingClient.queryPurchasesAsync(params)
        val hasPro = result.purchasesList.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
            purchase.products.any {
                it == Constants.MONTHLY_PRODUCT_ID || it == Constants.ANNUAL_PRODUCT_ID
            }
        }

        _tier.value = if (hasPro) SubscriptionTier.PRO else SubscriptionTier.FREE

        // Acknowledge any unacknowledged Pro purchases
        result.purchasesList
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { acknowledgeIfNeeded(it) }
    }

    // ── Acknowledgment ────────────────────────────────────────────────────────

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        scope.launch {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            withContext(Dispatchers.IO) {
                suspendCancellableCoroutine<Unit> { cont ->
                    billingClient.acknowledgePurchase(params) { _ ->
                        cont.resume(Unit)
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun clearError() { _purchaseError.value = null }

    private fun billingErrorMessage(code: Int): String = when (code) {
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE  -> "Play Store unavailable. Try again later."
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE  -> "Billing not available on this device."
        BillingClient.BillingResponseCode.DEVELOPER_ERROR      -> "Configuration error. Contact support."
        BillingClient.BillingResponseCode.ERROR                -> "Purchase failed. Please try again."
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "Subscriptions not supported on this device."
        BillingClient.BillingResponseCode.NETWORK_ERROR        -> "No internet connection."
        else -> "Something went wrong (code $code)."
    }
}
