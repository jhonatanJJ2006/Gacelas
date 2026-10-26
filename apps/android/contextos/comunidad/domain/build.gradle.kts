plugins {
    id("gacelas.kmp.dominio")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:comun"))
        implementation(project(":contextos:identidad:domain"))
        implementation(project(":contextos:actividad:domain"))
    }
}
