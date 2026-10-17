package com.gacelas.shared

import platform.Foundation.NSProcessInfo

actual fun relojMonotonoMs(): Long = (NSProcessInfo.processInfo.systemUptime * 1000).toLong()

actual fun logAviso(etiqueta: String, mensaje: String) = println("[$etiqueta] $mensaje")
