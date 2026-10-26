plugins {
    id("gacelas.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:identidad:domain"))
        implementation(project(":core:datos"))
        implementation(project(":contextos:rutas:domain"))
        implementation(project(":contextos:rutas:infrastructure"))
    }
}
