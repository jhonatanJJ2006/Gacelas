plugins {
    id("gacelas.kmp.dominio")
}

// Moderacion: denuncias sobre cualquier objetivo registrado (docs/ARQUITECTURA-DDD.md, 3.10).
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:comun"))
    }
}
