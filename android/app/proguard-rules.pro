# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-if @kotlinx.serialization.Serializable class **
-keep, allowobfuscation, allowshrinking class <1>

# Retrofit
-keepattributes Signature, Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Models are referenced reflectively by the serializer only
-keep class com.taskweave.android.data.remote.dto.** { *; }
