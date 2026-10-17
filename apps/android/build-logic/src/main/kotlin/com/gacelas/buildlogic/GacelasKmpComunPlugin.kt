package com.gacelas.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Modulo core:comun (utilidades puras, sin dependencias de contexto). */
class GacelasKmpComunPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            configureGacelasKmpAndroid("com.gacelas.core.comun")
            configureGacelasKmpTargets()
            val libs = gacelasLibs()
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.configure {
                    dependencies {
                        implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                        implementation(libs.findLibrary("kotlinx-datetime").get())
                        implementation(libs.findLibrary("kotlinx-serialization-json").get())
                        implementation(libs.findLibrary("ktor-client-core").get())
                    }
                }
            }
            configureGacelasKmpTestDeps()
        }
    }
}
