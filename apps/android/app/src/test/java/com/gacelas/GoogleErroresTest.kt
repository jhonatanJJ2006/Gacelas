package com.gacelas

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.gacelas.acceso.GoogleAcceso
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleErroresTest {
    @Test fun cancelarNoMuestraNada() = assertNull(GoogleAcceso.mensajeError(GetCredentialCancellationException()))

    @Test fun sinCredencialMencionaConfiguracion() {
        val m = GoogleAcceso.mensajeError(NoCredentialException())!!
        assertTrue(m.contains("SHA-1"))
    }

    @Test fun errorDeConfiguracionNoDiceNoHayCuentas() {
        val m = GoogleAcceso.mensajeError(GetCredentialProviderConfigurationException())!!
        assertFalse(m.contains("no hay", ignoreCase = true))
        assertTrue(GoogleAcceso.mensajeError(IllegalStateException())!!.contains("IllegalStateException"))
    }

    @Test fun nonceSha256() = assertTrue(GoogleAcceso.sha256("abc") == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
}
