package com.gacelas

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowToast
import java.io.File

/**
 * Auditoria de botones (docs/QA-TOTAL.md): abre cada pantalla del grafo, guarda su captura y toca
 * cada elemento clicable habilitado. Un toque "sin efecto" no cambia la pantalla, no abre modal,
 * no muestra aviso y no lanza otra app. Solo corre con -Pgacelas.shots=dir (lenta, fuera de CI);
 * el informe queda en dir/auditoria-botones.txt.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h873dp-xxhdpi")
class AuditoriaBotonesTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController

    private fun estado(): String {
        val raices = compose.onAllNodes(isRoot(), useUnmergedTree = true)
        val n = raices.fetchSemanticsNodes().size
        val texto = (0 until n).joinToString("\n") { runCatching { raices[it].printToString(Int.MAX_VALUE) }.getOrDefault("") }
        return "${nav.currentBackStackEntry?.destination?.route}|${nav.currentBackStackEntry?.arguments}|$n|" + texto.replace(Regex("\\d"), "#")
    }

    private fun abrir(ruta: String) {
        compose.runOnIdle { nav.navigate(ruta) { popUpTo(Destinos.INICIO) { inclusive = false }; launchSingleTop = false } }
        compose.mainClock.advanceTimeBy(1500)
        compose.waitForIdle()
    }

    @Test fun auditar() {
        val dir = System.getProperty("gacelas.shots")
        assumeTrue("Solo con -Pgacelas.shots", dir != null)
        File(dir!!).mkdirs()
        compose.setContent { nav = rememberNavController(); GacelasTheme { GacelasNavHost(nav, inicio = Destinos.INICIO, sesionActiva = true) } }
        compose.waitForIdle()
        val app = shadowOf(RuntimeEnvironment.getApplication())
        val informe = StringBuilder()
        val rutas = (listOf(Destinos.INICIO) + Destinos.todas + listOf(Destinos.MIS_REPORTES, Destinos.DETALLE_CARRERA, Destinos.ADMIN))
            .filter { it !in Destinos.acceso }.distinct()
        rutas.forEach { plantilla ->
            val ruta = plantilla.replace("{id}", "1")
            runCatching { abrir(ruta) }.onFailure { informe.appendLine("$ruta | no abre | ${it.message}"); return@forEach }
            runCatching { compose.onAllNodes(isRoot())[0].captureRoboImage("$dir/pantalla-${plantilla.replace(Regex("[^a-z0-9]+"), "_")}.png") }
            val total = compose.onAllNodes(hasClickAction() and isEnabled()).fetchSemanticsNodes().size
            var sinEfecto = 0
            for (i in 0 until minOf(total, 40)) {
                abrir(ruta)
                val nodos = compose.onAllNodes(hasClickAction() and isEnabled())
                val lista = nodos.fetchSemanticsNodes()
                if (i >= lista.size) break
                val nodo = lista[i]
                val nombre = (nodo.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
                    ?: nodo.config.getOrNull(SemanticsProperties.Text)?.joinToString()
                    ?: nodo.config.getOrNull(SemanticsProperties.TestTag)
                    ?: compose.onAllNodes(hasClickAction() and isEnabled(), useUnmergedTree = false)[i].printToString().lines().firstOrNull { "Text" in it || "ContentDescription" in it }?.trim()
                    ?: "(sin nombre)").take(60)
                runCatching { nodos[i].performScrollTo() }
                compose.waitForIdle()
                val antes = estado()
                while (app.nextStartedActivity != null) { }
                ShadowToast.reset()
                val avisoAntes = com.gacelas.ui.components.AvisosC2.id
                val toque = runCatching { nodos[i].performClick() }
                if (toque.isFailure) { informe.appendLine("$ruta | $nombre | error al tocar: ${toque.exceptionOrNull()?.message?.take(80)}"); continue }
                compose.mainClock.advanceTimeBy(800)
                compose.waitForIdle()
                // Los ajustes se guardan en DataStore (hilo de IO real): se espera a que vuelvan a la UI.
                repeat(3) { Thread.sleep(150); compose.waitForIdle() }
                val lanzo = app.nextStartedActivity != null || ShadowToast.shownToastCount() > 0 || com.gacelas.ui.components.AvisosC2.id != avisoAntes
                if (!lanzo && estado() == antes) { sinEfecto++; informe.appendLine("$ruta | $nombre | sin efecto") }
            }
            informe.appendLine("# $ruta: $total clicables, $sinEfecto sin efecto")
        }
        File(dir, "auditoria-botones.txt").writeText(informe.toString())
    }
}
