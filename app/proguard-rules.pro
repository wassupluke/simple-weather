# ──────────────────────────────────────────────
# kotlinx.serialization
# The plugin generates $$serializer classes; keep them so R8 doesn't strip
# the companion objects that Retrofit's converter needs at runtime.
# ──────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.wassupluke.simpleweather.**$$serializer { *; }
-keepclassmembers class com.wassupluke.simpleweather.** {
    *** Companion;
}
-keepclasseswithmembers class com.wassupluke.simpleweather.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ──────────────────────────────────────────────
# Retrofit
# Keep annotated interface methods; R8 cannot see calls made via dynamic proxy.
# ──────────────────────────────────────────────
-keepattributes Signature, Exceptions
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# ──────────────────────────────────────────────
# WorkManager
# Worker subclasses are instantiated by name via reflection.
# ──────────────────────────────────────────────
-keep class com.wassupluke.simpleweather.worker.WeatherFetchWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ──────────────────────────────────────────────
# OkHttp / Okio
# These classes are optional on some platforms; suppress R8 warnings.
# ──────────────────────────────────────────────
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
