plugins {
    `kotlin-dsl`
}

group = "com.gacelas.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("gacelasKmpComun") {
            id = "gacelas.kmp.comun"
            implementationClass = "com.gacelas.buildlogic.GacelasKmpComunPlugin"
        }
        register("gacelasKmpCoreDatos") {
            id = "gacelas.kmp.core-datos"
            implementationClass = "com.gacelas.buildlogic.GacelasKmpCoreDatosPlugin"
        }
        register("gacelasKmpCoreUi") {
            id = "gacelas.kmp.core-ui"
            implementationClass = "com.gacelas.buildlogic.GacelasKmpCoreUiPlugin"
        }
        register("gacelasKmpDominio") {
            id = "gacelas.kmp.dominio"
            implementationClass = "com.gacelas.buildlogic.GacelasKmpDominioPlugin"
        }
        register("gacelasKmpDatos") {
            id = "gacelas.kmp.datos"
            implementationClass = "com.gacelas.buildlogic.GacelasKmpDatosPlugin"
        }
        register("gacelasKmpCompose") {
            id = "gacelas.kmp.compose"
            implementationClass = "com.gacelas.buildlogic.GacelasKmpComposePlugin"
        }
        register("gacelasKmpUmbrella") {
            id = "gacelas.kmp.umbrella"
            implementationClass = "com.gacelas.buildlogic.GacelasKmpUmbrellaPlugin"
        }
    }
}
