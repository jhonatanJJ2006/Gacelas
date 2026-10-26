plugins {
    id("gacelas.kmp.datos")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":contextos:analitica:domain"))
        implementation(project(":contextos:actividad:domain"))
    }
}
