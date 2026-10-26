plugins {
    id("gacelas.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:seguridad:domain"))
        implementation(project(":contextos:seguridad:application"))
    }
}
