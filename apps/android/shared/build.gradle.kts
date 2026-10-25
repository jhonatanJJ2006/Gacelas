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
            export(project(":contextos:carreras:presentation"))
            export(project(":contextos:rutas:presentation"))
            export(project(":contextos:cuenta:presentation"))
            export(project(":contextos:comunidad:presentation"))
        }
    }
    compilerOptions { optIn.add("kotlin.uuid.ExperimentalUuidApi") }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:comun"))
            api(project(":core:datos"))
            api(project(":core:ui"))
            api(project(":contextos:carreras:domain"))
            api(project(":contextos:carreras:data"))
            api(project(":contextos:carreras:presentation"))
            api(project(":contextos:rutas:domain"))
            api(project(":contextos:rutas:data"))
            api(project(":contextos:seguridad:domain"))
            api(project(":contextos:seguridad:data"))
            api(project(":contextos:rutas:presentation"))
            api(project(":contextos:cuenta:domain"))
            api(project(":contextos:cuenta:data"))
            api(project(":contextos:cuenta:presentation"))
            api(project(":contextos:comunidad:domain"))
            api(project(":contextos:comunidad:data"))
            api(project(":contextos:comunidad:presentation"))
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
