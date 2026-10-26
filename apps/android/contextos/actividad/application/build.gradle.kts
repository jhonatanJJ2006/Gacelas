plugins {
    id("gacelas.kmp.dominio")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":contextos:actividad:domain"))
        implementation(project(":contextos:identidad:domain"))
    }
}
