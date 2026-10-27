package com.gacelas.ui.components

import androidx.compose.runtime.Composable

/** iOS no tiene boton atras del sistema; la hoja se cierra tocando el velo o sus acciones. */
@Composable
internal actual fun AtrasHojaC2(onAtras: () -> Unit) = Unit
