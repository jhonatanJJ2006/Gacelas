plugins {
    id("gacelas.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:comunidad:domain"))
        implementation(project(":contextos:cuenta:domain"))
        implementation(project(":contextos:carreras:domain"))
        implementation(project(":contextos:carreras:presentation"))
        implementation(project(":core:datos"))
    }
}
