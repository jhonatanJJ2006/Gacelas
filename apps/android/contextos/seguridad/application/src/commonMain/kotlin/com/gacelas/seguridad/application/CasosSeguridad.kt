package com.gacelas.seguridad.application

import com.gacelas.seguridad.domain.IncidentePublico
import com.gacelas.seguridad.domain.Incidentes
import com.gacelas.seguridad.domain.IncidentesRepositorio
import com.gacelas.seguridad.domain.SeguridadZona
import com.gacelas.seguridad.domain.TipoIncidente

/**
 * Seguridad de una ruta por dia de la semana. Sin servidor se muestran los datos de ejemplo;
 * con servidor, null significa "sin datos" (nunca se presenta como seguro).
 */
class ConsultarSeguridadDeRuta(private val repo: IncidentesRepositorio?) {
    suspend operator fun invoke(rutaId: String): SeguridadZona? =
        if (repo == null) Incidentes.EJEMPLO else repo.seguridadZona(rutaId)
}

/** Incidentes recientes reportados por corredores (o los de ejemplo sin servidor). */
class VerIncidentesRecientes(private val repo: IncidentesRepositorio?) {
    suspend operator fun invoke(ahoraMs: Long): List<IncidentePublico> =
        repo?.recientes() ?: Incidentes.ejemplos(ahoraMs)
}

/** Datos de un reporte de incidente; las fotos llegan ya sin EXIF. */
data class NuevoIncidente(
    val tipo: TipoIncidente,
    val lat: Double,
    val lng: Double,
    val direccion: String,
    val ocurridoMs: Long,
    val descripcion: String?,
    val fotosJpeg: List<ByteArray>,
)

class ReportarIncidente(private val repo: IncidentesRepositorio) {
    suspend operator fun invoke(n: NuevoIncidente, carpeta: String) =
        repo.reportar(n.tipo, n.lat, n.lng, n.direccion, n.ocurridoMs, n.descripcion, n.fotosJpeg, carpeta)
}

class ConfirmarIncidente(private val repo: IncidentesRepositorio) {
    suspend operator fun invoke(id: String): Boolean = repo.confirmar(id)
}

/** Dia de la semana (0 = domingo) con el indice mas bajo, o null si no hay datos. */
object SugerirDiaSeguro {
    operator fun invoke(zona: SeguridadZona?): Int? = zona?.dias?.minByOrNull { it.indice }?.dia
}
