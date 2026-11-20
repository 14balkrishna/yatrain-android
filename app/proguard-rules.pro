# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson / models
-keep class com.yatrain.app.models.** { *; }
-keepattributes *Annotation*

# Supabase / Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep class kotlinx.serialization.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# SLF4J — referenced by Ktor/OkHttp logging but not present at runtime on Android
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Ktor HTTP client — used by Supabase as its transport layer.
# The engine is registered via ServiceLoader (META-INF/services/io.ktor.client.HttpClientEngineContainer).
# R8 strips the service entry + engine class unless we keep them explicitly.
-keep class io.ktor.client.HttpClientEngineContainer { *; }
-keep class io.ktor.client.engine.android.** { *; }
-keep class io.ktor.** { *; }
-keepnames class io.ktor.** { *; }
-dontwarn io.ktor.**

# Supabase — plugin install() mechanism uses reflection internally
-keep class io.github.jan.supabase.** { *; }
-keepnames class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Google Play Billing
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.**

# Google Maps / Maps Compose
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }
-dontwarn com.google.maps.**
-dontwarn com.google.android.gms.maps.**

# Google ML Kit Translation
# ML Kit ships consumer rules in its AAR but keep explicitly to be safe
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_translate.** { *; }
-dontwarn com.google.mlkit.**

# kotlinx-coroutines-play-services — Task<T>.await() extension
-keep class kotlinx.coroutines.tasks.** { *; }
-dontwarn kotlinx.coroutines.tasks.**

# Google AdMob — ships its own consumer rules, but keep explicitly for safety
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
