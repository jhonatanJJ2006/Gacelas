package com.gacelas

import com.gacelas.social.Logro
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El catalogo de logros vive en tres sitios que deben decir lo mismo: Android (enum Logro), la web y
 * la base (migracion 049). packages/shared-types/logros-catalogo.json es la referencia.
 */
class ParidadLogrosTest {
    private fun raiz(): File {
        var d: File? = File("").absoluteFile
        while (d != null && !File(d, "packages/shared-types/logros-catalogo.json").exists()) d = d.parentFile
        return requireNotNull(d) { "no se encuentra packages/shared-types/logros-catalogo.json" }
    }

    private fun sinTildes(s: String) = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

    @Test
    fun androidDeclaraElMismoCatalogoQueElJson() {
        val json = Json.parseToJsonElement(File(raiz(), "packages/shared-types/logros-catalogo.json").readText()).jsonObject
        val logros: JsonArray = json.getValue("logros").jsonArray
        assertEquals(26, logros.size)
        val esperado = logros.map { e ->
            val o: JsonObject = e.jsonObject
            listOf(
                o.getValue("codigo").jsonPrimitive.content, o.getValue("titulo").jsonPrimitive.content,
                o.getValue("descripcion").jsonPrimitive.content, o.getValue("comoConseguirlo").jsonPrimitive.content,
                o.getValue("categoria").jsonPrimitive.content, o.getValue("meta").jsonPrimitive.int.toString(),
                o.getValue("unidad").jsonPrimitive.content,
            )
        }
        val actual = Logro.entries.map { l ->
            listOf(l.codigo, l.titulo, l.descripcion, l.comoConseguirlo, sinTildes(l.categoria.name).lowercase(), l.meta.toString(), l.unidad)
        }
        assertEquals(esperado, actual)
    }

    @Test
    fun laMigracionInsertaElMismoCatalogo() {
        val sql = File(raiz(), "supabase/migrations/049_logros_unificados.sql").readText()
        Logro.entries.forEach { l ->
            assertTrue("049 no inserta ${l.codigo}", sql.contains("('${l.codigo}', '${l.titulo.replace("'", "''")}'"))
        }
    }
}
