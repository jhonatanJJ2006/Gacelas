package com.gacelas.buildlogic

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.gacelasAndroidNamespace(): String {
    val p = path.removePrefix(":")
    return when {
        p.startsWith("contextos:") -> {
            val parts = p.split(":")
            "com.gacelas.${parts[1]}.${parts[2]}"
        }
        p.startsWith("core:") -> "com.gacelas.core.${p.removePrefix("core:")}"
        else -> "com.gacelas.${p.replace(':', '.')}"
    }
}

internal fun Project.configureGacelasKmpAndroid(namespace: String) {
    pluginManager.apply("com.android.library")
    extensions.configure<LibraryExtension> {
        this.namespace = namespace
        compileSdk = 35
        defaultConfig { minSdk = 24 }
        compileOptions {
            sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
            targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
        }
    }
}

internal fun Project.configureGacelasKmpTargets() {
    extensions.configure<KotlinMultiplatformExtension> {
        androidTarget {
            compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        }
        val soloSimulador = providers.gradleProperty("gacelas.ios.soloSimulador").orNull == "true"
        val iosTargets =
            if (soloSimulador) {
                listOf(iosSimulatorArm64())
            } else {
                listOf(iosX64(), iosArm64(), iosSimulatorArm64())
            }
        iosTargets.forEach { /* compilacion iOS sin framework en modulos hijos */ }
        compilerOptions { optIn.add("kotlin.uuid.ExperimentalUuidApi") }
    }
}

internal fun Project.gacelasLibs(): VersionCatalog =
    extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureGacelasKmpTestDeps() {
    val libs = gacelasLibs()
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.named("commonTest").configure {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.findLibrary("kotlinx-coroutines-test").get())
            }
        }
    }
}
