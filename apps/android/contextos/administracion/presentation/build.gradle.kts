plugins {
    id("gacelas.kmp.compose")
}

// Panel de administracion movil, compartido por Android e iOS (docs/ADMIN.md).
kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":contextos:administracion:domain"))
        api(project(":contextos:administracion:application"))
        implementation(project(":contextos:administracion:infrastructure"))
        implementation(libs.kotlinx.datetime)
    }
}
