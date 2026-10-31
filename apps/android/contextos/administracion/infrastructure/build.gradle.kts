plugins {
    id("gacelas.kmp.datos")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":contextos:administracion:domain"))
    }
}
