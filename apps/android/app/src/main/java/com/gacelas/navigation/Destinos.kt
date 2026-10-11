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

    const val EDITAR_PERFIL = "perfil/editar"
    const val AVATAR = "perfil/avatar"

    const val AJUSTES = "ajustes"
    const val PRIVACIDAD = "ajustes/privacidad"
    const val CONFIG_NOTIFICACIONES = "ajustes/notificaciones"
    const val NOTIFICACIONES = "notificaciones"

    /** Pantallas del flujo de acceso, fuera del scaffold con barra inferior. */
    val acceso = listOf(BIENVENIDA, INICIO_SESION, REGISTRO, RECUPERAR_CONTRASENA, PERMISOS_UBICACION)

    /** Pantallas con barra inferior. */
    val conBarraInferior = listOf(INICIO, RUTAS, COMUNIDAD, EVENTOS, PERFIL)

    val todas = acceso + conBarraInferior + listOf(
        CORRER, CARRERA_EN_CURSO, CRONOMETRO, RESUMEN,
        BUSCADOR_RUTAS, NAVEGACION, DETALLE_EVENTO, EN_VIVO, DETALLE_EN_VIVO,
        EDITAR_PERFIL, AVATAR,
        AJUSTES, PRIVACIDAD, CONFIG_NOTIFICACIONES, NOTIFICACIONES,
    )
}
