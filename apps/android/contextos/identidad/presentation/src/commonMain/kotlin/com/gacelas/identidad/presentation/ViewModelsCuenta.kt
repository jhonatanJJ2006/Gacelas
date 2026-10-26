package com.gacelas.identidad.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gacelas.acceso.FaseGoogle
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.identidad.domain.modelo.Perfil
import com.gacelas.identidad.domain.modelo.ResultadoAuth
import com.gacelas.identidad.domain.repositorio.AjustesRepository
import com.gacelas.identidad.domain.repositorio.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoLogin(val email: String = "", val password: String = "", val error: String? = null, val cargando: Boolean = false)

/** Iniciar sesion con correo y contrasena. */
class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoLogin())
    val estado: StateFlow<EstadoLogin> = _estado.asStateFlow()

    fun email(v: String) = _estado.update { it.copy(email = v, error = null) }
    fun password(v: String) = _estado.update { it.copy(password = v, error = null) }

    fun entrar(alEntrar: (Perfil) -> Unit) {
        val e = _estado.value
        if (e.cargando) return
        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            when (val r = auth.iniciarSesion(e.email, e.password)) {
                is ResultadoAuth.Ok -> alEntrar(r.perfil)
                is ResultadoAuth.Error -> _estado.update { it.copy(error = r.mensaje) }
            }
            _estado.update { it.copy(cargando = false) }
        }
    }
}

data class EstadoRegistro(val nombre: String = "", val email: String = "", val password: String = "", val error: String? = null, val cargando: Boolean = false)

/** Crear cuenta. */
class RegistroViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoRegistro())
    val estado: StateFlow<EstadoRegistro> = _estado.asStateFlow()

    fun nombre(v: String) = _estado.update { it.copy(nombre = v, error = null) }
    fun email(v: String) = _estado.update { it.copy(email = v, error = null) }
    fun password(v: String) = _estado.update { it.copy(password = v, error = null) }

    fun crear(alCrear: (Perfil) -> Unit) {
        val e = _estado.value
        if (e.cargando) return
        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            when (val r = auth.registrar(e.nombre, e.email, e.password)) {
                is ResultadoAuth.Ok -> alCrear(r.perfil)
                is ResultadoAuth.Error -> _estado.update { it.copy(error = r.mensaje) }
            }
            _estado.update { it.copy(cargando = false) }
        }
    }
}

data class EstadoRecuperar(val email: String = "", val enviado: Boolean = false, val error: String? = null)

/** Recuperar contrasena por correo. */
class RecuperarViewModel(private val auth: AuthRepository, enviadoInicial: Boolean = false) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoRecuperar(enviado = enviadoInicial))
    val estado: StateFlow<EstadoRecuperar> = _estado.asStateFlow()

    fun email(v: String) = _estado.update { it.copy(email = v, error = null) }

    fun enviar() {
        val email = _estado.value.email
        viewModelScope.launch {
            when (val r = auth.recuperarContrasena(email)) {
                is ResultadoAuth.Ok -> _estado.update { it.copy(enviado = true) }
                is ResultadoAuth.Error -> _estado.update { it.copy(error = r.mensaje) }
            }
        }
    }
}

enum class Proveedor(val nombre: String) { Google("Google"), Apple("Apple") }

/** Token del proveedor nativo (Credential Manager, GoogleSignIn o AuthenticationServices). mensaje null = cancelado. */
sealed interface TokenSocial {
    data class Ok(val idToken: String, val nonce: String?) : TokenSocial
    data class Error(val mensaje: String?) : TokenSocial
}

data class ErrorSocial(val titulo: String, val mensaje: String)

/** Entrar con Google o Apple: fases de carga y errores iguales en Android e iOS. */
class AccesoSocialViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _fase = MutableStateFlow<FaseGoogle?>(null)
    val fase: StateFlow<FaseGoogle?> = _fase.asStateFlow()
    private val _error = MutableStateFlow<ErrorSocial?>(null)
    val error: StateFlow<ErrorSocial?> = _error.asStateFlow()

    fun cerrarError() { _error.value = null }

    /**
     * [disponible] false muestra el aviso "no disponible" sin pedir token (sin servidor o sin cliente configurado).
     * [pedirToken] abre el selector nativo del proveedor.
     */
    fun entrar(p: Proveedor, disponible: Boolean = auth.googleDisponible, pedirToken: suspend () -> TokenSocial, alEntrar: () -> Unit) {
        val titulo = "No se pudo entrar con ${p.nombre}"
        _error.value = null
        if (!disponible) {
            _error.value = ErrorSocial(titulo, "El inicio con ${p.nombre} no está disponible en esta versión de la app. Usa tu correo por ahora.")
            return
        }
        _fase.value = if (p == Proveedor.Apple) FaseGoogle.Servidor else FaseGoogle.Cuenta
        viewModelScope.launch {
            try {
                when (val t = pedirToken()) {
                    is TokenSocial.Error -> t.mensaje?.let { _error.value = ErrorSocial(titulo, it) }
                    is TokenSocial.Ok -> {
                        _fase.value = FaseGoogle.Servidor
                        val r = if (p == Proveedor.Apple) auth.entrarConApple(t.idToken, t.nonce) else auth.entrarConGoogle(t.idToken, t.nonce)
                        when (r) {
                            is ResultadoAuth.Ok -> { _fase.value = FaseGoogle.Perfil; alEntrar() }
                            is ResultadoAuth.Error -> _error.value = ErrorSocial(titulo, r.mensaje.ifBlank { "No se pudo iniciar sesión con ${p.nombre}" })
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorSocial(titulo, "No se pudo iniciar sesión con ${p.nombre} (${e::class.simpleName}: ${e.message})")
            } finally { _fase.value = null }
        }
    }
}

/** Ajustes del usuario (preferencias, privacidad, notificaciones). */
class AjustesViewModel(private val repo: AjustesRepository) : ViewModel() {
    val ajustes: StateFlow<Ajustes> = repo.ajustes.stateIn(viewModelScope, SharingStarted.Eagerly, Ajustes())
    fun actualizar(transformar: (Ajustes) -> Ajustes) { viewModelScope.launch { repo.actualizar(transformar) } }
}
