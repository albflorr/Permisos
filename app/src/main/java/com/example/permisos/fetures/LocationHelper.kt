package com.example.permisos.fetures

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

object LocationHelper {
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(context: Context, onResult: (String) -> Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            onResult("Permisos de ubicación no otorgados")
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)

            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val formatted = String.format(
                        Locale.US,
                        "Latitud: %.6f\n Longitud: %.6f",
                        location.latitude,
                        location.longitude
                    )
                    onResult(formatted)
                } else {
                    val cancellationTokenSource = CancellationTokenSource()
                    fusedClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { currentLocation ->
                        if (currentLocation != null) {
                            val formatted = String.format(
                                Locale.US,
                                "Latitud: %.6f\n Longitud: %.6f",
                                currentLocation.latitude,
                                currentLocation.longitude
                            )
                            onResult(formatted)
                        } else {
                            onResult("Ubicación nula. Ve a los Extended Controls del emulador")
                        }
                    }.addOnFailureListener { e ->
                        onResult("Error al consultar las coordenadas: ${e.localizedMessage}")
                    }
                }
            }

        } catch (e: SecurityException) {
            onResult("Error de seguridad: ${e.localizedMessage}")
        } catch (e: Exception) {
            onResult("Error inesperado: ${e.localizedMessage}")
        }
    }
}