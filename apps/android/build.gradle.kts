plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.roborazzi) apply false
}

// Reglas de arquitectura (docs/adr/004-arquitectura-ddd.md). Modo aviso: lista las violaciones
// sin romper el build. Con -Pgacelas.arquitectura.estricta=true la tarea falla.
val violacionesArquitectura = mutableListOf<String>()
val capasPermitidas = mapOf(
    "domain" to setOf("domain"),
    "application" to setOf("domain", "application"),
    "infrastructure" to setOf("domain", "application", "infrastructure"),
    "presentation" to setOf("domain", "application", "presentation"),
)
gradle.projectsEvaluated {
    rootProject.subprojects.forEach { p ->
        val origen = p.path.removePrefix(":").split(":")
        if (origen.size != 3 || origen[0] != "contextos") return@forEach
        val (_, ctx, capa) = origen
        p.configurations.forEach { conf ->
            conf.dependencies.withType(ProjectDependency::class.java).forEach { d ->
                val destinoPath = d.dependencyProject.path
                val destino = destinoPath.removePrefix(":").split(":")
                val problema = when {
                    destino.size == 3 && destino[0] == "contextos" && destino[1] != ctx && destino[2] !in setOf("api", "domain") ->
                        "otro contexto solo por api o domain"
                    destino.size == 3 && destino[0] == "contextos" && destino[1] != ctx && destino[2] == "domain" ->
                        "dependencia de domain de otro contexto (migrar a api)"
                    destino.size == 3 && destino[0] == "contextos" && destino[1] == ctx &&
                        destino[2] !in (capasPermitidas[capa] ?: emptySet()) -> "capa no permitida"
                    capa == "domain" && destinoPath in setOf(":core:datos", ":core:ui") -> "domain no depende de datos ni ui"
                    else -> null
                }
                if (problema != null) violacionesArquitectura += "${p.path} -> $destinoPath (${conf.name}): $problema"
            }
        }
    }
}
tasks.register("verificarArquitectura") {
    group = "verification"
    description = "Lista dependencias entre modulos que rompen las reglas DDD"
    val lista = violacionesArquitectura
    val estricta = providers.gradleProperty("gacelas.arquitectura.estricta").map { it == "true" }.orElse(false)
    doLast {
        val unicas = lista.distinct().sorted()
        if (unicas.isEmpty()) {
            println("Arquitectura: sin violaciones")
        } else {
            println("Arquitectura: ${unicas.size} violaciones (modo aviso)")
            unicas.forEach { println("  $it") }
            if (estricta.get()) throw GradleException("Hay violaciones de arquitectura")
        }
    }
}
