# SnapReceipt - Release ProGuard/R8 rules

# Gson: keep generic type signatures for reflection
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.Unsafe
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# Keep data models used by Gson (API DTOs and domain entities if serialized)
-keep class com.snapreceipt.io.domain.model.** { *; }
-keep class com.snapreceipt.io.data.network.model.** { *; }
-keep class com.skybound.space.core.network.** { *; }

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep native methods
-keepclasseswithmembers class * {
    native <methods>;
}
