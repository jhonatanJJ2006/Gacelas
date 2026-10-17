plugins {
    id("gacelas.kmp.dominio")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:comun"))
        implementation(project(":contextos:cuenta:domain"))
        implementation(project(":contextos:carreras:domain"))
    }
}
