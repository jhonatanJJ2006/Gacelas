package com.gacelas.identidad.domain.repositorio

import com.gacelas.identidad.domain.modelo.Perfil
import com.gacelas.identidad.domain.modelo.ResultadoAuth
import kotlinx.coroutines.flow.Flow

/**
 * Autenticacion de la app. Hoy la implementa [LocalAuthRepository] (local, sin red);
 * mas adelante se puede reemplazar por una implementacion con Supabase Auth.
 */
interface AuthRepository {
    /** Perfil de la sesion activa o null si no hay sesion. */
    val sesion: Flow<Perfil?>
    suspend fun iniciarSesion(email: String, password: String): ResultadoAuth
    suspend fun registrar(nombre: String, email: String, password: String): ResultadoAuth
    suspend fun recuperarContrasena(email: String): ResultadoAuth
    suspend fun actualizarPerfil(perfil: Perfil)
    suspend fun cerrarSesion()
    /** True si la app puede entrar con Google (hay servidor configurado). */
    val googleDisponible: Boolean get() = false
    /** Entra con el ID token de Google obtenido con Credential Manager. */
    suspend fun entrarConGoogle(idToken: String, nonce: String?): ResultadoAuth =
        ResultadoAuth.Error("El inicio con Google no está disponible en esta versión de la app. Usa tu correo por ahora.")
    /** Entra con el ID token de Sign in with Apple (iOS). */
    suspend fun entrarConApple(idToken: String, nonce: String?): ResultadoAuth =
        ResultadoAuth.Error("El inicio con Apple no está disponible en esta versión de la app. Usa tu correo por ahora.")
}

