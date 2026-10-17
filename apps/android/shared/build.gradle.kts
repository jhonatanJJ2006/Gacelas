import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Modulo compartido Android/iOS (Kotlin Multiplatform + Compose Multiplatform).
 * Etapa 1: logica pura (rumbo, formatos, reglas de actualizacion) y la UI de arranque de iOS.
 * Las siguientes etapas mueven aqui rutas, cronometro, Supabase y pantallas.
 */
kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    val xcf = XCFramework("Shared")
    // En CI (-Pgacelas.ios.soloSimulador=true) solo se declara iosSimulatorArm64: evita configurar
    // y compilar iosX64/iosArm64 cuando unicamente se prueba en el simulador Apple Silicon.
    val soloSimulador = providers.gradleProperty("gacelas.ios.soloSimulador").orNull == "true"
    val iosTargets = if (soloSimulador) listOf(iosSimulatorArm64()) else listOf(iosX64(), iosArm64(), iosSimulatorArm64())
    iosTargets.forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcf.add(this)
        }
    }

    compilerOptions { optIn.add("kotlin.uuid.ExperimentalUuidApi") }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            // MVVM compartido (ViewModel + viewModelScope en Android e iOS).
            api(libs.jetbrains.lifecycle.viewmodel)
            api(libs.jetbrains.lifecycle.viewmodel.compose)
            api(libs.androidx.datastore.preferences.core)
            api(libs.ktor.client.core)
            api(project.dependencies.platform(libs.supabase.bom))
            api(libs.supabase.auth)
            api(libs.supabase.postgrest)
            api(libs.supabase.storage)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            // DataStore en iOS (createWithPath) usa rutas okio.
            implementation("com.squareup.okio:okio:3.9.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.gacelas.shared"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
