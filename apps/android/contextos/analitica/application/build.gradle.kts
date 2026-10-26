plugins {
    id("gacelas.kmp.dominio")
}

// Casos de uso de Analitica y reportes (docs/ARQUITECTURA-DDD.md, 3.11).
kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":contextos:analitica:domain"))
    }
}
