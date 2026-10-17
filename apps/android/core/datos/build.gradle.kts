plugins {
    id("gacelas.kmp.core-datos")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":contextos:cuenta:domain"))
        implementation(project(":contextos:carreras:domain"))
        implementation(project(":contextos:comunidad:domain"))
        implementation(project(":contextos:rutas:domain"))
    }
}
