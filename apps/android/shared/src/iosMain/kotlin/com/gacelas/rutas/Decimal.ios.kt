package com.gacelas.rutas

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterRoundHalfUp
import platform.Foundation.currentLocale

actual fun decimalLocal(x: Double): String = NSNumberFormatter().apply {
    locale = NSLocale.currentLocale
    minimumFractionDigits = 1u
    maximumFractionDigits = 1u
    usesGroupingSeparator = false
    roundingMode = NSNumberFormatterRoundHalfUp
}.stringFromNumber(NSNumber(double = x)) ?: x.toString()
