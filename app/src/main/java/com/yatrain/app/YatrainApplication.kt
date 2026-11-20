package com.yatrain.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.functions.Functions

class YatrainApplication : Application() {

    companion object {
        lateinit var supabase: io.github.jan.supabase.SupabaseClient
            private set
    }

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Functions)
        }
    }
}
