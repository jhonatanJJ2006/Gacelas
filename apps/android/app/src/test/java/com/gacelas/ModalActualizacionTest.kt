package com.gacelas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.gacelas.actualizacion.ContenidoModal
import com.gacelas.actualizacion.Fase
import com.gacelas.actualizacion.textoBytes
import com.gacelas.data.actualizacion.InfoVersion
import com.gacelas.data.jsonGacelas
import com.gacelas.ui.theme.GacelasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class ModalActualizacionTest {
    @get:Rule val compose = createComposeRule()

    private val info = InfoVersion(605, "0.6.5", "x", "https://example.com/a.apk",
        items = listOf("Nuevo diseño de bienvenida y login", "Mapa de Loja sin conexión en el acceso", "Tocar fuera cierra el teclado"), apkBytes = 49_283_072)

    private fun mostrar(fase: Fase, obligatoria: Boolean = false, alto: Int? = null) = compose.setContent {
        GacelasTheme {
            val m = if (alto != null) Modifier.size(360.dp, alto.dp) else Modifier
            Box(m) { ContenidoModal("0.6.4", info, obligatoria, fase, info.apkBytes, {}, {}, {}) }
        }
    }

    @Test fun normalMuestraVersionesNovedadesYTamano() {
        mostrar(Fase.Info)
        compose.onNodeWithText("0.6.4").assertIsDisplayed()
        compose.onNodeWithText("v0.6.5").assertIsDisplayed()
        compose.onNodeWithText("47 MB").assertIsDisplayed()
        info.items!!.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithTag("mas-tarde").assertIsDisplayed()
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
    }

    @Test fun descargandoMuestraProgresoReal() {
        mostrar(Fase.Descargando(62, 30_408_704, 49_283_072))
        compose.onNodeWithText("62%").assertIsDisplayed()
        compose.onNodeWithText("29 de 47 MB · no cierres la app").assertIsDisplayed()
        compose.onAllNodes(hasTestTag("mas-tarde")).assertCountEquals(0)
    }

    @Test fun obligatoriaSinMasTarde() {
        mostrar(Fase.Info, obligatoria = true)
        compose.onNodeWithTag("badge-obligatoria").assertIsDisplayed()
        compose.onNodeWithText("Esta versión es necesaria para seguir usando Gacelas.").assertIsDisplayed()
        compose.onAllNodes(hasTestTag("mas-tarde")).assertCountEquals(0)
    }

    @Test fun cabeEnPocoAltoSinScroll() {
        mostrar(Fase.Info, alto = 420)
        compose.onNodeWithTag("boton-actualizar").assertIsDisplayed()
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
    }

    @Test fun manifiestoViejoSinItemsSigueFuncionando() {
        val viejo = jsonGacelas.decodeFromString(InfoVersion.serializer(),
            """{"versionCode":604,"versionName":"0.6.4","notes":"Detalle de cada carrera desde tu perfil, distancias de rutas iguales a su trazo, el corredor 3D pisa la línea.","apkUrl":"https://x/a.apk","sha256":"ab"}""")
        assertNull(viejo.items); assertNull(viejo.apkBytes)
        assertEquals(listOf("Detalle de cada carrera desde tu perfil", "Distancias de rutas iguales a su trazo", "El corredor 3D pisa la línea"), viejo.novedades())
        assertEquals(listOf("Uno", "Dos"), InfoVersion(1, "1", "Uno; dos", "https://x").novedades())
        assertEquals(listOf("Uno", "Dos"), InfoVersion(1, "1", "Uno.\nDos.", "https://x").novedades())
    }

    @Test fun textoDeBytes() {
        assertEquals("0 de 47 MB · no cierres la app", textoBytes(0, 49_283_072))
    }
}
