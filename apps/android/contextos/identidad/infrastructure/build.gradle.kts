plugins {
    id("gacelas.kmp.datos")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:identidad:domain"))
    }
}
