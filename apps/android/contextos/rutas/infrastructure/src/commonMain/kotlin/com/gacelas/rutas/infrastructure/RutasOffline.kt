package com.gacelas.rutas.infrastructure

import com.gacelas.rutas.domain.reglas.Guia

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.modelo.Ruta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** Ruta creada sin conexion, con todo lo necesario para publicarla despues. */
@Serializable
data class RutaNueva(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val dificultad: Dificultad,
    val etiquetas: List<String>,
    val puntos: List<Pair<Double, Double>>,
    val elevacionM: Int,
    val actividadId: String? = null,
    val perfil: List<Int> = emptyList(),
    val origen: String = "actividad",
)

/** Operacion de rutas pendiente de subir. tipo: crear, corrida, guardar o quitar. */
@Serializable
data class OperacionRuta(
    val tipo: String,
    val rutaId: String,
    val nueva: RutaNueva? = null,
    val actividadId: String? = null,
    val intentos: Int = 0,
)

/**
 * Rutas guardadas en el celular (DataStore, igual que carreras y ajustes): copia de las rutas
 * del servidor para verlas y navegarlas sin red, y cola de cambios pendientes que sube
 * SincronizacionWorker cuando vuelve la conexion.
 */
class RutasOffline(store: DataStore<Preferences>) {
    companion object {
        /** Una operacion que falla mas veces se descarta (dato invalido, no falta de red). */
        const val INTENTOS_MAX = 8
    }

    private val cache = ValorJson(store, "rutas_cache", ListSerializer(Ruta.serializer()), emptyList())
    private val cola = ValorJson(store, "rutas_pendientes", ListSerializer(OperacionRuta.serializer()), emptyList())

    val rutas: Flow<List<Ruta>> = cache.flujo
    val pendientes: Flow<List<OperacionRuta>> = cola.flujo

    suspend fun rutasAhora(): List<Ruta> = cache.flujo.first()

    /** Reemplaza la copia local con las del servidor, conservando las creadas sin red. */
    suspend fun guardarDelServidor(remotas: List<Ruta>) {
        val locales = cola.flujo.first().mapNotNull { it.nueva?.id }.toSet()
        cache.actualizar { actual -> remotas + actual.filter { it.id in locales && remotas.none { r -> r.id == it.id } } }
    }

    suspend fun guardarRuta(ruta: Ruta) = cache.actualizar { lista -> listOf(ruta) + lista.filterNot { it.id == ruta.id } }

    suspend fun encolar(op: OperacionRuta) = cola.actualizar { lista ->
        // Guardar y quitar la misma ruta se anulan: solo vale la ultima.
        val sinOpuesta = if (op.tipo == "guardar" || op.tipo == "quitar") lista.filterNot { (it.tipo == "guardar" || it.tipo == "quitar") && it.rutaId == op.rutaId } else lista
        sinOpuesta + op
    }

    /**
     * Sube la cola en orden con [subir]. Se detiene en el primer fallo (sin red) y suma un intento;
     * descarta operaciones que superan [INTENTOS_MAX]. Devuelve cuantas quedan.
     */
    suspend fun procesar(subir: suspend (OperacionRuta) -> Unit): Int {
        val ops = cola.flujo.first()
        var hechas = 0
        var fallo: OperacionRuta? = null
        for (op in ops) {
            if (runCatching { subir(op) }.isFailure) { fallo = op; break }
            hechas++
        }
        cola.actualizar { actual ->
            var resto = actual.drop(hechas)
            if (fallo != null && resto.firstOrNull() == fallo) {
                val f = fallo.copy(intentos = fallo.intentos + 1)
                resto = if (f.intentos >= INTENTOS_MAX) resto.drop(1) else listOf(f) + resto.drop(1)
            }
            resto
        }
        return cola.flujo.first().size
    }
}

fun RutaNueva.aRuta(autorId: String): Ruta = Ruta(
    id = id, nombre = nombre, distanciaKm = Guia.longitudM(puntos) / 1000.0, dificultad = dificultad, elevacionM = elevacionM,
    descripcion = descripcion, autorId = autorId, etiquetas = etiquetas, puntos = puntos, propia = true, perfilElevacion = perfil,
)
