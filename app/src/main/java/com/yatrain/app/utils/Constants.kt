package com.yatrain.app.utils

object Constants {
    // Deep link scheme
    const val DEEP_LINK_SCHEME = "yatrain"

    // Supabase
    const val OAUTH_REDIRECT = "yatrain://auth-callback"

    // Google Play — one subscription product with two base plans
    const val SUBSCRIPTION_PRODUCT_ID  = "pro.monthly"
    const val MONTHLY_BASE_PLAN_ID     = "monthly-base"
    const val ANNUAL_BASE_PLAN_ID      = "yatrain-base-yearly"

    // Legal URLs
    const val PRIVACY_URL = "https://yatrain.com/privacy.html"
    const val TERMS_URL   = "https://yatrain.com/terms.html"
    const val SUPPORT_URL = "https://yatrain.com/yatrain-support.html"
    const val CONTACT_EMAIL = "balkrishnap@me.com"

    // API
    const val TRAIN_DATA_HOST = "irctc-indian-railway-pnr-status.p.rapidapi.com"
    const val RAILRADAR_BASE_URL = "https://railradar.trainman.in/"
}
