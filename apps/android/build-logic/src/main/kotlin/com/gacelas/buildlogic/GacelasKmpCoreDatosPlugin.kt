package com.gacelas.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class GacelasKmpCoreDatosPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            configureGacelasKmpAndroid("com.gacelas.core.datos")
            configureGacelasKmpTargets()
            val libs = gacelasLibs()
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.configure {
                    dependencies {
                        implementation(project(":core:comun"))
                        implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                        implementation(libs.findLibrary("kotlinx-serialization-json").get())
                        implementation(libs.findLibrary("androidx-datastore-preferences-core").get())
                        implementation(project.dependencies.platform(libs.findLibrary("supabase-bom").get()))
                        implementation(libs.findLibrary("supabase-auth").get())
                        implementation(libs.findLibrary("supabase-postgrest").get())
                        implementation(libs.findLibrary("supabase-storage").get())
                        implementation(libs.findLibrary("ktor-client-core").get())
                    }
                }
                sourceSets.androidMain.configure {
                    dependencies {
                        implementation(libs.findLibrary("ktor-client-okhttp").get())
                    }
                }
                sourceSets.iosMain.configure {
                    dependencies {
                        implementation(libs.findLibrary("ktor-client-darwin").get())
                        implementation("com.squareup.okio:okio:3.9.1")
                    }
                }
            }
            configureGacelasKmpTestDeps()
        }
    }
}
