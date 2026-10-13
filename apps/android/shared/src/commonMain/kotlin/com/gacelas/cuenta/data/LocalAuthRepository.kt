package com.gacelas.cuenta.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.cuenta.domain.modelo.Perfil
import com.gacelas.cuenta.domain.modelo.ResultadoAuth
import com.gacelas.cuenta.domain.reglas.ValidacionAuth
import com.gacelas.cuenta.domain.repositorio.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class CuentaLocal(val perfil: Perfil, val hash: String)

@Serializable
data class DatosAuth(val cuentas: Map<String, CuentaLocal> = emptyMap(), val sesionEmail: String? = null)

/** Autenticacion local: guarda cuentas y sesion en DataStore. La contrasena se guarda como hash SHA-256. */
class LocalAuthRepository(store: DataStore<Preferences>) : AuthRepository {
    private val datos = ValorJson(store, "auth", DatosAuth.serializer(), DatosAuth())

    override val sesion: Flow<Perfil?> = datos.flujo.map { d -> d.sesionEmail?.let { d.cuentas[it]?.perfil } }

    override suspend fun iniciarSesion(email: String, password: String): ResultadoAuth {
        ValidacionAuth.email(email)?.let { return ResultadoAuth.Error(it) }
        if (password.isEmpty()) return ResultadoAuth.Error("Ingresa tu contraseña")
        val clave = email.trim().lowercase()
        val cuenta = datos.flujo.first().cuentas[clave]
        if (cuenta == null || cuenta.hash != hash(clave, password)) return ResultadoAuth.Error("Correo o contraseña incorrectos")
        datos.actualizar { it.copy(sesionEmail = clave) }
        return ResultadoAuth.Ok(cuenta.perfil)
    }

    override suspend fun registrar(nombre: String, email: String, password: String): ResultadoAuth {
        (ValidacionAuth.nombre(nombre) ?: ValidacionAuth.email(email) ?: ValidacionAuth.password(password))?.let { return ResultadoAuth.Error(it) }
        val clave = email.trim().lowercase()
        if (datos.flujo.first().cuentas.containsKey(clave)) return ResultadoAuth.Error("Ya existe una cuenta con ese correo")
        val perfil = Perfil(clave, nombre.trim(), ValidacionAuth.usernameDesde(nombre, clave))
        datos.actualizar { it.copy(cuentas = it.cuentas + (clave to CuentaLocal(perfil, hash(clave, password))), sesionEmail = clave) }
        return ResultadoAuth.Ok(perfil)
    }

    override suspend fun recuperarContrasena(email: String): ResultadoAuth {
        ValidacionAuth.email(email)?.let { return ResultadoAuth.Error(it) }
        // Sin servidor de correo: se responde igual exista o no la cuenta, para no revelar correos registrados.
        return ResultadoAuth.Ok(Perfil(email.trim().lowercase(), "", ""))
    }

    override suspend fun actualizarPerfil(perfil: Perfil) {
        datos.actualizar { d ->
            val cuenta = d.cuentas[perfil.email] ?: return@actualizar d
            d.copy(cuentas = d.cuentas + (perfil.email to cuenta.copy(perfil = perfil)))
        }
    }

    override suspend fun cerrarSesion() = datos.actualizar { it.copy(sesionEmail = null) }

    private fun hash(email: String, password: String): String =
        com.gacelas.shared.sha256Hex("gacelas:$email:$password")

}
