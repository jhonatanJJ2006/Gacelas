# APK publicado (-Pgacelas.release=true): R8 solo quita los iconos de material-icons-extended que
# no se usan. Todo lo demas se conserva tal cual (sin ofuscar, sin optimizar, sin quitar clases),
# asi que la reflexion de Supabase, Ktor, serializacion, Filament, MapLibre y JNI no cambia.
# Ahorro medido en docs/REGRESIONES.md (APK 0.9.14).
-dontobfuscate
-dontoptimize
-ignorewarnings
-keepattributes *
-keep class !androidx.compose.material.icons.**,** { *; }
-keep interface !androidx.compose.material.icons.**,** { *; }
-keep enum !androidx.compose.material.icons.**,** { *; }
