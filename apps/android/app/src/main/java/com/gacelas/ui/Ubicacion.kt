package com.gacelas.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.gacelas.core.location.LocationService
import com.gacelas.rutas.ultimaUbicacion
import com.gacelas.ui.components.ModalC2
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

/**
 * Devuelve una accion que consigue la ubicacion actual: pide el permiso si falta, abre el
 * dialogo nativo de Google "Activar ubicacion" (sale desde abajo) si el GPS esta apagado y
 * entrega la posicion. Sin permiso definitivo ofrece abrir los ajustes del telefono.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberPedirUbicacion(
    onFallo: (String) -> Unit = { com.gacelas.ui.components.AvisosC2.mostrar(it) },
    onLista: (Pair<Double, Double>) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val lista by rememberUpdatedState(onLista)
    val fallo by rememberUpdatedState(onFallo)
    var denegado by remember { mutableStateOf(false) }

    val obtener = {
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { l ->
                val p = l?.let { it.latitude to it.longitude } ?: ultimaUbicacion(context)
                if (p != null) lista(p) else fallo("No pudimos obtener tu ubicación. Intenta de nuevo al aire libre.")
            }
            .addOnFailureListener { ultimaUbicacion(context)?.let(lista) ?: fallo("No pudimos obtener tu ubicación.") }
        Unit
    }
    val resolver = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) obtener() else fallo("Activa la ubicación para continuar.")
    }
    val verificarGps = {
        val pedido = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000).build())
            .setAlwaysShow(true)
            .build()
        LocationServices.getSettingsClient(context).checkLocationSettings(pedido)
            .addOnSuccessListener { obtener() }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    runCatching { resolver.launch(IntentSenderRequest.Builder(e.resolution).build()) }
                        .onFailure { fallo("Activa la ubicación para continuar.") }
                } else obtener()
            }
        Unit
    }
    val permisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (LocationService.tienePermiso(context)) verificarGps() else denegado = true
    }

    if (denegado) ModalC2(
        icono = Icons.Filled.LocationOff, titulo = "Permiso de ubicación",
        texto = "Necesitamos tu ubicación para mostrarte dónde estás y las rutas cercanas. Actívala en los ajustes del teléfono.",
        primario = "Abrir ajustes", secundario = "Ahora no", tag = "modal-permiso-ubicacion",
        onCerrar = { denegado = false },
        onPrimario = {
            denegado = false
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        },
    )

    return remember {
        {
            if (LocationService.tienePermiso(context)) verificarGps()
            else permisos.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}
