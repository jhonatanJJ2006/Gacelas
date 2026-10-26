package com.gacelas.identidad.domain.reglas

/** Reglas de validacion de los formularios de acceso. Devuelven el mensaje de error o null. */
object ValidacionAuth {
    private val regexEmail = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun email(email: String): String? = when {
        email.isBlank() -> "Ingresa tu correo electrónico"
        !regexEmail.matches(email.trim()) -> "El correo no es válido"
        else -> null
    }

    fun password(password: String): String? = when {
        password.length < 8 -> "La contraseña debe tener mínimo 8 caracteres"
        password.none { it.isLetter() } || password.none { it.isDigit() } -> "Usa letras y números"
        else -> null
    }

    fun nombre(nombre: String): String? = if (nombre.trim().length < 2) "Ingresa tu nombre" else null

    fun usernameDesde(nombre: String, email: String): String {
        val base = nombre.lowercase().filter { it.isLetterOrDigit() || it == ' ' }.trim().replace(' ', '_')
        return base.ifBlank { email.substringBefore('@') }.take(20)
    }
}

