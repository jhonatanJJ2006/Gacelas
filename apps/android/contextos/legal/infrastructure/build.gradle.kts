plugins {
    id("gacelas.kmp.datos")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":contextos:legal:domain"))
    }
}
