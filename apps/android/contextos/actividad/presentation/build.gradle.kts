plugins {
    id("gacelas.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:actividad:domain"))
        implementation(project(":core:datos"))
    }
}
