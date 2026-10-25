pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Gacelas"

fun includeModule(path: String, dir: String) {
    include(path)
    project(path).projectDir = file(dir)
}

include(":app")
includeModule(":core:comun", "core/comun")
includeModule(":core:datos", "core/datos")
includeModule(":core:ui", "core/ui")

listOf("carreras", "rutas", "cuenta", "comunidad").forEach { ctx ->
    includeModule(":contextos:$ctx:domain", "contextos/$ctx/domain")
    includeModule(":contextos:$ctx:data", "contextos/$ctx/data")
    includeModule(":contextos:$ctx:presentation", "contextos/$ctx/presentation")
}

// Contextos que por ahora solo tienen dominio.
listOf("analitica", "moderacion").forEach { ctx ->
    includeModule(":contextos:$ctx:domain", "contextos/$ctx/domain")
}

include(":shared")
