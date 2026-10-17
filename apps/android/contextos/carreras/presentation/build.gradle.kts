plugins {
    id("gacelas.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:carreras:domain"))
        implementation(project(":core:datos"))
    }
}
