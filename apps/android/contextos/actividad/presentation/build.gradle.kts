plugins {
    id("gacelas.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:actividad:domain"))
        implementation(project(":contextos:actividad:application"))
        implementation(project(":core:datos"))
    }
}
