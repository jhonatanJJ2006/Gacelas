package com.gacelas.correr

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun ManejarAtras(enabled: Boolean, onAtras: () -> Unit) = BackHandler(enabled, onAtras)
