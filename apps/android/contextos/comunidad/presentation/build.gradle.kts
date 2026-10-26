plugins {
    id("gacelas.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:comunidad:domain"))
        implementation(project(":contextos:identidad:domain"))
        implementation(project(":contextos:actividad:domain"))
        implementation(project(":contextos:actividad:presentation"))
        implementation(project(":core:datos"))
    }
}
