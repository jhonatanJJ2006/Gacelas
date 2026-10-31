package com.gacelas.administracion.domain

/**
 * Rol de la sesion en el panel. Lo decide el servidor (public.is_admin y public.es_moderador,
 * migracion 020); el cliente solo lo usa para mostrar u ocultar la entrada.
 */
enum class RolAdmin(val clave: String, val etiqueta: String) {
    Admin("admin", "Administrador"),
    Moderador("moderador", "Moderador"),
    ;

    companion object {
        fun desde(clave: String?): RolAdmin? = entries.firstOrNull { it.clave == clave }
    }
}

/** Lo que la sesion actual puede hacer en el panel. Sin rol no hay entrada. */
data class AccesoAdmin(val rol: RolAdmin?) {
    val puedeEntrar: Boolean get() = rol != null
    val esAdmin: Boolean get() = rol == RolAdmin.Admin

    /** Secciones visibles: el moderador solo modera (como en el servidor, admin_cola_reportes). */
    val secciones: List<SeccionAdmin>
        get() = when (rol) {
            RolAdmin.Admin -> SeccionAdmin.entries
            RolAdmin.Moderador -> listOf(SeccionAdmin.Moderacion)
            null -> emptyList()
        }

    companion object {
        val Ninguno = AccesoAdmin(null)
    }
}

/** Secciones del panel, en el mismo orden que la web. */
enum class SeccionAdmin(val titulo: String, val descripcion: String) {
    Dashboard("Dashboard", "Métricas de la última semana"),
    Usuarios("Usuarios", "Buscar, suspender y cambiar rol"),
    Moderacion("Moderación", "Cola de reportes"),
    Estadisticas("Estadísticas", "Uso por rango de fechas"),
    Eventos("Eventos", "Publicar y editar eventos"),
    RutasYClubes("Rutas y clubes", "Listar y eliminar"),
    Ajustes("Versiones y ajustes", "Versión mínima e interruptores"),
}

/** Resultado de admin_metricas(). */
data class MetricasAdmin(
    val usuarios: Long,
    val usuarios7d: Long,
    val carreras7d: Long,
    val km7d: Double,
    val reportesPendientes: Long,
    val eventosProximos: Long,
    val clubes: Long,
    val rutas: Long,
)

/** Fila de admin_usuarios(). */
data class UsuarioAdmin(
    val id: String,
    val username: String,
    val nombre: String?,
    val creadoEn: String,
    val suspendidoEn: String?,
    val rol: RolAdmin?,
    val carreras: Long,
    val km: Double,
) {
    val suspendido: Boolean get() = suspendidoEn != null
    val nombreVisible: String get() = nombre?.takeIf { it.isNotBlank() } ?: username
}

/** Busca por nombre o usuario sin distinguir mayusculas ni tildes. */
fun List<UsuarioAdmin>.buscar(texto: String): List<UsuarioAdmin> {
    val q = texto.trim().normalizar()
    if (q.isEmpty()) return this
    return filter { q in it.username.normalizar() || q in it.nombre.orEmpty().normalizar() }
}

internal fun String.normalizar(): String = lowercase()
    .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n')

/** Evento tal como lo edita el panel (tabla events). */
data class EventoAdmin(
    val id: String?,
    val titulo: String,
    val descripcion: String = "",
    val iniciaEn: String? = null,
    val lugar: String = "",
    val distanciaKm: Double = 0.0,
    val publicado: Boolean = false,
)

data class RutaAdmin(val id: String, val nombre: String, val distanciaKm: Double, val dificultad: String)

data class ClubAdmin(val id: String, val nombre: String, val descripcion: String)

/** Ajuste de app_settings. [valor] es el JSON tal cual (booleano, texto o numero). */
data class AjusteApp(val clave: String, val valor: kotlinx.serialization.json.JsonElement)

/** Resultado de admin_estadisticas(desde, hasta): totales del rango. */
data class EstadisticasAdmin(val desde: String, val hasta: String, val totales: Map<String, Double>)

const val CLAVE_VERSION_MINIMA = "version_minima_android"

/** Valida una version semantica simple (1.2.3). */
fun versionValida(v: String): Boolean = Regex("""^\d+\.\d+\.\d+$""").matches(v.trim())
