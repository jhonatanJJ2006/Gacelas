package com.gacelas.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class GacelasKmpCoreUiPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            pluginManager.apply("org.jetbrains.compose")
            configureGacelasKmpAndroid("com.gacelas.core.ui")
            configureGacelasKmpTargets()
            val libs = gacelasLibs()
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.configure {
                    dependencies {
                        implementation(project(":core:comun"))
                        implementation(libs.findLibrary("compose-runtime").get())
                        implementation(libs.findLibrary("compose-foundation").get())
                        implementation(libs.findLibrary("compose-material3").get())
                        implementation(libs.findLibrary("compose-ui").get())
                        implementation(libs.findLibrary("compose-material-icons-extended").get())
                        implementation(libs.findLibrary("kotlinx-serialization-json").get())
                    }
                }
                sourceSets.androidMain.configure {
                    dependencies {
                        implementation(libs.findLibrary("androidx-activity-compose").get())
                    }
                }
            }
            configureGacelasKmpTestDeps()
        }
    }
}
