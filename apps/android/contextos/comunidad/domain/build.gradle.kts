plugins {
    id("gacelas.kmp.dominio")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:comun"))
        implementation(project(":core:datos"))
    }
}
