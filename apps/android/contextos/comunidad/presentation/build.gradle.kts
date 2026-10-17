plugins {
    id("gacelas.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:comunidad:domain"))
    }
}
