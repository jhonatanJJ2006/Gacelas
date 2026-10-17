package com.gacelas.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class GacelasKmpDominioPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            configureGacelasKmpAndroid(gacelasAndroidNamespace())
            configureGacelasKmpTargets()
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.configure {
                    dependencies {
                        implementation(project(":core:comun"))
                        implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                        implementation(libs.findLibrary("kotlinx-datetime").get())
                        implementation(libs.findLibrary("kotlinx-serialization-json").get())
                    }
                }
            }
            configureGacelasKmpTestDeps()
        }
    }
}
