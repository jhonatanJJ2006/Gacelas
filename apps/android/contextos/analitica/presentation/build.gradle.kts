plugins {
    id("gacelas.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:analitica:domain"))
        implementation(project(":contextos:analitica:application"))
    }
}
