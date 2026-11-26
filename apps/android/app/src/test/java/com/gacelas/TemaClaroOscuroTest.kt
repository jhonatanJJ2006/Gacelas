package com.gacelas

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.gacelas.actividad.domain.reglas.Estadisticas
import com.gacelas.actividad.domain.reglas.EstadisticasPerfil
import com.gacelas.actividad.domain.reglas.Racha
import com.gacelas.administracion.domain.AccesoAdmin
import com.gacelas.administracion.domain.RolAdmin
import com.gacelas.administracion.domain.SeccionAdmin
import com.gacelas.administracion.infrastructure.AdminRepositorioMemoria
import com.gacelas.administracion.presentation.PanelAdminContenido
import com.gacelas.ajustes.AjustesContenido
import com.gacelas.correr.CarreraMuestra
import com.gacelas.data.mock.MockData
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.identidad.domain.modelo.ModoTema
import com.gacelas.inicio.InicioContenido
import com.gacelas.inicio.rangoSemana
import com.gacelas.perfil.Contadores
import com.gacelas.perfil.EstadisticasHistoricas
import com.gacelas.perfil.KilometrajeMensual
import com.gacelas.perfil.Medallero
import com.gacelas.perfil.Records
import com.gacelas.rutas.ListaRutasContenido
import com.gacelas.rutas.domain.modelo.FiltrosRuta
import com.gacelas.rutas.domain.modelo.PestanaRutas
import com.gacelas.ui.components.Pantalla
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.gacelas.actividad.domain.EstadoCarrera
import com.gacelas.actividad.domain.FaseCarrera
import com.gacelas.actividad.domain.modelo.Parcial
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.infrastructure.LocalEventosRepository
import com.gacelas.comunidad.presentation.EventosViewModel
import com.gacelas.comunidad.presentation.ui.EventosContenido
import com.gacelas.correr.ContenidoCarreraC4
import com.gacelas.correr.TrazadoRuta
import com.gacelas.correr.VistaCarrera
import com.gacelas.ui.components.ChipVidrio
import com.gacelas.ui.components.DockTablero
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.rules.TemporaryFolder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tema claro y oscuro de Android: la regla de resolucion de ModoTema, el selector de Ajustes y capturas
 * de las pantallas principales en los dos modos (sin emulador: Robolectric + Roborazzi). Las capturas
 * van a gacelas.shots (por defecto build/outputs/roborazzi/tema).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h2000dp-xxhdpi")
class TemaClaroOscuroTest {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
    private val ahora = CarreraMuestra.guardada.inicioMs + 3_600_000L
    private val carreras = listOf(CarreraMuestra.guardada)

    @get:Rule val carpeta = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("p.preferences_pb") } }

    @After fun restaurar() { GColors.claro = false }

    @Test fun modoTemaResuelveClaroOscuroYSistema() {
        assertTrue(ModoTema.Claro.esClaro(sistemaOscuro = true))
        assertFalse(ModoTema.Oscuro.esClaro(sistemaOscuro = false))
        assertTrue(ModoTema.Sistema.esClaro(sistemaOscuro = false))
        assertFalse(ModoTema.Sistema.esClaro(sistemaOscuro = true))
        assertEquals(ModoTema.Oscuro, Ajustes().tema)
    }

    @Test fun losTokensCambianConElModo() {
        GColors.claro = false
        val oscuro = listOf(GColors.TextPrimary, GColors.Surface, GColors.C2Violeta)
        GColors.claro = true
        val claro = listOf(GColors.TextPrimary, GColors.Surface, GColors.C2Violeta)
        assertTrue(oscuro.zip(claro).all { (o, c) -> o != c })
        // El relleno de marca no cambia: es el color de los botones en los dos modos.
        assertEquals(GColors.Primary, GColors.Primary)
    }

    @Test fun selectorDeAjustesGuardaElTemaElegido() {
        var guardado: Ajustes? = null
        compose.setContent {
            GacelasTheme {
                AjustesContenido(Ajustes(), "1.0.0", { f -> guardado = f(Ajustes()) }, {}, {}, {}, {}, {}, {}, mapaOffline = {}, cabecera = { Text("Jhona") })
            }
        }
        compose.onNodeWithTag("tema-claro").performScrollTo().performClick()
        assertEquals(ModoTema.Claro, guardado?.tema)
        compose.onNodeWithTag("tema-sistema").performScrollTo().performClick()
        assertEquals(ModoTema.Sistema, guardado?.tema)
    }

    private fun capturar(claro: Boolean, nombre: String, contenido: @androidx.compose.runtime.Composable () -> Unit) {
        GColors.claro = claro
        compose.setContent { GacelasTheme { contenido() } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$dir/tema/$nombre-${if (claro) "claro" else "oscuro"}.png")
    }


    @Test fun capturaInicioClaro() = capturar(true, "inicio", { inicio() })
    @Test fun capturaInicioOscuro() = capturar(false, "inicio", { inicio() })
    @Test fun capturaRutasClaro() = capturar(true, "rutas", { rutas() })
    @Test fun capturaRutasOscuro() = capturar(false, "rutas", { rutas() })
    @Test fun capturaPerfilClaro() = capturar(true, "perfil", { perfil() })
    @Test fun capturaPerfilOscuro() = capturar(false, "perfil", { perfil() })
    @Test fun capturaAjustesClaro() = capturar(true, "ajustes", { ajustes() })
    @Test fun capturaAjustesOscuro() = capturar(false, "ajustes", { ajustes() })
    @Test fun capturaAdminClaro() = capturar(true, "admin-menu") { admin() }
    @Test fun capturaAdminOscuro() = capturar(false, "admin-menu") { admin() }
    @Test fun capturaAdminEstadisticasClaro() = capturar(true, "admin-estadisticas") { admin(SeccionAdmin.Estadisticas) }
    @Test fun capturaAdminEstadisticasOscuro() = capturar(false, "admin-estadisticas") { admin(SeccionAdmin.Estadisticas) }

    @Test fun capturaCarreraClaro() = capturar(true, "carrera", { carrera() })
    @Test fun capturaCarreraOscuro() = capturar(false, "carrera", { carrera() })
    @Test fun capturaEventosClaro() = capturar(true, "eventos", { eventos() })
    @Test fun capturaEventosOscuro() = capturar(false, "eventos", { eventos() })
    @Test fun capturaDockClaro() = capturar(true, "dock", { dock() })
    @Test fun capturaDockOscuro() = capturar(false, "dock", { dock() })

    @androidx.compose.runtime.Composable private fun dock() = Column(Modifier.fillMaxSize().background(com.gacelas.ui.components.Tablero.Fondo)) {
        DockTablero("inicio", {})
        ChipVidrio("Filtros", {}, activo = true)
        ChipVidrio("Cerca de mí", {}, punto = GColors.C2Menta)
    }

    @androidx.compose.runtime.Composable private fun carrera(): Unit {
        val e = EstadoCarrera(
            fase = FaseCarrera.Corriendo, segundos = 892, distanciaKm = 2.34, desnivelM = 18.0, ultimaPrecisionM = 9f, ritmoActualSegKm = 381,
            puntos = (0..40).map { PuntoGps(-4.0 + it * 0.0005, -79.2 + Math.sin(it / 6.0) * 0.001, it * 1000L, 5f) },
            parciales = listOf(Parcial(1, 1.0, 378), Parcial(2, 1.0, 385), Parcial(1, 0.34, 129, manual = true)),
        )
        ContenidoCarreraC4(VistaCarrera(e), onPausa = {}, onStop = {}, onVuelta = {}, onAtras = {}, panelInicial = true) { _, controles, _ ->
            Box(Modifier.fillMaxSize().background(GColors.MapaFondo)) {
                TrazadoRuta(e.puntos, Modifier.fillMaxSize())
                controles({}, {}, 0f, true)
            }
        }
    }

    @androidx.compose.runtime.Composable private fun eventos(): Unit {
        val vm = EventosViewModel(LocalEventosRepository(store))
        val lista = listOf(
            Evento("e1", "Vuelta al Parque Jipiro", "", "sábado 25 de octubre", "06:30", "Torre Eiffel, Parque Jipiro", 42, 10, inicioMs = System.currentTimeMillis() + 86_400_000L * 4, cupo = 80),
            Evento("e2", "Fondo Zamora Huayco 15K", "", "domingo 26 de octubre", "06:00", "Puente Zamora Huayco", 28, 15, inicioMs = System.currentTimeMillis() + 86_400_000L * 5, cupo = 50, tipo = "entrenamiento"),
        )
        EventosContenido(vm, lista, emptySet(), true, null, {}, {}, {})
    }

    @androidx.compose.runtime.Composable private fun inicio() = InicioContenido(
        saludo = "Buenos días", nombre = "Jhona", semana = Estadisticas.semana(carreras, ahora), hoy = Estadisticas.diaSemana(ahora), m = true,
        racha = Racha(), evento = MockData.eventos.first(), onCorrer = {}, onEventos = {}, onEvento = {}, corredor3D = { Box(Modifier) },
        totales = EstadisticasPerfil.totalesSemana(carreras, ahora), meta = 3, rango = rangoSemana(ahora),
        reto = EstadisticasPerfil.retoMes(carreras, ahora), amigos = emptyList(),
    )

    @androidx.compose.runtime.Composable private fun rutas() = ListaRutasContenido(
        MockData.rutas, PestanaRutas.Explorar, {}, emptySet(), "", {}, FiltrosRuta(), {}, null, {}, true, true, {}, {}, {}, { _, _ -> },
        mapaRutas = {},
    )

    @androidx.compose.runtime.Composable private fun perfil() = Pantalla {
        Contadores(carreras.size, 0, 0)
        EstadisticasHistoricas(EstadisticasPerfil.historico(carreras), true)
        KilometrajePlaceholder()
        Records(EstadisticasPerfil.records(carreras))
        Medallero(EstadisticasPerfil.medallas(carreras))
    }

    @androidx.compose.runtime.Composable private fun KilometrajePlaceholder() = KilometrajeMensual(EstadisticasPerfil.kmMensual(carreras, ahora), true)

    @androidx.compose.runtime.Composable private fun ajustes() =
        AjustesContenido(Ajustes(), "1.0.0", {}, {}, {}, {}, {}, {}, {}, mapaOffline = {}, cabecera = { Text("Jhona") }, onAdmin = {})

    @androidx.compose.runtime.Composable private fun admin(seccion: SeccionAdmin? = null) =
        PanelAdminContenido(AccesoAdmin(RolAdmin.Admin), AdminRepositorioMemoria(RolAdmin.Admin), onAtras = {}, inicial = seccion)
}
