package com.gacelas

import androidx.compose.ui.test.junit4.createComposeRule
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.correr.CarreraMuestra
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.sin

/**
 * QA del rediseno movil: cada pantalla principal en 393x873, 360x800 y fuente 130 %, con historial
 * local sembrado (carreras de las ultimas semanas). Solo corre con -Pgacelas.shots=dir.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RedisenoMovilQaTest(private val pantalla: String, private val ruta: String, private val tamano: String) {
    @get:Rule val compose = createComposeRule()

    private fun sembrar() {
        val app = RuntimeEnvironment.getApplication() as GacelasApp
        val ahora = System.currentTimeMillis()
        val dia = 86_400_000L
        runBlocking {
            listOf(0, 1, 3, 5, 8, 10, 13, 16, 20, 24, 27).forEachIndexed { i, d ->
                val km = 3.0 + (i % 4) * 1.6
                val seg = (km * (360 + (i % 3) * 15)).toInt()
                val puntos = (0..60).map { PuntoGps(-3.99 - it * 0.0004, -79.205 + sin(it / (5.0 + i)) * 0.0015, ahora - d * dia + it * 1000L, 5f, 2050.0 + sin(it / 7.0) * 20) }
                app.contenedor.carreras.guardar(
                    Carrera(id = "qa-$i", titulo = if (i % 2 == 0) "Carrera matutina" else "Rodaje por Jipiro", inicioMs = ahora - d * dia - 3_600_000L,
                        segundos = seg, distanciaKm = km, desnivelM = 20 + i * 4, parciales = CarreraMuestra.guardada.parciales, puntos = puntos),
                )
            }
        }
        runBlocking {
            app.contenedor.ajustes.actualizar {
                it.copy(objetivoTipo = "distancia", objetivoValor = 6000, compartirEnVivo = true,
                    contactosEmergencia = listOf(com.gacelas.identidad.domain.modelo.ContactoEmergencia("Mamá", "+593991234567")))
            }
        }
        if (ruta == Destinos.CARRERA_EN_CURSO || ruta == Destinos.RESUMEN) {
            // Estado fijo de la carrera de muestra (2,34 km en 14:52), con el reloj detenido para que no cambie.
            val t = app.contenedor.tracker
            val ahoraMs = System.currentTimeMillis()
            val base = CarreraMuestra.estado.copy(
                inicioMs = ahoraMs - 892_000L,
                fase = if (ruta == Destinos.RESUMEN) com.gacelas.actividad.domain.FaseCarrera.Terminada else com.gacelas.actividad.domain.FaseCarrera.Corriendo,
                puntos = CarreraMuestra.estado.puntos.mapIndexed { i, p -> p.copy(altitudM = 2050.0 + sin(i / 5.0) * 12) },
            )
            val campo = t.javaClass.getDeclaredField("_estado").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            (campo.get(t) as kotlinx.coroutines.flow.MutableStateFlow<com.gacelas.actividad.domain.EstadoCarrera>).value = base
            t.javaClass.getDeclaredField("acumuladoMs").apply { isAccessible = true }.setLong(t, 892_000L)
        }
    }

    @Test fun captura() {
        val dir = System.getProperty("gacelas.shots")
        assumeTrue("Solo con -Pgacelas.shots", dir != null)
        File(dir!!).mkdirs()
        when (tamano) {
            "393x873" -> RuntimeEnvironment.setQualifiers("w393dp-h873dp-xxhdpi")
            "360x800" -> RuntimeEnvironment.setQualifiers("w360dp-h800dp-xxhdpi")
            else -> { RuntimeEnvironment.setQualifiers("w393dp-h873dp-xxhdpi"); RuntimeEnvironment.setFontScale(1.3f) }
        }
        sembrar()
        compose.setContent { GacelasTheme { GacelasNavHost(inicio = Destinos.INICIO, rutaInicial = ruta, sesionActiva = true) } }
        compose.mainClock.advanceTimeBy(2500)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1500)
        captureScreenRoboImage("$dir/$pantalla-$tamano.png")
    }

    companion object {
        private val pantallas = listOf(
            "inicio" to Destinos.INICIO, "correr" to Destinos.CORRER, "carrera" to Destinos.CARRERA_EN_CURSO, "resumen" to Destinos.RESUMEN,
            "rutas" to Destinos.RUTAS, "detalle-ruta" to Destinos.detalleRuta("1"), "clubes" to Destinos.CLUBES, "comunidad" to Destinos.COMUNIDAD,
            "eventos" to Destinos.EVENTOS, "perfil" to Destinos.PERFIL, "ajustes" to Destinos.AJUSTES,
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{2}")
        fun casos(): List<Array<Any>> = pantallas.flatMap { (n, r) -> listOf("393x873", "360x800", "393x873-fuente130").map { arrayOf<Any>(n, r, it) } }
    }
}
