plugins {
    id("gacelas.kmp.core-ui")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:actividad:domain"))
    }
}
