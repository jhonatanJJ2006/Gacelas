# Proguard rules for Gacelas
-keep class com.gacelas.** { *; }

# Supabase
-keep class io.github.jan.supabase.** { *; }

# Ktor
-keep class io.ktor.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.gacelas.**$$serializer { *; }
-keepclassmembers class com.gacelas.** {
    *** Companion;
}
-keepclasseswithmembers class com.gacelas.** {
    kotlinx.serialization.KSerializer serializer(...);
}
