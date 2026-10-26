package com.gacelas.identidad

import com.gacelas.acceso.FaseGoogle
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.identidad.domain.modelo.Perfil
import com.gacelas.identidad.domain.modelo.ResultadoAuth
import com.gacelas.identidad.domain.reglas.ValidacionAuth
import com.gacelas.identidad.domain.repositorio.AjustesRepository
import com.gacelas.identidad.domain.repositorio.AuthRepository
import com.gacelas.identidad.presentation.AccesoSocialViewModel
import com.gacelas.identidad.presentation.AjustesViewModel
import com.gacelas.identidad.presentation.LoginViewModel
import com.gacelas.identidad.presentation.Proveedor
import com.gacelas.identidad.presentation.RecuperarViewModel
import com.gacelas.identidad.presentation.RegistroViewModel
import com.gacelas.identidad.presentation.TokenSocial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class AuthFalso(override val googleDisponible: Boolean = true) : AuthRepository {
    override val sesion = MutableStateFlow<Perfil?>(null)
    var ultimoToken: String? = null
    private fun ok(email: String) = ResultadoAuth.Ok(Perfil(email, "Ana", "ana"))
    override suspend fun iniciarSesion(email: String, password: String) =
        if (password == "clave123") ok(email) else ResultadoAuth.Error("Correo o contraseña incorrectos")
    override suspend fun registrar(nombre: String, email: String, password: String) =
        (ValidacionAuth.nombre(nombre) ?: ValidacionAuth.email(email) ?: ValidacionAuth.password(password))?.let { ResultadoAuth.Error(it) } ?: ok(email)
    override suspend fun recuperarContrasena(email: String) = ValidacionAuth.email(email)?.let { ResultadoAuth.Error(it) } ?: ok(email)
    override suspend fun actualizarPerfil(perfil: Perfil) {}
    override suspend fun cerrarSesion() {}
    override suspend fun entrarConGoogle(idToken: String, nonce: String?): ResultadoAuth { ultimoToken = idToken; return if (idToken == "malo") ResultadoAuth.Error("") else ok("g@x.com") }
    override suspend fun entrarConApple(idToken: String, nonce: String?): ResultadoAuth { ultimoToken = "apple:$idToken"; return ok("a@x.com") }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CuentaTest {
    @BeforeTest fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun despues() = Dispatchers.resetMain()

    @Test fun loginErrorSeLimpiaAlEscribirYEntra() = runTest {
        val vm = LoginViewModel(AuthFalso())
        vm.email("ana@x.com"); vm.password("mala")
        var entro = false
        vm.entrar { entro = true }
        assertEquals("Correo o contraseña incorrectos", vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
        vm.password("clave123")
        assertNull(vm.estado.value.error)
        vm.entrar { entro = true }
        assertTrue(entro)
    }

    @Test fun registroValidaYCrea() = runTest {
        val vm = RegistroViewModel(AuthFalso())
        vm.nombre("A"); vm.crear {}
        assertEquals("Ingresa tu nombre", vm.estado.value.error)
        vm.nombre("Ana"); vm.email("ana@x.com"); vm.password("clave123")
        var creada: Perfil? = null
        vm.crear { creada = it }
        assertEquals("ana@x.com", creada?.email)
    }

    @Test fun recuperar() = runTest {
        val vm = RecuperarViewModel(AuthFalso())
        vm.email("no-es-correo"); vm.enviar()
        assertEquals("El correo no es válido", vm.estado.value.error)
        vm.email("ana@x.com"); vm.enviar()
        assertTrue(vm.estado.value.enviado)
        assertTrue(RecuperarViewModel(AuthFalso(), enviadoInicial = true).estado.value.enviado)
    }

    @Test fun socialGoogleAppleYErrores() = runTest {
        val auth = AuthFalso()
        val vm = AccesoSocialViewModel(auth)
        var entradas = 0
        vm.entrar(Proveedor.Google, pedirToken = { TokenSocial.Ok("tok", "n") }, alEntrar = { entradas++ })
        assertEquals(1, entradas); assertEquals("tok", auth.ultimoToken); assertNull(vm.fase.value)
        vm.entrar(Proveedor.Apple, pedirToken = { TokenSocial.Ok("t2", "n") }, alEntrar = { entradas++ })
        assertEquals("apple:t2", auth.ultimoToken)
        vm.entrar(Proveedor.Google, pedirToken = { TokenSocial.Error(null) }, alEntrar = {})
        assertNull(vm.error.value)
        vm.entrar(Proveedor.Google, pedirToken = { TokenSocial.Ok("malo", null) }, alEntrar = {})
        assertEquals("No se pudo iniciar sesión con Google", vm.error.value?.mensaje)
        vm.cerrarError(); assertNull(vm.error.value)
        AccesoSocialViewModel(AuthFalso(googleDisponible = false)).also { s ->
            s.entrar(Proveedor.Apple, pedirToken = { error("no se pide") }, alEntrar = {})
            assertEquals("No se pudo entrar con Apple", s.error.value?.titulo)
            assertEquals(null, s.fase.value as FaseGoogle?)
        }
    }

    @Test fun ajustes() = runTest {
        val repo = object : AjustesRepository {
            override val ajustes = MutableStateFlow(Ajustes())
            override suspend fun actualizar(transformar: (Ajustes) -> Ajustes) { ajustes.value = transformar(ajustes.value) }
        }
        val vm = AjustesViewModel(repo)
        vm.actualizar { it.copy(metaSemanal = 5) }
        assertEquals(5, vm.ajustes.value.metaSemanal)
    }
}
