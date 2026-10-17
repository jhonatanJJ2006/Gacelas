package com.gacelas.correr

import androidx.compose.runtime.Composable

/** Gesto/boton atras del sistema (Android); en iOS no hay boton atras del sistema. */
@Composable
expect fun ManejarAtras(enabled: Boolean = true, onAtras: () -> Unit)
