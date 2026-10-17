package com.gacelas.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class GacelasKmpComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("org.jetbrains.compose")
            configureGacelasKmpAndroid(gacelasAndroidNamespace())
            configureGacelasKmpTargets()
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.configure {
                    dependencies {
                        implementation(project(":core:comun"))
                        implementation(project(":core:ui"))
                        implementation(libs.findLibrary("kotlinx-datetime").get())
                        implementation(libs.findLibrary("compose-runtime").get())
                        implementation(libs.findLibrary("compose-foundation").get())
                        implementation(libs.findLibrary("compose-material3").get())
                        implementation(libs.findLibrary("compose-ui").get())
                        implementation(libs.findLibrary("compose-material-icons-extended").get())
                        implementation(libs.findLibrary("jetbrains-lifecycle-viewmodel").get())
                        implementation(libs.findLibrary("jetbrains-lifecycle-viewmodel-compose").get())
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
