# NutriAI ProGuard Rules

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes used with Retrofit/serialization
-keep class com.app.nutriai.data.remote.** { *; }
-keep class com.app.nutriai.domain.model.** { *; }

# Keep Room entities
-keep class com.app.nutriai.data.local.db.entity.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
