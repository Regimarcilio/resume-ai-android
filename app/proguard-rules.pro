# ProGuard / R8 rules - release build
#
# Without these the release APK crashes at runtime even though it compiles:
#   "TypeToken must be created with a type argument ... make sure that
#    generic signatures are preserved."
# (Gson reflects over generic types at runtime; R8 strips the signatures.)

# ---------------------------------------------------------------------------
# Keep generic signatures and annotations - required by Gson/TypeToken,
# Room and kotlinx.serialization.
# ---------------------------------------------------------------------------
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations, AnnotationDefault

# ---------------------------------------------------------------------------
# Gson
# ---------------------------------------------------------------------------
# Domain models are (de)serialized by Gson via reflective TypeToken.
-keep class com.resumeapp.domain.model.** { *; }

# TypeToken subclasses must keep their type parameter AND their Signature.
# allowshrinking is deliberately omitted: R8 would then be free to shrink the
# anonymous TypeToken subclasses away and strip the generic signature,
# crashing Gson at runtime.
-keep,allowobfuscation class * extends com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken { <init>(...); }

# Enum values used by Gson adapters.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
-keep class com.resumeapp.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# kotlinx.serialization
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Hilt / Dagger
# ---------------------------------------------------------------------------
# Hilt ships its own consumer rules; keep the generated components intact.
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# ---------------------------------------------------------------------------
# Compose
# ---------------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ---------------------------------------------------------------------------
# PDFBox
# ---------------------------------------------------------------------------
# PDFBox is a desktop library and references java.awt.* / javax.imageio.*,
# which do not exist on Android. Those code paths are never reached at
# runtime, so the references are simply ignored.
-dontwarn org.apache.pdfbox.**
-dontwarn org.apache.fontbox.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn java.beans.**
-dontwarn java.sql.**
-keep class org.apache.pdfbox.** { *; }
-keep class org.apache.fontbox.** { *; }

# ---------------------------------------------------------------------------
# OkHttp / Retrofit / Moshi
# ---------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
