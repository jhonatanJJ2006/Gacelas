# Reglas de Proguard para Runcity
-keep class com.gacelas.** { *; }
# Filament / SceneView usan JNI
-keep class com.google.android.filament.** { *; }

# Filament / SceneView usan JNI y reflexion.
-keep class com.google.android.filament.** { *; }
-keep class io.github.sceneview.** { *; }
# Modelos serializados en DataStore.
-keepclassmembers @kotlinx.serialization.Serializable class com.gacelas.** { *; }
-keep class com.gacelas.**$$serializer { *; }
-keepclassmembers class com.gacelas.** { *** Companion; kotlinx.serialization.KSerializer serializer(...); }
-dontwarn org.slf4j.**
# Supabase (supabase-kt) y Ktor: serializadores y motor HTTP que se cargan por reflexion.
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-keepclassmembers class io.github.jan.supabase.** { *** Companion; kotlinx.serialization.KSerializer serializer(...); }
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**
# Credential Manager con Google.
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
