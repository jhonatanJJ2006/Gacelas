# Reglas de Proguard para Gacelas
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
