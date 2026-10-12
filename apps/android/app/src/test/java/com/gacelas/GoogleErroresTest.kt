package com.gacelas

import com.gacelas.acceso.GoogleAcceso
import com.gacelas.data.remoto.traducirError
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleErroresTest {
    @Test fun erroresDeSupabaseConCausa() {
        assertTrue(traducirError("Unacceptable audience in id_token: [123]").contains("Client IDs"))
        assertTrue(traducirError("Passed nonce and nonce in id_token should either both exist or not.").contains("nonce"))
        assertTrue(traducirError("Bad ID token").contains("Bad ID token"))
        assertTrue(traducirError("Provider (issuer \"https://accounts.google.com\") is not enabled").contains("Providers"))
        assertTrue(traducirError("Signups not allowed for this instance").contains("registro"))
    }

    @Test fun firmaNoRegistradaSeExplica() {
        assertTrue(GoogleAcceso.mensajeCredencial("TYPE_UNKNOWN", "[16] Account reauth failed.").contains("SHA-1"))
        assertTrue(GoogleAcceso.mensajeCredencial("x", "[10] Developer console is not set up correctly.").contains("SHA-1"))
        assertTrue(GoogleAcceso.mensajeCredencial("TYPE_X", "otro").contains("TYPE_X"))
    }
}
