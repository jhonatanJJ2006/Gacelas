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
            pluginManager.apply("org.jetbrains.compose")
            configureGacelasKmpAndroid("com.gacelas.core.ui")
            configureGacelasKmpTargets()
            val libs = gacelasLibs()
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.configure {
                    dependencies {
                        implementation(project(":core:comun"))
                        implementation(compose.runtime)
                        implementation(compose.foundation)
                        implementation(compose.material3)
                        implementation(compose.ui)
                        implementation(compose.materialIconsExtended)
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
