package com.gacelas.inicio

import android.content.Context
import android.location.Geocoder
import com.gacelas.clima.domain.Coordenadas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Ubicacion para el clima de Inicio: la ultima conocida del sistema y solo si el permiso de
 * ubicacion (el de carreras) ya esta concedido; no pide permisos ni rastrea. null = Loja.
 */
suspend fun ubicacionParaClima(context: Context): Coordenadas? = withContext(Dispatchers.IO) {
    com.gacelas.rutas.ultimaUbicacion(context)?.let { (lat, lng) -> Coordenadas(lat, lng) }
}

/** Nombre simple (ciudad) con el Geocoder de Android; null si no hay servicio o falla. */
@Suppress("DEPRECATION")
suspend fun nombreLugarParaClima(context: Context, c: Coordenadas): String? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null
    runCatching {
        Geocoder(context, Locale("es")).getFromLocation(c.lat, c.lng, 1)?.firstOrNull()
            ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
    }.getOrNull()
}
