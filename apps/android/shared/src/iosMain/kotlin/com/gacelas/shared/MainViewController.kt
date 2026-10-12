package com.gacelas.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Punto de entrada de iOS: SwiftUI (iosApp) muestra este UIViewController de Compose. */
fun MainViewController(): UIViewController = ComposeUIViewController { InicioComun() }
