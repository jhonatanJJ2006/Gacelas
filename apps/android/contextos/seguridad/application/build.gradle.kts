plugins {
    id("gacelas.kmp.dominio")
}

// Casos de uso de Seguridad de rutas (docs/ARQUITECTURA-DDD.md, 3.6).
kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":contextos:seguridad:domain"))
    }
}
