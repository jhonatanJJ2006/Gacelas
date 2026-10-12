package com.gacelas.data.carreras

import com.gacelas.shared.Fechas

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class PuntoGps(val lat: Double, val lng: Double, val tiempoMs: Long, val precisionM: Float = 0f, val altitudM: Double? = null)

@Serializable
data class Parcial(val numero: Int, val distanciaKm: Double, val segundos: Int, val manual: Boolean = false)

@Serializable
data class CarreraGuardada(
    val id: String,
    val titulo: String,
    val nota: String = "",
    val publica: Boolean = true,
    val inicioMs: Long,
    val segundos: Int,
    val distanciaKm: Double,
    val desnivelM: Int = 0,
    val parciales: List<Parcial> = emptyList(),
    val puntos: List<PuntoGps> = emptyList(),
) {
    /** Ritmo promedio en segundos por km, o null si no hay distancia. */
    val ritmoSegKm: Int? get() = if (distanciaKm > 0.01) (segundos / distanciaKm).toInt() else null
}

/** Historial de carreras. Hoy local (DataStore); preparado para Supabase. */
interface CarrerasRepository {
    val carreras: Flow<List<CarreraGuardada>>
    suspend fun guardar(carrera: CarreraGuardada)
    suspend fun eliminar(id: String)
}

class LocalCarrerasRepository(store: DataStore<Preferences>) : CarrerasRepository {
    private val valor = ValorJson(store, "carreras", ListSerializer(CarreraGuardada.serializer()), emptyList())
    override val carreras: Flow<List<CarreraGuardada>> = valor.flujo
    override suspend fun guardar(carrera: CarreraGuardada) = valor.actualizar { lista -> listOf(carrera) + lista.filterNot { it.id == carrera.id } }
    override suspend fun eliminar(id: String) = valor.actualizar { lista -> lista.filterNot { it.id == id } }
}

data class ResumenSemana(
    /** Dias de la semana (0 = lunes ... 6 = domingo) con al menos una carrera. */
    val diasCorridos: Set<Int>,
    val km: Double,
    val ritmoSegKm: Int?,
    val carreras: Int,
    /** Dias seguidos con carrera hasta hoy (o ayer). */
    val racha: Int,
)

data class ResumenPeriodo(val carreras: Int, val km: Double, val segundos: Int, val desnivelM: Int)

object Estadisticas {
    private fun inicioDia(ms: Long): Long = Fechas.inicioDia(ms)

    fun inicioSemana(ahoraMs: Long): Long {
        val d = inicioDia(ahoraMs)
        return Fechas.sumarDias(d, -Fechas.diaSemana(d))
    }

    fun diaSemana(ms: Long): Int = Fechas.diaSemana(ms)

    fun semana(carreras: List<CarreraGuardada>, ahoraMs: Long): ResumenSemana {
        val desde = inicioSemana(ahoraMs)
        val semana = carreras.filter { it.inicioMs >= desde && it.inicioMs <= ahoraMs + 86_400_000L }
        val km = semana.sumOf { it.distanciaKm }
        val seg = semana.sumOf { it.segundos }
        return ResumenSemana(
            diasCorridos = semana.map { diaSemana(it.inicioMs) }.toSet(),
            km = km,
            ritmoSegKm = if (km > 0.01) (seg / km).toInt() else null,
            carreras = semana.size,
            racha = racha(carreras, ahoraMs),
        )
    }

    fun racha(carreras: List<CarreraGuardada>, ahoraMs: Long): Int {
        val dias = carreras.map { inicioDia(it.inicioMs) }.toSet()
        var dia = inicioDia(ahoraMs)
        if (dia !in dias) dia = Fechas.sumarDias(dia, -1)
        var n = 0
        while (dia in dias) {
            n++
            dia = Fechas.sumarDias(dia, -1)
        }
        return n
    }

    fun mes(carreras: List<CarreraGuardada>, ahoraMs: Long): ResumenPeriodo {
        val c = Fechas.fecha(ahoraMs)
        val delMes = carreras.filter {
            val x = Fechas.fecha(it.inicioMs)
            x.year == c.year && x.monthNumber == c.monthNumber
        }
        return ResumenPeriodo(delMes.size, delMes.sumOf { it.distanciaKm }, delMes.sumOf { it.segundos }, delMes.sumOf { it.desnivelM })
    }
}
