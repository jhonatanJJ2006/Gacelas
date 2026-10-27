package com.gacelas

import com.gacelas.ui.three.MotorCorredor
import com.gacelas.ui.three.motorCorredor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v0.9.5: Inicio (sobreFondo) usaba el WebView transparente desde 0.9.1 aunque el telefono tuviera
 * Filament, y el corredor no se dibujaba. Con Filament disponible tiene que usarse Filament translucido.
 */
class MotorCorredorTest {
    @Test fun inicioConFilamentUsaFilamentTranslucido() =
        assertEquals(MotorCorredor.FilamentTranslucido, motorCorredor(sobreFondo = true, filament = true))

    @Test fun otrasPantallasConFilament() = assertEquals(MotorCorredor.Filament, motorCorredor(sobreFondo = false, filament = true))

    @Test fun sinFilamentQuedaWebGL() {
        assertEquals(MotorCorredor.WebGL, motorCorredor(sobreFondo = true, filament = false))
        assertEquals(MotorCorredor.WebGL, motorCorredor(sobreFondo = false, filament = false))
    }
}
