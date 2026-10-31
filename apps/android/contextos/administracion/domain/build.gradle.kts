plugins {
    id("gacelas.kmp.dominio")
}

// Administracion: panel de administracion movil (docs/ADMIN.md, parte movil).
kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":contextos:moderacion:domain"))
    }
}
