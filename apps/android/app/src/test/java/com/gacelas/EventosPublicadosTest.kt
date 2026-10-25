package com.gacelas

import com.gacelas.comunidad.data.eventoVisible
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventosPublicadosTest {
    @Test fun ocultaBorradoresAlResto() {
        assertTrue(eventoVisible(buildJsonObject { put("published", JsonPrimitive(true)) }, "u1"))
        assertTrue(eventoVisible(buildJsonObject { }, "u1"))
        assertFalse(eventoVisible(buildJsonObject { put("published", false); put("created_by", "u2") }, "u1"))
        assertTrue(eventoVisible(buildJsonObject { put("published", false); put("created_by", "u1") }, "u1"))
    }
}
