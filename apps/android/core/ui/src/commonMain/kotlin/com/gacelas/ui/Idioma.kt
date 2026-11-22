package com.gacelas.ui

import androidx.compose.runtime.compositionLocalOf

/** Espanol de Ecuador es el texto de origen. Ingles solo donde hay traduccion. */
val LocalIdioma = compositionLocalOf { "es-EC" }

private val EN = mapOf(
    "Inicio" to "Home",
    "Rutas" to "Routes",
    "Correr" to "Run",
    "Comunidad" to "Community",
    "Perfil" to "Profile",
    "Notificaciones" to "Notifications",
    "Hoy" to "Today",
    "Club" to "Club",
    "Tú" to "You",
    "CORRER" to "RUN",
    "Apariencia" to "Appearance",
    "Interfaz" to "Interface",
    "Tema" to "Theme",
    "Optimizado para salidas nocturnas y al alba" to "Tuned for night runs and dawn",
    "Oscuro" to "Dark",
    "Claro" to "Light",
    "Idioma de la aplicación" to "App language",
    "Configuración regional" to "Regional settings",
    "Rachas" to "Streaks",
    "Constancia" to "Consistency",
    "Meta semanal" to "Weekly goal",
    "Carreras por semana para mantener la racha" to "Runs per week to keep the streak",
    "Mapas offline" to "Offline maps",
    "Rutas de Loja sin conexión" to "Loja routes without a connection",
    "Brújula" to "Compass",
    "Rumbo y calibración" to "Heading and calibration",
    "Menos carreras por semana" to "Fewer runs per week",
    "Más carreras por semana" to "More runs per week",
)

fun tr(idioma: String, es: String): String = if (idioma == "en") EN[es] ?: es else es
