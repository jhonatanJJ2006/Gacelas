plugins {
    id("gacelas.kmp.dominio")
}

// Casos de uso del panel de administracion movil (docs/ADMIN.md).
kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":contextos:administracion:domain"))
    }
}
