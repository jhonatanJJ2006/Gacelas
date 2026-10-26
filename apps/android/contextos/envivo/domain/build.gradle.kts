plugins {
    id("gacelas.kmp.dominio")
}

// En vivo: posiciones de corredores en el mapa (docs/ARQUITECTURA-DDD.md, 3.5).
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:comun"))
    }
}
