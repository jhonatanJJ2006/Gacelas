package com.gacelas.shared

import platform.UIKit.UIDevice

actual object Plataforma {
    actual val nombre: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}
