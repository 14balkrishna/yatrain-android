import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Load secrets from secrets.properties (not committed to git)
val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties().apply {
    if (secretsFile.exists()) load(secretsFile.inputStream())
}

android {
    namespace = "com.yatrain.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yatrain.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 20
        versionName = "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject secrets as BuildConfig fields
        buildConfigField("String", "SUPABASE_URL",      "\"${secrets["SUPABASE_URL"] ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secrets["SUPABASE_ANON_KEY"] ?: ""}\"")
        buildConfigField("String", "RAPIDAPI_KEY",      "\"${secrets["RAPIDAPI_KEY"] ?: ""}\"")
        buildConfigField("String", "RAILRADAR_KEY",     "\"${secrets["RAILRADAR_KEY"] ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${secrets["GOOGLE_WEB_CLIENT_ID"] ?: ""}\"")

        buildConfigField("Boolean", "DEBUG_FORCE_PRO", "false")

        // AdMob — replace with real IDs in secrets.properties before release
        // Create separate banner units per screen in AdMob console for best tracking.
        buildConfigField("String", "ADMOB_APP_ID",    "\"${secrets["ADMOB_APP_ID"]    ?: "ca-app-pub-3940256099942544~3347511713"}\"")
        buildConfigField("String", "ADMOB_BANNER_1",  "\"${secrets["ADMOB_BANNER_1"]  ?: "ca-app-pub-3940256099942544/6300978111"}\"")
        buildConfigField("String", "ADMOB_BANNER_2",  "\"${secrets["ADMOB_BANNER_2"]  ?: "ca-app-pub-3940256099942544/6300978111"}\"")
        buildConfigField("String", "ADMOB_BANNER_3",  "\"${secrets["ADMOB_BANNER_3"]  ?: "ca-app-pub-3940256099942544/6300978111"}\"")

        manifestPlaceholders["appAuthRedirectScheme"] = "yatrain"
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = secrets["GOOGLE_MAPS_API_KEY"] ?: ""
        manifestPlaceholders["ADMOB_APP_ID"] = secrets["ADMOB_APP_ID"] ?: "ca-app-pub-3940256099942544~3347511713"
    }

    signingConfigs {
        create("release") {
            storeFile     = file(secrets["KEYSTORE_PATH"] ?: "yatrain-release.jks")
            storePassword = secrets["KEYSTORE_PASSWORD"] as String? ?: ""
            keyAlias      = secrets["KEY_ALIAS"] as String? ?: ""
            keyPassword   = secrets["KEY_PASSWORD"] as String? ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            buildConfigField("Boolean", "DEBUG_FORCE_PRO", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)

    // Supabase
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.client.android)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Google Play Billing (StoreKit 2 equivalent)
    implementation(libs.billing)

    // Google Maps
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // ML Kit on-device translation (free, offline after first model download)
    implementation(libs.mlkit.translate)
    implementation(libs.kotlinx.coroutines.play.services)

    // Google AdMob
    implementation(libs.play.services.ads)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
