package com.gacelas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.actividad.domain.reglas.Estadisticas
import com.gacelas.correr.CarreraMuestra
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureScreenRoboImage
import androidx.datastore.preferences.core.edit
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
 * Capturas de la direccion A "Tablero": todas las pantallas, hojas y modales numerados en
 * 393x873, y las principales tambien en 360x800 y con fuente al 130 %. Solo con -Pgacelas.shots=dir.
 *
 * Las pantallas se abren en el NavHost real con historial local sembrado (sin servidor). Inicio con
 * datos de servidor (clima por hora, seguridad del canton, corredores en vivo) se arma con el mismo
 * composable y valores de prueba inyectados aqui: la app nunca inventa esos datos.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class TableroQaTest(private val nombre: String, private val tamano: String) {
    @get:Rule val compose = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication() as GacelasApp

    private fun carreras(): List<Carrera> {
        val ahora = System.currentTimeMillis()
        val dia = 86_400_000L
        return listOf(0, 1, 3, 5, 8, 10, 13, 16, 20, 24, 27, 33, 40, 47).mapIndexed { i, d ->
            val km = 3.0 + (i % 4) * 1.6
            val seg = (km * (390 - i * 3 + (i % 3) * 12)).toInt()
            val puntos = (0..60).map { PuntoGps(-3.99 - it * 0.0004, -79.205 + sin(it / (5.0 + i)) * 0.0015, ahora - d * dia + it * 1000L, 5f, 2050.0 + sin(it / 7.0) * 20) }
            Carrera(id = "qa-$i", titulo = if (i % 2 == 0) "Carrera matutina" else "Rodaje por Jipiro", inicioMs = ahora - d * dia - 3_600_000L,
                segundos = seg, distanciaKm = km, desnivelM = 20 + i * 4, parciales = CarreraMuestra.guardada.parciales, puntos = puntos)
        }
    }

    private fun sembrar(ruta: String?) {
        runBlocking {
            carreras().forEach { app.contenedor.carreras.guardar(it) }
            // Ultima lista de rutas seguras recibida del servidor (cache offline real de RutaSugeridaRepository).
            app.contenedor.store.edit { it[androidx.datastore.preferences.core.stringPreferencesKey("ruta_sugerida")] = """{"historial":[],"seguras":["1","2","3"]}""" }
            app.contenedor.ajustes.actualizar {
                it.copy(objetivoTipo = "distancia", objetivoValor = 6000, compartirEnVivo = true, metaKmSemana = 25, pesoKg = 62.0,
                    contactosEmergencia = listOf(com.gacelas.identidad.domain.modelo.ContactoEmergencia("Mamá", "+593991234567"),
                        com.gacelas.identidad.domain.modelo.ContactoEmergencia("Andrés", "+593987654321")))
            }
        }
        if (ruta == Destinos.CARRERA_EN_CURSO || ruta == Destinos.RESUMEN) {
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
        val caso = CASOS.first { it.nombre == nombre }
        sembrar(caso.ruta)
        if (caso.ruta != null) compose.setContent { GacelasTheme { GacelasNavHost(inicio = Destinos.INICIO, rutaInicial = caso.ruta, sesionActiva = true) } }
        else compose.setContent { GacelasTheme { InicioConServidor(carreras()) } }
        compose.mainClock.advanceTimeBy(2500)
        compose.waitForIdle()
        caso.accion(compose)
        compose.mainClock.advanceTimeBy(1500)
        compose.waitForIdle()
        captureScreenRoboImage("$dir/$nombre-$tamano.png")
        if (tamano == "393x873") volcarClicables(File(dir, "clicables").apply { mkdirs() }.resolve("$nombre.tsv"))
    }

    /** Lista cada nodo pulsable (etiqueta, texto, descripcion y accion) para la auditoria de interacciones. */
    private fun volcarClicables(destino: File) {
        val P = androidx.compose.ui.semantics.SemanticsProperties
        val A = androidx.compose.ui.semantics.SemanticsActions
        val nodos = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("pulsable") {
            it.config.contains(A.OnClick) || it.config.contains(A.OnLongClick) || it.config.contains(A.SetText) || it.config.contains(A.SetProgress) || it.config.contains(P.ToggleableState)
        }, useUnmergedTree = false).fetchSemanticsNodes(atLeastOneRootRequired = false)
        destino.writeText(nodos.joinToString("\n") { n ->
            val c = n.config
            val tag = c.getOrElseNullable(P.TestTag) { null }.orEmpty()
            val texto = c.getOrElseNullable(P.Text) { null }?.joinToString(" ") { it.text }.orEmpty()
            val desc = c.getOrElseNullable(P.ContentDescription) { null }?.joinToString(" ").orEmpty()
            val etiqueta = c.getOrElseNullable(A.OnClick) { null }?.label.orEmpty()
            val tipo = when { c.contains(A.SetText) -> "texto"; c.contains(A.SetProgress) -> "deslizador"; c.contains(P.ToggleableState) -> "interruptor"; c.contains(A.OnLongClick) && !c.contains(A.OnClick) -> "pulsacion larga"; else -> "toque" }
            listOf(tag, texto, desc, etiqueta, tipo).joinToString("\t") { it.replace('\t', ' ').replace('\n', ' ') }
        })
    }

    class Caso(val nombre: String, val ruta: String?, val principal: Boolean = false, val accion: (ComposeContentTestRule) -> Unit = {})

    companion object {
        private fun ComposeContentTestRule.tocar(tag: String) { onNodeWithTag(tag, useUnmergedTree = true).performClick(); mainClock.advanceTimeBy(800); waitForIdle() }
        private fun ComposeContentTestRule.tocarTexto(t: String) { onNodeWithText(t, useUnmergedTree = true).performClick(); mainClock.advanceTimeBy(800); waitForIdle() }
        private fun ComposeContentTestRule.deslizar(tag: String, veces: Int = 1) { repeat(veces) { onNodeWithTag(tag).performTouchInput { swipeUp() }; mainClock.advanceTimeBy(600) }; waitForIdle() }

        val CASOS = listOf(
            Caso("01-inicio", null, principal = true),
            Caso("02-inicio-widgets", null) { it.deslizar("inicio-tablero", 2) },
            Caso("03-inicio-sin-servidor", Destinos.INICIO),
            Caso("04-correr", Destinos.CORRER, principal = true),
            Caso("05-correr-modo-libre", Destinos.CORRER) { it.tocar("modo-libre") },
            Caso("06-correr-hoja-objetivo", Destinos.CORRER) { it.tocar("correr-editar") },
            Caso("07-correr-reportar", Destinos.CORRER) { it.tocar("boton-reportar-incidente") },
            Caso("08-carrera", Destinos.CARRERA_EN_CURSO, principal = true),
            Caso("09-carrera-crono", Destinos.CARRERA_EN_CURSO) { it.tocar("boton-cronometro") },
            Caso("10-carrera-modal-pausa", Destinos.CARRERA_EN_CURSO) { it.tocar("boton-pausa") },
            Caso("11-carrera-modal-terminar", Destinos.CARRERA_EN_CURSO) { it.tocar("boton-stop") },
            Caso("12-carrera-bloqueo", Destinos.CARRERA_EN_CURSO) { it.tocar("boton-bloqueo") },
            Caso("13-carrera-sos", Destinos.CARRERA_EN_CURSO) {
                it.onNodeWithTag("boton-sos").performSemanticsAction(SemanticsActions.OnLongClick); it.mainClock.advanceTimeBy(1200); it.waitForIdle()
            },
            Caso("14-carrera-reportar", Destinos.CARRERA_EN_CURSO) { it.tocar("boton-reportar-incidente") },
            Caso("15-carrera-salir", Destinos.CARRERA_EN_CURSO) { it.tocar("boton-volver-mapa") },
            Caso("16-cronometro", Destinos.CRONOMETRO),
            Caso("17-resumen", Destinos.RESUMEN, principal = true),
            Caso("18-resumen-bento", Destinos.RESUMEN) { it.deslizar("pantalla-resumen", 2) },
            Caso("19-resumen-3d", Destinos.RESUMEN) { it.tocar("resumen-3d") },
            Caso("20-resumen-compartir", Destinos.RESUMEN) { it.tocar("boton-compartir") },
            Caso("21-rutas", Destinos.RUTAS, principal = true),
            Caso("22-rutas-filtros", Destinos.RUTAS) { it.tocar("boton-filtros") },
            Caso("23-rutas-lista", Destinos.RUTAS) { it.tocar("rutas-ver-lista") },
            Caso("24-rutas-reportar", Destinos.RUTAS) { it.tocar("boton-reportar-incidente") },
            Caso("25-detalle-ruta", Destinos.detalleRuta("1"), principal = true),
            Caso("26-detalle-ruta-compartir", Destinos.detalleRuta("1")) { it.tocar("detalle-compartir") },
            Caso("27-buscador-rutas", Destinos.BUSCADOR_RUTAS),
            Caso("28-crear-ruta", Destinos.CREAR_RUTA),
            Caso("29-comunidad", Destinos.COMUNIDAD, principal = true),
            Caso("30-en-vivo", Destinos.EN_VIVO),
            Caso("31-mapa-vivo", Destinos.MAPA_VIVO),
            Caso("32-clubes", Destinos.CLUBES, principal = true),
            Caso("33-eventos", Destinos.EVENTOS, principal = true),
            Caso("34-eventos-crear", Destinos.EVENTOS) { c -> c.onNodeWithContentDescription("Crear evento").performClick(); c.mainClock.advanceTimeBy(800); c.waitForIdle() },
            Caso("35-notificaciones", Destinos.NOTIFICACIONES),
            Caso("36-perfil", Destinos.PERFIL, principal = true),
            Caso("37-detalle-carrera", Destinos.detalleCarrera("qa-0")),
            Caso("38-mis-reportes", Destinos.MIS_REPORTES),
            Caso("39-editar-perfil", Destinos.EDITAR_PERFIL),
            Caso("40-ajustes", Destinos.AJUSTES, principal = true),
            Caso("41-privacidad", Destinos.PRIVACIDAD),
            Caso("42-config-notificaciones", Destinos.CONFIG_NOTIFICACIONES),
            Caso("43-mapas-offline", Destinos.MAPAS_OFFLINE),
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun casos(): List<Array<Any>> = CASOS.flatMap { c ->
            (if (c.principal) listOf("393x873", "360x800", "393x873-fuente130") else listOf("393x873")).map { arrayOf<Any>(c.nombre, it) }
        }
    }
}

/** Inicio con datos de servidor de prueba (solo para la captura): mismo composable que usa la app. */
@androidx.compose.runtime.Composable
private fun InicioConServidor(carreras: List<Carrera>) {
    val ahora = System.currentTimeMillis()
    val trazos = com.gacelas.rutas.infrastructure.TrazosRutas.puntos
    val rutas = com.gacelas.data.mock.MockData.rutas.map { r -> r.copy(puntos = trazos[r.id].orEmpty()) }
    val ruta = rutas.first { it.id == "2" }
    com.gacelas.ui.components.ScaffoldPrincipal(Destinos.INICIO, 2, {}) {
        com.gacelas.inicio.InicioTablero(
            semana = Estadisticas.semana(carreras, ahora), hoy = Estadisticas.diaSemana(ahora), m = true,
            racha = com.gacelas.actividad.domain.reglas.Rachas.calcular(carreras, ahora, 3),
            mapa = {
                com.gacelas.ui.components.MapaEsquemaC2(ruta.puntos, Modifier.fillMaxSize(),
                    otros = rutas.filter { it.id != ruta.id && it.puntos.size > 4 }.take(2).map { it.puntos },
                    yo = ruta.puntos[ruta.puntos.size / 3], marcadores = listOf(ruta.puntos[ruta.puntos.size * 2 / 3]), margenArriba = 110.dp, margenAbajo = 110.dp)
            },
            onCorrer = {}, onBuscar = {}, onNotificaciones = {}, onProgreso = {}, onMapaVivo = {}, onComunidad = {}, onEvento = {}, onRutas = {}, onRuta = {},
            sinLeer = 2, metaKm = 25,
            kmPorDia = DoubleArray(7).also { km -> carreras.filter { it.inicioMs >= Estadisticas.inicioSemana(ahora) }.forEach { km[Estadisticas.diaSemana(it.inicioMs)] += it.distanciaKm } }.toList(),
            clima = com.gacelas.clima.domain.ClimaActual(14, "parcialmente nublado", "Loja", ahora,
                listOf(6 to 1, 7 to 2, 8 to 3, 9 to 61, 10 to 3).mapIndexed { i, (h, c) -> com.gacelas.clima.domain.HoraClima(h, 13 + i, c) }),
            ritmoSemanas = com.gacelas.inicio.ritmoPorSemana(carreras, ahora),
            evento = com.gacelas.data.mock.MockData.eventos.first(),
            rutaDelDia = ruta, seguridadRuta = "riesgo bajo",
            zona = com.gacelas.seguridad.domain.ResumenZona("2026-09", listOf("Tránsito y Movilidad" to 41, "Seguridad Ciudadana" to 12), 2),
            corriendo = 8,
        )
    }
}
