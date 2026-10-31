package com.gacelas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.gacelas.comunidad.domain.modelo.BorradorClub
import com.gacelas.comunidad.domain.modelo.Club
import com.gacelas.comunidad.domain.modelo.MiembroClub
import com.gacelas.comunidad.domain.modelo.NivelGrupo
import com.gacelas.comunidad.domain.modelo.RolClub
import com.gacelas.comunidad.domain.modelo.TipoClub
import com.gacelas.comunidad.domain.repositorio.ClubesRepository
import com.gacelas.comunidad.presentation.ClubesViewModel
import com.gacelas.comunidad.presentation.DetalleClubViewModel
import com.gacelas.comunidad.presentation.ui.ClubesContenido
import com.gacelas.comunidad.presentation.ui.DetalleClubContenido
import com.gacelas.comunidad.presentation.ui.FormularioClubContenido
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas de grupos y comunidades (lista, registro y detalle) en diseño C2. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ComunidadesCapturasTest {
    @get:Rule val compose = createComposeRule()

    private val runners = Club("c1", "Runners Loja", "La comunidad abierta de corredores de Loja", "Loja", false, 128, tipo = TipoClub.comunidad, contacto = "@runnersloja")
    private val utpl = Club("c2", "UTPL Running", "Comunidad universitaria", "Loja", true, 46, tipo = TipoClub.comunidad)
    private val jipiro = Club("g1", "Gacelas del Jipiro", "Fondos suaves los sábados", "Loja", false, 23, comunidadId = "c1", lugarEncuentro = "Parque Jipiro",
        dias = listOf(2, 6), hora = "06:00", nivel = NivelGrupo.principiante, ritmoSegKm = 390, cupo = 30)
    private val villonaco = Club("g2", "Trail Villonaco", "Subidas al Villonaco", "Loja", true, 9, comunidadId = "c1", lugarEncuentro = "Redondel de la Argelia",
        dias = listOf(4, 7), hora = "05:30", nivel = NivelGrupo.avanzado, ritmoSegKm = 360, cupo = 15)
    private val todos = listOf(runners, utpl, jipiro, villonaco)

    private val repo = object : ClubesRepository {
        override suspend fun listar() = todos to setOf("g1")
        override suspend fun miId() = "u-1"
        override suspend fun detalle(id: String) = todos.firstOrNull { it.id == id }
        override suspend fun miembros(id: String) = listOf(
            MiembroClub("u-2", "María López", RolClub.owner, true), MiembroClub("u-1", "Jhona Runner", RolClub.member, true),
            MiembroClub("u-3", "Carlos Ruiz", RolClub.member, true),
        )
    }

    private fun dir() = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    private fun captura(nombre: String, contenido: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { GacelasTheme { contenido() } }
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("${dir()}/comunidades/android-$nombre.png")
    }

    @Test fun listaGrupos() = captura("lista-grupos") { ClubesContenido(ClubesViewModel(repo), true, {}, {}, {}) }
    @Test fun listaComunidades() = captura("lista-comunidades") { ClubesContenido(ClubesViewModel(repo), true, {}, {}, {}, tipoInicial = TipoClub.comunidad) }
    @Test fun listaVacia() = captura("lista-vacia") { ClubesContenido(ClubesViewModel(object : ClubesRepository { override suspend fun listar() = emptyList<Club>() to emptySet<String>() }), true, {}, {}, {}) }
    @Test fun listaVaciaComunidades() = captura("lista-vacia-comunidades") { ClubesContenido(ClubesViewModel(object : ClubesRepository { override suspend fun listar() = emptyList<Club>() to emptySet<String>() }), true, {}, {}, {}, tipoInicial = TipoClub.comunidad) }
    @Test fun listaError() = captura("lista-error") { ClubesContenido(ClubesViewModel(object : ClubesRepository { override suspend fun listar(): Pair<List<Club>, Set<String>> = error("sin red") }), true, {}, {}, {}) }
    @Test fun listaCargando() = captura("lista-cargando") { ClubesContenido(ClubesViewModel(object : ClubesRepository { override suspend fun listar(): Pair<List<Club>, Set<String>> = kotlinx.coroutines.awaitCancellation() }), true, {}, {}, {}) }
    @Test fun registrarGrupo() = captura("registrar-grupo") {
        FormularioClubContenido("Registrar grupo", BorradorClub(nombre = "Madrugadores Zamora Huayco", lugarEncuentro = "Parque Lineal La Tebaida", dias = listOf(1, 3), hora = "05:45", ritmo = "6:15", nivel = NivelGrupo.intermedio, comunidadId = "c1"),
            listOf(runners, utpl), true, null, {}, {})
    }
    @Test fun registrarComunidad() = captura("registrar-comunidad") {
        FormularioClubContenido("Registrar comunidad", BorradorClub(tipo = TipoClub.comunidad, nombre = "Corredores del Sur", conAprobacion = true), emptyList(), true, null, {}, {})
    }
    @Test fun detalleGrupo() = captura("detalle-grupo") { DetalleClubContenido("g1", DetalleClubViewModel("g1", repo), true, {}, {}, {}, {}, onReportar = {}) }
    @Test fun detalleComunidad() = captura("detalle-comunidad") { DetalleClubContenido("c1", DetalleClubViewModel("c1", repo), true, {}, {}, {}, {}, onReportar = {}) }
}
