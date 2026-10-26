package com.gacelas.acceso

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Pantallas de acceso C4 compartidas por Android e iOS (mismo codigo = mismo diseño).
 * No tienen estado de sesion: cada plataforma pone la logica (Supabase, Google/Apple) y el
 * boton de Google como slot.
 */

/** Bienvenida C4. [version] es el texto "Versión x (n)" de cada app. */
@Composable
fun BienvenidaC4(
    version: String,
    onSaltar: () -> Unit,
    onCrearCuenta: () -> Unit,
    onIniciarSesion: () -> Unit,
    botonGoogle: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(C4.Fondo).testTag("pantalla-bienvenida")) {
        val compacta = maxHeight < AlturasAcceso.BIENVENIDA_COMPACTA
        FondoMapaC4(login = false)
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = if (compacta) 8.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarcaC4()
                Spacer(Modifier.weight(1f))
                ChipC4(PortadaRuta.etiqueta())
                Text("Saltar", style = C4Texto.labelCaps, color = C4.Secondary, maxLines = 1, softWrap = false,
                    modifier = Modifier.padding(start = 4.dp).clip(CircleShape).clickable(onClick = onSaltar).padding(8.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().padding(start = 20.dp, end = 20.dp, bottom = 12.dp), contentAlignment = Alignment.BottomStart) {
                IndicadorZonaC4()
            }
            TarjetaC4(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(top = 8.dp, bottom = if (compacta) 12.dp else 24.dp), padding = if (compacta) 20.dp else 24.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(C4.Magenta))
                    Spacer(Modifier.width(6.dp))
                    Text("RUNNING EN LOJA, ECUADOR", style = C4Texto.labelCaps, color = C4.Magenta)
                }
                Spacer(Modifier.height(12.dp))
                TitularC4(compacto = compacta)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Únete a la comunidad de runners en Loja. Explora rutas andinas, registra tus marcas y corre acompañado.",
                    style = C4Texto.bodyMd, color = C4.Secondary.copy(alpha = 0.9f),
                    maxLines = if (compacta) 2 else 4, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(if (compacta) 16.dp else 24.dp))
                BotonC4("Empezar", onCrearCuenta)
                Spacer(Modifier.height(12.dp))
                botonGoogle()
                PieC4("¿Ya tienes cuenta? ", "Inicia sesión", onIniciarSesion)
                Text(version, style = C4Texto.bodySm.copy(fontSize = 10.sp), color = C4.Secondary.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("version-app"), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

/** Inicio de sesion C4 (sin scroll; se adapta por niveles de altura al abrirse el teclado). */
@Composable
fun InicioSesionC4(
    email: String,
    password: String,
    error: String?,
    cargando: Boolean,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onEntrar: () -> Unit,
    onAtras: () -> Unit,
    onRecuperar: () -> Unit,
    onRegistro: () -> Unit,
    botonGoogle: @Composable () -> Unit,
    onLegal: ((terminos: Boolean) -> Unit)? = null,
) {
    // Sin scroll: el teclado (imePadding) achica el hueco y la pantalla se adapta por niveles.
    // El mapa es fondo de ventana completa (no se mueve con el teclado), igual que en el export de Stitch.
    BoxWithConstraints(Modifier.fillMaxSize().background(C4.Fondo).testTag("pantalla-login")) {
        FondoMapaC4(login = true)
        BoxWithConstraints(Modifier.fillMaxSize().imePadding().cerrarTecladoAlTocarFuera()) {
            val compacto = maxHeight < AlturasAcceso.LOGIN_COMPACTO
            val minimo = maxHeight < AlturasAcceso.LOGIN_MINIMO
            Column(Modifier.fillMaxSize()) {
                if (!minimo) {
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = if (compacto) 8.dp else 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AtrasC4(onAtras)
                        Spacer(Modifier.width(12.dp))
                        MarcaC4()
                        Spacer(Modifier.weight(1f))
                        ChipC4(PortadaRuta.etiqueta())
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().padding(start = 20.dp, end = 20.dp, bottom = 12.dp), contentAlignment = Alignment.BottomStart) {
                    if (!compacto) IndicadorZonaC4()
                }
                TarjetaC4(
                    Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(top = 8.dp, bottom = if (compacto) 8.dp else 24.dp),
                    alpha = 0.96f, padding = if (minimo) 16.dp else if (compacto) 20.dp else 24.dp,
                ) {
                    Text("Bienvenido de vuelta", style = C4Texto.headlineLgMobile.copy(fontSize = if (compacto) 20.sp else 24.sp), color = Color.White)
                    if (!compacto) {
                        Spacer(Modifier.height(6.dp))
                        Text("Inicia sesión para seguir corriendo", style = C4Texto.bodySm, color = C4.Subtitulo.copy(alpha = 0.8f))
                    }
                    Spacer(Modifier.height(if (minimo) 10.dp else if (compacto) 14.dp else 20.dp))
                    CampoC4("Correo electrónico", email, onEmail, "tu@email.com", IconosC4.Correo,
                        tipo = KeyboardType.Email, mostrarEtiqueta = !minimo)
                    Spacer(Modifier.height(if (minimo) 10.dp else 16.dp))
                    CampoC4("Contraseña", password, onPassword, "••••••••", IconosC4.Candado,
                        password = true, mostrarEtiqueta = !minimo, onListo = onEntrar,
                        accion = if (minimo) null else ({
                            Text("¿Olvidaste tu contraseña?", style = C4Texto.labelCaps.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp), color = C4.Enlace,
                                modifier = Modifier.clickable(onClick = onRecuperar))
                        }))
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, style = C4Texto.bodySm, color = Color(0xFFFFB4AB), modifier = Modifier.testTag("error-acceso"))
                    }
                    if (minimo) {
                        Text("¿Olvidaste tu contraseña?", style = C4Texto.labelCaps.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp), color = C4.Enlace,
                            modifier = Modifier.align(Alignment.End).padding(top = 8.dp).clickable(onClick = onRecuperar))
                    }
                    Spacer(Modifier.height(if (minimo) 10.dp else 20.dp))
                    BotonC4(if (cargando) "Ingresando..." else "Iniciar sesión", onEntrar, alto = 48.dp, tamTexto = 16, enabled = !cargando)
                    if (!compacto) {
                        SeparadorC4()
                        botonGoogle()
                        PieC4("¿No tienes cuenta? ", "Crear cuenta", onRegistro, arriba = 10.dp)
                        if (onLegal != null) Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                            Text("Términos", fontSize = 12.sp, color = C4.Secondary, modifier = Modifier.clickable { onLegal(true) }.padding(6.dp).testTag("pie-terminos"))
                            Text("Privacidad", fontSize = 12.sp, color = C4.Secondary, modifier = Modifier.clickable { onLegal(false) }.padding(6.dp).testTag("pie-privacidad"))
                        }
                    }
                }
            }
        }
    }
}
