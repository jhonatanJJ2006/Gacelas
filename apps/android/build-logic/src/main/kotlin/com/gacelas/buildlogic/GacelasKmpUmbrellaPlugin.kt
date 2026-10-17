package com.gacelas.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

class GacelasKmpUmbrellaPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("org.jetbrains.compose")
            configureGacelasKmpAndroid("com.gacelas.shared")
            extensions.configure<KotlinMultiplatformExtension> {
                androidTarget {
                    compilerOptions {
                        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                    }
                }
                val xcf = XCFramework("Shared")
                val soloSimulador = providers.gradleProperty("gacelas.ios.soloSimulador").orNull == "true"
                val iosTargets =
                    if (soloSimulador) {
                        listOf(iosSimulatorArm64())
                    } else {
                        listOf(iosX64(), iosArm64(), iosSimulatorArm64())
                    }
                iosTargets.forEach {
                    it.binaries.framework {
                        baseName = "Shared"
                        isStatic = true
                        xcf.add(this)
                    }
                }
                compilerOptions { optIn.add("kotlin.uuid.ExperimentalUuidApi") }
            }
        }
    }
}
