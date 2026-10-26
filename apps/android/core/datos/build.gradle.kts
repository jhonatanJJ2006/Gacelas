plugins {
    id("gacelas.kmp.core-datos")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:identidad:domain"))
        implementation(project(":contextos:actividad:domain"))
        implementation(project(":contextos:comunidad:domain"))
        implementation(project(":contextos:rutas:domain"))
    }
}
