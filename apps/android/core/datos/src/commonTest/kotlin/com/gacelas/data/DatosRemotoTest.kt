package com.gacelas.data

import com.gacelas.data.actualizacion.Aviso
import com.gacelas.data.actualizacion.InfoVersion
import com.gacelas.data.actualizacion.ReglasActualizacion
import com.gacelas.data.remoto.FechaIso
import com.gacelas.data.remoto.esColumnaInexistente
import com.gacelas.data.remoto.traducirError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DatosRemotoTest {
    @Test fun fechasIso() {
        val ms = FechaIso.leer("2026-10-10 10:00:00.123+00")!!
        assertEquals("2026-10-10T10:00:00Z", FechaIso.formatear(ms))
        assertEquals(ms, FechaIso.leer("2026-10-10T05:00:00-05"))
    }

    @Test fun actualizacionYErrores() {
        val info = InfoVersion(versionCode = 700, versionName = "0.7.0", apkUrl = "https://x/a.apk")
        assertIs<Aviso.Nueva>(ReglasActualizacion.aviso(609, info, null))
        assertTrue(ReglasActualizacion.sinCache("https://a/b.json", 5).contains("t=5"))
        assertEquals("Sin conexión con el servidor", traducirError("timeout"))
        assertEquals(
            "Tu cuenta está suspendida. No puedes publicar ni modificar datos.",
            traducirError("cuenta suspendida"),
        )
    }

    @Test fun registroMuestraElMotivoRealEnEspanol() {
        assertEquals("El correo no es válido o el servidor no lo acepta. Usa un correo real", traducirError("Email address \"x@y.z\" is invalid"))
        assertTrue(traducirError("Password should be at least 8 characters").startsWith("La contraseña es muy débil"))
        assertTrue(traducirError("Database error saving new user").startsWith("El servidor no pudo crear tu perfil"))
        assertEquals("Ya existe una cuenta con ese correo", traducirError("User already registered"))
        assertEquals("No se pudo completar. Intenta de nuevo", traducirError("algo raro"))
    }

    @Test fun detectaColumnaInexistenteDelServidor() {
        assertTrue(esColumnaInexistente("column activities.updated_at does not exist (42703)", "updated_at"))
        assertTrue(esColumnaInexistente("Could not find the 'updated_at' column of 'events'", "updated_at"))
        assertFalse(esColumnaInexistente("timeout", "updated_at"))
        assertFalse(esColumnaInexistente("column events.title does not exist", "updated_at"))
    }
}
