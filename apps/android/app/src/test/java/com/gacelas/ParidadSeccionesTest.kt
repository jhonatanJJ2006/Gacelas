package com.gacelas

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.gacelas.carreras.domain.reglas.Estadisticas
import com.gacelas.carreras.domain.reglas.EstadisticasPerfil
import com.gacelas.carreras.domain.reglas.Racha
import com.gacelas.correr.CarreraMuestra
import com.gacelas.data.mock.MockData
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
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.gacelas.ajustes.AjustesContenido
import com.gacelas.comunidad.data.LocalEventosRepository
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.modelo.Publicacion
import com.gacelas.comunidad.domain.modelo.ComentarioFeed
import com.gacelas.comunidad.domain.repositorio.FeedRepository
import com.gacelas.comunidad.presentation.EventosViewModel
import com.gacelas.comunidad.presentation.FeedViewModel
import com.gacelas.comunidad.presentation.ui.EventosContenido
import com.gacelas.comunidad.presentation.ui.FeedContenido
import com.gacelas.carreras.data.LocalCarrerasRepository
import com.gacelas.cuenta.data.LocalAjustesRepository
import com.gacelas.cuenta.data.LocalAuthRepository
import com.gacelas.cuenta.domain.modelo.Ajustes
import androidx.compose.material3.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas de las secciones de Stitch (Inicio, Perfil, Rutas) para comparar con la web movil. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h2400dp-xxhdpi")
class ParidadSeccionesTest {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
    private val ahora = CarreraMuestra.guardada.inicioMs + 3_600_000L
    private val carreras = listOf(CarreraMuestra.guardada)

    @Test fun inicio() {
        compose.setContent {
            GacelasTheme {
                InicioContenido(
                    saludo = "Buenos días", nombre = "Jhona", semana = Estadisticas.semana(carreras, ahora), hoy = Estadisticas.diaSemana(ahora), m = true,
                    racha = Racha(), evento = MockData.eventos.first(), onCorrer = {}, onEventos = {}, onEvento = {}, corredor3D = { Box(Modifier) },
                    totales = EstadisticasPerfil.totalesSemana(carreras, ahora), meta = 3, rango = rangoSemana(ahora),
                    reto = EstadisticasPerfil.retoMes(carreras, ahora), amigos = emptyList(),
                )
            }
        }
        compose.onRoot().captureRoboImage("$dir/paridad/inicio-secciones.png")
    }

    @Test fun perfil() {
        compose.setContent {
            GacelasTheme {
                Pantalla {
                    Contadores(carreras.size, 0, 0)
                    EstadisticasHistoricas(EstadisticasPerfil.historico(carreras), true)
                    KilometrajeMensual(EstadisticasPerfil.kmMensual(carreras, ahora), true)
                    Records(EstadisticasPerfil.records(carreras))
                    Medallero(EstadisticasPerfil.medallas(carreras))
                }
            }
        }
        compose.onRoot().captureRoboImage("$dir/paridad/perfil-secciones.png")
    }

    @Test fun rutas() {
        compose.setContent {
            GacelasTheme {
                ListaRutasContenido(
                    MockData.rutas, PestanaRutas.Explorar, {}, emptySet(), "", {}, FiltrosRuta(), {}, null, {}, true, true, {}, {}, {}, { _, _ -> },
                    mapaRutas = {},
                )
            }
        }
        compose.onRoot().captureRoboImage("$dir/paridad/rutas-lista.png")
    }

    @get:Rule val carpeta = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("p.preferences_pb") } }

    @Test fun ajustes() {
        compose.setContent {
            GacelasTheme {
                AjustesContenido(Ajustes(), "1.0.0", {}, {}, {}, {}, {}, {}, {}, mapaOffline = {}, cabecera = { Text("Jhona • Gacela Intermedia • Sede Loja • 42 km totales") })
            }
        }
        compose.onRoot().captureRoboImage("$dir/paridad/ajustes.png")
    }

    @Test fun eventos() {
        val vm = EventosViewModel(LocalEventosRepository(store))
        val lista = listOf(
            Evento("e1", "Vuelta al Parque Jipiro", "", "sábado 25 de octubre", "06:30", "Torre Eiffel, Parque Jipiro", 42, 10, inicioMs = System.currentTimeMillis() + 86_400_000L * 4, cupo = 80),
            Evento("e2", "Fondo Zamora Huayco 15K", "", "domingo 26 de octubre", "06:00", "Puente Zamora Huayco", 28, 15, inicioMs = System.currentTimeMillis() + 86_400_000L * 5, cupo = 50, tipo = "entrenamiento"),
            Evento("e3", "Carrera 10K Fiestas de Loja", "", "martes 18 de noviembre", "07:00", "Puerta de la Ciudad", 120, 10, inicioMs = System.currentTimeMillis() + 86_400_000L * 28, cupo = 200, tipo = "carrera"),
        )
        compose.setContent { GacelasTheme { EventosContenido(vm, lista, emptySet(), true, null, {}, {}, {}) } }
        compose.onRoot().captureRoboImage("$dir/paridad/eventos.png")
    }

    @Test fun feed() {
        val pub = Publicacion("p1", "María López", false, 1, "hace 2 h", "Fondo Jipiro", 8.4, 2580, 307, 12, 3,
            puntos = CarreraMuestra.guardada.puntos, remota = true, desnivelM = 42, compartidos = 2, autorKm = 180.0,
            ultimoComentario = "Carlos" to "Qué buen ritmo en la subida")
        val repo = object : FeedRepository {
            override suspend fun publicaciones() = listOf(pub)
            override suspend fun alternarMeGusta(id: String, activo: Boolean) {}
            override suspend fun comentarios(id: String) = emptyList<ComentarioFeed>()
            override suspend fun comentar(id: String, texto: String) {}
            override suspend fun siguiendo() = emptySet<String>()
            override suspend fun alternarSeguir(id: String) = emptySet<String>()
        }
        val vm = FeedViewModel(LocalCarrerasRepository(store), LocalAuthRepository(store), LocalAjustesRepository(store), emptyList(), repo, intervaloMs = 0)
        vm.refrescar()
        repeat(200) { if (vm.publicaciones.value.none { it.id == "p1" }) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(20) } }
        println("feed publicaciones=" + vm.publicaciones.value.map { it.id })
        compose.setContent { GacelasTheme { FeedContenido(vm, {}, {}, {}, {}, eventoClub = MockData.eventos.first()) } }
        compose.onRoot().captureRoboImage("$dir/paridad/feed.png")
    }
}
