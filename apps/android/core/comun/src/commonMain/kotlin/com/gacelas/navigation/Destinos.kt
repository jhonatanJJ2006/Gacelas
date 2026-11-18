package com.gacelas.navigation

/** Rutas de navegacion. Coinciden con las rutas de la web (apps/web/src/App.tsx). */
object Destinos {
    const val BIENVENIDA = "bienvenida"
    const val INICIO_SESION = "inicio-sesion"
    const val REGISTRO = "registro"
    const val RECUPERAR_CONTRASENA = "recuperar-contrasena"
    const val PERMISOS_UBICACION = "permisos-ubicacion"

    const val INICIO = "inicio"
    const val RUTAS = "rutas"
    const val COMUNIDAD = "comunidad"
    const val EVENTOS = "comunidad/eventos"
    const val PERFIL = "perfil"

    const val SEMANA = "inicio/semana"
    const val CLIMA = "inicio/clima"
    const val MAPA_SEGURIDAD = "inicio/seguridad"

    const val CORRER = "correr"
    const val CARRERA_EN_CURSO = "correr/en-curso"
    const val CRONOMETRO = "correr/cronometro"
    const val RESUMEN = "correr/resumen"

    const val BUSCADOR_RUTAS = "rutas/buscar"
    const val NAVEGACION = "rutas/navegacion"

    const val DETALLE_EVENTO = "comunidad/eventos/{id}"
    fun detalleEvento(id: String) = "comunidad/eventos/$id"

    const val EN_VIVO = "comunidad/en-vivo"
    const val DETALLE_EN_VIVO = "comunidad/en-vivo/{id}"
    fun detalleEnVivo(id: String) = "comunidad/en-vivo/$id"

    const val CREAR_RUTA = "rutas/crear"
    const val CLUBES = "comunidad/clubes"
    const val DETALLE_CLUB = "comunidad/clubes/{id}"
    fun detalleClub(id: String) = "comunidad/clubes/$id"
    const val EVENTO_EN_VIVO = "comunidad/eventos/{id}/en-vivo"
    fun eventoEnVivo(id: String) = "comunidad/eventos/$id/en-vivo"
    const val DETALLE_RUTA = "rutas/detalle/{id}"
    fun detalleRuta(id: String) = "rutas/detalle/$id"

    const val EDITAR_PERFIL = "perfil/editar"
    const val MIS_REPORTES = "perfil/reportes"
    const val AVATAR = "perfil/avatar"
    const val LOGROS = "perfil/logros"
    const val MEDALLERO = "perfil/medallero"
    const val DETALLE_CARRERA = "perfil/carrera/{id}"
    fun detalleCarrera(id: String) = "perfil/carrera/$id"

    const val AJUSTES = "ajustes"
    const val ADMIN = "ajustes/admin"
    const val PRIVACIDAD = "ajustes/privacidad"
    const val CONFIG_NOTIFICACIONES = "ajustes/notificaciones"
    const val NOTIFICACIONES = "notificaciones"
    const val BRUJULA = "ajustes/brujula"
    const val MAPAS_OFFLINE = "ajustes/mapas-offline"
    const val MAPA_VIVO = "comunidad/mapa"
    const val TERMINOS = "legal/terminos"
    const val POLITICA_PRIVACIDAD = "legal/privacidad"

    /** Pantallas del flujo de acceso, fuera del scaffold con barra inferior. */
    val acceso = listOf(BIENVENIDA, INICIO_SESION, REGISTRO, RECUPERAR_CONTRASENA, PERMISOS_UBICACION)

    /** Pantallas con barra inferior. */
    val conBarraInferior = listOf(INICIO, RUTAS, COMUNIDAD, EVENTOS, PERFIL)

    /** Pantallas de una carrera: no pertenecen a ninguna pestana, asi que no se guardan con su pila. */
    val flujoCarrera = listOf(CORRER, CARRERA_EN_CURSO, CRONOMETRO, RESUMEN, NAVEGACION)

    val todas = acceso + conBarraInferior + listOf(
        CORRER, CARRERA_EN_CURSO, CRONOMETRO, RESUMEN,
        BUSCADOR_RUTAS, NAVEGACION, DETALLE_EVENTO, EN_VIVO, DETALLE_EN_VIVO, DETALLE_RUTA, CREAR_RUTA, CLUBES, DETALLE_CLUB, EVENTO_EN_VIVO,
        EDITAR_PERFIL, AVATAR, LOGROS, MEDALLERO,
        SEMANA, CLIMA, MAPA_SEGURIDAD,
        AJUSTES, PRIVACIDAD, CONFIG_NOTIFICACIONES, NOTIFICACIONES, BRUJULA, MAPAS_OFFLINE,
        MAPA_VIVO, TERMINOS, POLITICA_PRIVACIDAD,
    )
}
