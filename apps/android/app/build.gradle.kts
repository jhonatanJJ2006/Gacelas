import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(name: String): String = localProperties.getProperty(name, "")

// Sin estas claves el APK queda sin servidor: sin Google, sin sincronizacion ni datos de la cuenta.
// Pasó en v0.6.0 (compilado sin SUPABASE_URL ni GOOGLE_WEB_CLIENT_ID). Ver docs/SUPABASE.md.
listOf("SUPABASE_URL", "SUPABASE_ANON_KEY", "GOOGLE_WEB_CLIENT_ID").filter { localProp(it).isBlank() }.takeIf { it.isNotEmpty() }?.let { faltan ->
    val msg = "local.properties no tiene ${faltan.joinToString()}: el APK no podra iniciar sesion con Google ni conectarse a Supabase."
    if (project.findProperty("gacelas.release") == "true") throw GradleException("$msg No se puede publicar asi.")
    logger.warn("ADVERTENCIA: $msg")
}

android {
    namespace = "com.gacelas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gacelas"
        minSdk = 24
        targetSdk = 35
        versionName = "0.6.6"
        // versionCode sale de versionName (0.6.4 -> 604): siempre sube con cada version y el aviso
        // de actualizacion lo compara con latest.json. Hasta 0.6.1 fue siempre 1.
        versionCode = versionName!!.split('.', '-').take(3).map { it.toIntOrNull() ?: 0 }.plus(listOf(0, 0, 0)).let { it[0] * 10000 + it[1] * 100 + it[2] }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "SUPABASE_URL", "\"${localProp("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProp("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProp("GOOGLE_WEB_CLIENT_ID")}\"")
        // Instagram exige un Facebook App ID en las historias compartidas (vacio = se usa el paquete).
        buildConfigField("String", "FACEBOOK_APP_ID", "\"${localProp("FACEBOOK_APP_ID")}\"")
    }

    // APK mas liviano para un solo tipo de CPU: ./gradlew assembleDebug -Pgacelas.abi=arm64-v8a
    (project.findProperty("gacelas.abi") as String?)?.let { abi ->
        defaultConfig { ndk { abiFilters += abi.split(",") } }
    }

    buildTypes {
        // Con -Pgacelas.minify=true el debug tambien pasa por R8 (quita iconos y clases sin uso).
        debug {
            if (project.findProperty("gacelas.minify") == "true") {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Los modelos GLB no se deben comprimir para que Filament los lea directo desde assets.
    androidResources { noCompress += "glb" }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.maxHeapSize = "2g"
                (project.findProperty("gacelas.shots") as String?)?.let { dir -> it.systemProperty("gacelas.shots", dir) }
            }
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.sceneview)
    implementation(libs.play.services.location)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)
    implementation(libs.maplibre.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
