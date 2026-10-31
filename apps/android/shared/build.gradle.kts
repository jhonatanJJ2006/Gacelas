import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    val xcf = XCFramework("Shared")
    val soloSimulador = providers.gradleProperty("gacelas.ios.soloSimulador").orNull == "true"
    val iosTargets =
        if (soloSimulador) listOf(iosSimulatorArm64()) else listOf(iosX64(), iosArm64(), iosSimulatorArm64())
    iosTargets.forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcf.add(this)
            export(project(":core:comun"))
            export(project(":core:datos"))
            export(project(":core:ui"))
            export(project(":contextos:actividad:presentation"))
            export(project(":contextos:rutas:presentation"))
            export(project(":contextos:identidad:presentation"))
            export(project(":contextos:comunidad:presentation"))
        }
    }
    compilerOptions { optIn.add("kotlin.uuid.ExperimentalUuidApi") }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:comun"))
            api(project(":core:datos"))
            api(project(":core:ui"))
            api(project(":contextos:actividad:domain"))
            api(project(":contextos:actividad:application"))
            api(project(":contextos:actividad:infrastructure"))
            api(project(":contextos:actividad:presentation"))
            api(project(":contextos:rutas:domain"))
            api(project(":contextos:rutas:infrastructure"))
            api(project(":contextos:seguridad:domain"))
            api(project(":contextos:seguridad:infrastructure"))
            api(project(":contextos:seguridad:application"))
            api(project(":contextos:seguridad:presentation"))
            api(project(":contextos:rutas:presentation"))
            api(project(":contextos:identidad:domain"))
            api(project(":contextos:identidad:infrastructure"))
            api(project(":contextos:identidad:presentation"))
            api(project(":contextos:comunidad:domain"))
            api(project(":contextos:comunidad:infrastructure"))
            api(project(":contextos:comunidad:presentation"))
            api(project(":contextos:analitica:domain"))
            api(project(":contextos:analitica:application"))
            api(project(":contextos:analitica:infrastructure"))
            api(project(":contextos:analitica:presentation"))
            api(project(":contextos:moderacion:domain"))
            api(project(":contextos:legal:domain"))
            api(project(":contextos:legal:infrastructure"))
            api(project(":contextos:envivo:domain"))
            api(project(":contextos:envivo:infrastructure"))
            api(project(":contextos:administracion:domain"))
            api(project(":contextos:administracion:application"))
            api(project(":contextos:administracion:infrastructure"))
            api(project(":contextos:administracion:presentation"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.compose.material.icons.extended)
            api(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.jetbrains.lifecycle.viewmodel)
            api(libs.jetbrains.lifecycle.viewmodel.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        iosMain.dependencies {
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
