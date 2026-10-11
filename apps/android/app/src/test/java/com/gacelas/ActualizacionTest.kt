package com.gacelas

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.gacelas.data.actualizacion.ActualizacionRepository
import com.gacelas.data.actualizacion.Aviso
import com.gacelas.data.actualizacion.InfoVersion
import com.gacelas.data.actualizacion.ReglasActualizacion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ActualizacionTest {
    @get:Rule val carpeta = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("act.preferences_pb") } }
    @After fun cerrar() = scope.cancel()

    private val json = """{"versionCode":602,"versionName":"0.6.2","notes":"Novedades","apkUrl":"https://x.supabase.co/storage/v1/object/public/app-releases/android/a.apk"}"""
    private fun info(code: Int, min: Int? = null) = InfoVersion(code, "v", apkUrl = "https://x/a.apk", minVersionCode = min)

    @Test fun reglas() {
        assertEquals(Aviso.Nada, ReglasActualizacion.aviso(602, info(602)))
        assertTrue(ReglasActualizacion.aviso(601, info(602)) is Aviso.Nueva)
        assertEquals(Aviso.Nada, ReglasActualizacion.aviso(601, info(602), pospuesta = 602))
        assertTrue((ReglasActualizacion.aviso(601, info(603), pospuesta = 602) as Aviso.Nueva).let { !it.obligatoria })
        assertTrue((ReglasActualizacion.aviso(601, info(602, min = 602), pospuesta = 602) as Aviso.Nueva).obligatoria)
        assertEquals(Aviso.Nada, ReglasActualizacion.aviso(1, InfoVersion(9, "v", apkUrl = "http://inseguro/a.apk")))
        assertEquals(601, ReglasActualizacion.codigoDe("0.6.1"))
        assertEquals(10203, ReglasActualizacion.codigoDe("1.2.3"))
        assertTrue(BuildConfig.VERSION_CODE == ReglasActualizacion.codigoDe(BuildConfig.VERSION_NAME))
        assertEquals("https://p.supabase.co/storage/v1/object/public/app-releases/android/latest.json", ReglasActualizacion.urlManifiesto("https://p.supabase.co/"))
    }

    @Test fun revisaUnaVezAlDiaYRecuerdaMasTarde() = runBlocking {
        var ahora = 1_000_000L; var llamadas = 0
        val r = ActualizacionRepository(store, "https://m", 601, { ahora }) { llamadas++; json }
        val a = r.revisar() as Aviso.Nueva
        assertEquals("0.6.2", a.info.versionName)
        r.revisar(); assertEquals(1, llamadas)
        r.posponer(602)
        assertEquals(Aviso.Nada, r.revisar())
        ahora += ReglasActualizacion.UN_DIA_MS
        r.revisar(); assertEquals(2, llamadas)
    }

    @Test fun sinRedUsaLoUltimoConocido() = runBlocking {
        var falla = false
        var ahora = 5L
        val r = ActualizacionRepository(store, "https://m", 601, { ahora }) { if (falla) error("sin red") else json }
        r.revisar()
        falla = true; ahora += ReglasActualizacion.UN_DIA_MS * 2
        assertTrue(r.revisar() is Aviso.Nueva)
    }
}
