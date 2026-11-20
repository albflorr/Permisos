package com.example.permisos.ui


import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.permisos.fetures.AudioHelper
import com.example.permisos.fetures.ContactsHelper
import com.example.permisos.fetures.LocationHelper
import com.example.permisos.permissions.PermissionStatus
import com.example.permisos.ui.components.PermissionCard

private const val PREFS_NAME = "permisos_app_prefs"
private const val KEY_CONTACTS_REQUESTED = "contacts_requested"
private const val KEY_LOCATION_REQUESTED = "location_requested"
private const val KEY_AUDIO_REQUESTED = "audio_requested"

/**
 * Pantalla principal de "Voice & Context Memo".
 * Administra el ciclo de vida de los permisos, las tarjetas interactivas y la observación de ON_RESUME.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Preferencias locales para persistir si una solicitud previa ya fue realizada
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var contactsRequested by remember { mutableStateOf(prefs.getBoolean(KEY_CONTACTS_REQUESTED, false)) }
    var locationRequested by remember { mutableStateOf(prefs.getBoolean(KEY_LOCATION_REQUESTED, false)) }
    var audioRequested by remember { mutableStateOf(prefs.getBoolean(KEY_AUDIO_REQUESTED, false)) }

    // Evaluación dinámica de los 4 estados de permisos
    var contactsStatus by remember { mutableStateOf(PermissionStatus.evaluate(context, Manifest.permission.READ_CONTACTS, contactsRequested)) }
    var locationStatus by remember { mutableStateOf(PermissionStatus.evaluate(context, Manifest.permission.ACCESS_FINE_LOCATION, locationRequested)) }
    var audioStatus by remember { mutableStateOf(PermissionStatus.evaluate(context, Manifest.permission.RECORD_AUDIO, audioRequested)) }

    // Campo de nota de texto
    var memoText by remember { mutableStateOf("Reunión de demostración sobre permisos en Android") }

    // Resultados devueltos por los helpers de características
    var contactsResult by remember { mutableStateOf("No se ha consultado ningún contacto aún.") }
    var locationResult by remember { mutableStateOf("No se han obtenido coordenadas GPS aún.") }
    var audioResult by remember { mutableStateOf("Sin nota de voz grabada.") }

    // Administrador de grabación y reproducción de audio
    val audioHelper = remember { AudioHelper(context) }
    var isRecording by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(3) }
    var isPlayingAudio by remember { mutableStateOf(false) }

    // Diálogos educativos (Rationale / Permanently Denied)
    var activeRationalePermission by remember { mutableStateOf<String?>(null) }
    var activeRationaleMessage by remember { mutableStateOf("") }
    var showBlockedDialogPermission by remember { mutableStateOf<String?>(null) }

    // Función para refrescar el estado de todos los permisos
    val refreshPermissionStates = {
        contactsStatus = PermissionStatus.evaluate(context, Manifest.permission.READ_CONTACTS, contactsRequested)
        locationStatus = PermissionStatus.evaluate(context, Manifest.permission.ACCESS_FINE_LOCATION, locationRequested)
        audioStatus = PermissionStatus.evaluate(context, Manifest.permission.RECORD_AUDIO, audioRequested)
    }

    // Registra que una solicitud ha sido lanzada
    val markRequested = { permission: String ->
        when (permission) {
            Manifest.permission.READ_CONTACTS -> {
                contactsRequested = true
                prefs.edit().putBoolean(KEY_CONTACTS_REQUESTED, true).apply()
            }
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                locationRequested = true
                prefs.edit().putBoolean(KEY_LOCATION_REQUESTED, true).apply()
            }
            Manifest.permission.RECORD_AUDIO -> {
                audioRequested = true
                prefs.edit().putBoolean(KEY_AUDIO_REQUESTED, true).apply()
            }
        }
        refreshPermissionStates()
    }

    // Re-evaluar automáticamente los permisos al volver de Ajustes o segundo plano (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audioHelper.stopRecording()
            audioHelper.stopPlayback()
        }
    }

    // Launchers para permisos individuales
    val requestContactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        markRequested(Manifest.permission.READ_CONTACTS)
    }

    val requestLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        markRequested(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val requestAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        markRequested(Manifest.permission.RECORD_AUDIO)
    }

    // Launcher múltiple para el botón "Activar Todo"
    val requestAllLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        markRequested(Manifest.permission.READ_CONTACTS)
        markRequested(Manifest.permission.ACCESS_FINE_LOCATION)
        markRequested(Manifest.permission.RECORD_AUDIO)
    }

    // Abre la configuración del sistema para la app en Ajustes
    val openAppSettings = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    // Manejador central de peticiones de permiso según estado
    val handlePermissionRequest = { permission: String ->
        val currentStatus = PermissionStatus.evaluate(
            context, permission,
            when (permission) {
                Manifest.permission.READ_CONTACTS -> contactsRequested
                Manifest.permission.ACCESS_FINE_LOCATION -> locationRequested
                Manifest.permission.RECORD_AUDIO -> audioRequested
                else -> false
            }
        )

        when (currentStatus) {
            PermissionStatus.GRANTED -> {
                // Ya concedido
            }
            PermissionStatus.NOT_REQUESTED -> {
                when (permission) {
                    Manifest.permission.READ_CONTACTS -> requestContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                    Manifest.permission.ACCESS_FINE_LOCATION -> requestLocationLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                    Manifest.permission.RECORD_AUDIO -> requestAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            PermissionStatus.RATIONALE_REQUIRED -> {
                activeRationalePermission = permission
                activeRationaleMessage = when (permission) {
                    Manifest.permission.READ_CONTACTS ->
                        "Necesitamos acceso a tus Contactos para asociar el primer contacto de tu agenda a la nota."
                    Manifest.permission.ACCESS_FINE_LOCATION ->
                        "La ubicación nos permite guardar las coordenadas GPS (Latitud y Longitud) junto con la nota."
                    Manifest.permission.RECORD_AUDIO ->
                        "El permiso de Micrófono es indispensable para grabar un audio explicativo de 3 segundos."
                    else -> "Se requiere el permiso para continuar."
                }
            }
            PermissionStatus.PERMANENTLY_DENIED -> {
                showBlockedDialogPermission = permission
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Voice & Context Memo",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nota de texto
            OutlinedTextField(
                value = memoText,
                onValueChange = { memoText = it },
                label = { Text("Nota Rápida / Memoria de Contexto") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            // Botón "Activar Todo"
            Button(
                onClick = {
                    requestAllLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Activar Todos los Permisos", fontWeight = FontWeight.Bold)
            }

            // 1. Tarjeta READ_CONTACTS
            PermissionCard(
                featureTitle = "1. Contacto de Contexto",
                permissionName = "READ_CONTACTS",
                icon = Icons.Default.Contacts,
                status = contactsStatus,
                resultText = contactsResult,
                onRequestPermission = { handlePermissionRequest(Manifest.permission.READ_CONTACTS) },
                onOpenSettings = openAppSettings,
                onExecuteAction = {
                    contactsResult = ContactsHelper.getFirstContact(context)
                },
                actionButtonText = "Obtener Contacto"
            )

            // 2. Tarjeta ACCESS_FINE_LOCATION
            PermissionCard(
                featureTitle = "2. Ubicación Geográfica",
                permissionName = "ACCESS_FINE_LOCATION",
                icon = Icons.Default.MyLocation,
                status = locationStatus,
                resultText = locationResult,
                onRequestPermission = { handlePermissionRequest(Manifest.permission.ACCESS_FINE_LOCATION) },
                onOpenSettings = openAppSettings,
                onExecuteAction = {
                    locationResult = "Obteniendo coordenadas GPS..."
                    LocationHelper.fetchCurrentLocation(context) { result ->
                        locationResult = result
                    }
                },
                actionButtonText = "Obtener Coordenadas"
            )

            // 3. Tarjeta RECORD_AUDIO
            PermissionCard(
                featureTitle = "3. Memoria de Voz (3s)",
                permissionName = "RECORD_AUDIO",
                icon = Icons.Default.Mic,
                status = audioStatus,
                resultText = if (isRecording) "🎙️ Grabando audio... Quedan $countdownSeconds s" else audioResult,
                onRequestPermission = { handlePermissionRequest(Manifest.permission.RECORD_AUDIO) },
                onOpenSettings = openAppSettings,
                onExecuteAction = {
                    isRecording = true
                    audioHelper.start4SecondsRecording(
                        scope = scope,
                        onProgress = { seconds -> countdownSeconds = seconds },
                        onFinished = { _, message ->
                            isRecording = false
                            audioResult = message
                        }
                    )
                },
                actionButtonText = if (isRecording) "Grabando ($countdownSeconds s)..." else "Grabar Clip (3s)",
                isActionLoading = isRecording,
                extraActionButton = {
                    if (audioStatus == PermissionStatus.GRANTED && audioHelper.isAudioAvailable()) {
                        Button(
                            onClick = {
                                if (isPlayingAudio) {
                                    audioHelper.stopPlayback()
                                    isPlayingAudio = false
                                } else {
                                    audioHelper.playAudio(
                                        onPlaybackStarted = { isPlayingAudio = true },
                                        onPlaybackFinished = { isPlayingAudio = false },
                                        onError = { err ->
                                            isPlayingAudio = false
                                            audioResult = err
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlayingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlayingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPlayingAudio) "Detener" else "Reproducir")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Diálogo AlertDialog para Explicación de Rationale
    activeRationalePermission?.let { permission ->
        AlertDialog(
            onDismissRequest = { activeRationalePermission = null },
            icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
            title = { Text("Explicación del Permiso (Rationale)") },
            text = { Text(activeRationaleMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        activeRationalePermission = null
                        when (permission) {
                            Manifest.permission.READ_CONTACTS -> requestContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            Manifest.permission.ACCESS_FINE_LOCATION -> requestLocationLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                            Manifest.permission.RECORD_AUDIO -> requestAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Text("Entendido, Solicitar")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeRationalePermission = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo AlertDialog para Permisos Bloqueados Permanentemente
    showBlockedDialogPermission?.let { _ ->
        AlertDialog(
            onDismissRequest = { showBlockedDialogPermission = null },
            icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
            title = { Text("Permiso Bloqueado Permanentemente") },
            text = {
                Text(
                    "Has denegado este permiso en múltiples ocasiones ('No volver a preguntar'). " +
                            "Para continuar, debes otorgarlo manualmente desde los Ajustes del sistema."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockedDialogPermission = null
                        openAppSettings()
                    }
                ) {
                    Text("Abrir Ajustes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockedDialogPermission = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
