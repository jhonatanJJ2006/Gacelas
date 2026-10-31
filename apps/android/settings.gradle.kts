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

listOf("actividad", "rutas", "identidad", "comunidad", "seguridad").forEach { ctx ->
    includeModule(":contextos:$ctx:domain", "contextos/$ctx/domain")
    includeModule(":contextos:$ctx:infrastructure", "contextos/$ctx/infrastructure")
    includeModule(":contextos:$ctx:presentation", "contextos/$ctx/presentation")
}

// Contextos que por ahora solo tienen dominio (legal tiene ademas infrastructure).
listOf("analitica", "moderacion", "legal").forEach { ctx ->
    includeModule(":contextos:$ctx:domain", "contextos/$ctx/domain")
}

includeModule(":contextos:actividad:application", "contextos/actividad/application")
includeModule(":contextos:legal:infrastructure", "contextos/legal/infrastructure")
includeModule(":contextos:seguridad:application", "contextos/seguridad/application")
includeModule(":contextos:analitica:application", "contextos/analitica/application")
includeModule(":contextos:analitica:infrastructure", "contextos/analitica/infrastructure")
includeModule(":contextos:analitica:presentation", "contextos/analitica/presentation")
includeModule(":contextos:envivo:domain", "contextos/envivo/domain")
includeModule(":contextos:envivo:infrastructure", "contextos/envivo/infrastructure")

listOf("domain", "application", "infrastructure", "presentation").forEach { capa ->
    includeModule(":contextos:administracion:$capa", "contextos/administracion/$capa")
}

include(":shared")
