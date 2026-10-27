package com.gacelas.ui.components

import androidx.compose.runtime.Composable

@Composable
internal actual fun AtrasHojaC2(onAtras: () -> Unit) = androidx.activity.compose.BackHandler(onBack = onAtras)
