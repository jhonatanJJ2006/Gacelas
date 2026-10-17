package com.gacelas.cuenta.domain.modelo

import kotlinx.serialization.Serializable

@Serializable
data class Perfil(
    val email: String,
    val nombre: String,
    val username: String,
    val bio: String = "",
    val ubicacion: String = "Loja, Ecuador",
    val edad: String = "",
    val genero: String = "Prefiero no decir",
    val fotoPath: String? = null,
)

sealed interface ResultadoAuth {
    data class Ok(val perfil: Perfil) : ResultadoAuth
    data class Error(val mensaje: String) : ResultadoAuth
}

