plugins {
    id("gacelas.kmp.dominio")
}

// Analitica y reportes: modelo de lectura sobre las carreras (docs/ARQUITECTURA-DDD.md, 3.11).
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:comun"))
        api(project(":contextos:carreras:domain"))
    }
}
