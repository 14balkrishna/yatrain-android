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
        versionCode = 5
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject secrets as BuildConfig fields
        buildConfigField("String", "SUPABASE_URL",      "\"${secrets["SUPABASE_URL"] ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secrets["SUPABASE_ANON_KEY"] ?: ""}\"")
        buildConfigField("String", "RAPIDAPI_KEY",      "\"${secrets["RAPIDAPI_KEY"] ?: ""}\"")
        buildConfigField("String", "RAILRADAR_KEY",     "\"${secrets["RAILRADAR_KEY"] ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${secrets["GOOGLE_WEB_CLIENT_ID"] ?: ""}\"")

        buildConfigField("Boolean", "DEBUG_FORCE_PRO", "false")

        manifestPlaceholders["appAuthRedirectScheme"] = "yatrain"
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = secrets["GOOGLE_MAPS_API_KEY"] ?: ""
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

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
