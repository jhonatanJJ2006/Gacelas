package com.gacelas.data.mock

import kotlinx.serialization.Serializable

/** Datos de prueba tipados, equivalentes a apps/web/src/mocks/data.ts. Se reemplazaran por Supabase. */
data class Usuario(val id: String, val nombre: String, val username: String)

data class Actividad(
    val id: String,
    val usuarioId: String,
    val titulo: String,
    val distanciaKm: Double,
    val duracionSegundos: Int,
    val ritmo: String,
    val fecha: String,
    val likes: Int,
    val comentarios: Int,
)

enum class Dificultad(val etiqueta: String) { Facil("Fácil"), Moderado("Moderado"), Dificil("Difícil") }

data class Ruta(
    val id: String,
    val nombre: String,
    val distanciaKm: Double,
    val dificultad: Dificultad,
    val elevacionM: Int,
    val descripcion: String,
    val autorId: String = "gacelas",
    val etiquetas: List<String> = emptyList(),
    val puntos: List<Pair<Double, Double>> = emptyList(),
    val meGusta: Int = 0,
    val valoracion: Double? = null,
    val resenas: Int = 0,
    val autorNombre: String? = null,
    val propia: Boolean = false,
)

@Serializable
data class Evento(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fecha: String,
    val hora: String,
    val ubicacion: String,
    val participantes: Int,
    val distanciaKm: Int,
    val pasado: Boolean = false,
    val creadoPorUsuario: Boolean = false,
)

data class Split(val id: Int, val distancia: Double, val ritmo: String, val tiempo: String, val etiqueta: String, val actual: Boolean = false)

data class RutaSugerida(val id: String, val nombre: String, val distanciaKm: Double, val minutos: Int, val elevacionM: Int, val principal: Boolean)

enum class TipoNotificacion { Like, Comentario, Seguidor, Evento, EnVivo, Racha }

data class Notificacion(val id: String, val tipo: TipoNotificacion, val usuario: String, val mensaje: String, val tiempo: String, val leida: Boolean)

object MockData {
    /** Identificador del usuario local para rutas propias. */
    const val ID_USUARIO = "0"

    val usuarios = listOf(
        Usuario("1", "María López", "maria_runs"),
        Usuario("2", "Carlos Ruiz", "carlos_runner"),
        Usuario("3", "Ana Martínez", "ana_trails"),
    )

    val usuarioActual = Usuario(ID_USUARIO, "Jhonatan Runner", "jhonatan_runs")

    val actividades = listOf(
        Actividad("1", "1", "Carrera matutina", 5.2, 1714, "5:30", "hace 2h", 23, 5),
        Actividad("2", "2", "Entrenamiento de intervalos", 8.5, 2720, "5:20", "hace 4h", 18, 3),
    )

    val rutas = listOf(
        Ruta("1", "Parque Jipiro", 5.2, Dificultad.Facil, 45, "Ruta panorámica alrededor del Parque Jipiro, ideal para principiantes.", etiquetas = listOf("parque", "plana", "urbana")),
        Ruta("2", "Río Zamora", 8.5, Dificultad.Moderado, 120, "Ruta junto al río con algunas subidas moderadas."),
        Ruta("3", "Cerro Villonaco", 12.3, Dificultad.Dificil, 380, "Ruta de montaña desafiante con vistas espectaculares de Loja."),
        Ruta("4", "Campus UTPL - San Cayetano", 6.1, Dificultad.Moderado, 150, "Mi ruta de entrenamiento entre el campus y San Cayetano.", autorId = ID_USUARIO),
    )

    val eventos = listOf(
        Evento("1", "Carrera Parque Jipiro", "Carrera grupal de 10km por el Parque Jipiro. Todos los niveles bienvenidos.", "Sábado 11 de Octubre", "6:00 AM", "Parque Jipiro, Loja", 15, 10),
        Evento("2", "Trail Running Podocarpus", "Aventura de trail running en el Parque Nacional Podocarpus.", "Domingo 19 de Octubre", "7:00 AM", "Entrada Cajanuma", 8, 15),
        Evento("3", "Entrenamiento Intervalos Villonaco", "Series en la base del Cerro Villonaco.", "Miércoles 15 de Octubre", "6:30 AM", "Base Cerro Villonaco", 12, 8),
        Evento("4", "Fondo dominical Zamora", "Fondo suave de 12 km junto al río Zamora.", "Domingo 28 de Septiembre", "6:30 AM", "Parque Lineal La Tebaida", 21, 12, pasado = true),
    )

    val splits = listOf(
        Split(5, 5.42, "4:52", "02:18", "En curso actual", actual = true),
        Split(4, 1.0, "5:12", "05:12", "Subida San Cayetano"),
        Split(3, 1.0, "4:48", "04:48", "Mejor vuelta"),
        Split(2, 1.0, "5:20", "05:20", "Av. Universitaria"),
        Split(1, 1.0, "5:08", "05:08", "Inicio Jipiro"),
    )

    val rutasSugeridas = listOf(
        RutaSugerida("primary", "Jipiro → Puerta → Zamora Huayco", 8.5, 45, 120, principal = true),
        RutaSugerida("alt", "Ruta Universitaria Directa", 6.8, 36, 85, principal = false),
    )

    val notificaciones = listOf(
        Notificacion("1", TipoNotificacion.EnVivo, "María López", "está corriendo ahora", "hace 5 min", false),
        Notificacion("2", TipoNotificacion.Like, "Carlos Ruiz", "le dio like a tu carrera matutina", "hace 1h", false),
        Notificacion("3", TipoNotificacion.Comentario, "Ana Martínez", "comentó en tu actividad: \"Excelente ritmo\"", "hace 2h", true),
        Notificacion("4", TipoNotificacion.Seguidor, "Pedro Sánchez", "comenzó a seguirte", "hace 3h", true),
        Notificacion("5", TipoNotificacion.Evento, "Gacelas Loja", "Nuevo evento: Carrera Parque Jipiro", "hace 1d", true),
    )

    fun usuario(id: String): Usuario? = usuarios.firstOrNull { it.id == id }

    fun evento(id: String): Evento? = eventos.firstOrNull { it.id == id }

    fun duracionTexto(segundos: Int): String = "%d:%02d".format(segundos / 60, segundos % 60)
}
