package com.sebas.tiendaropa.ui.settings

import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sebas.tiendaropa.data.prefs.SettingsState

@Composable
fun SettingsScreen(
    state: SettingsState,
    onSetStoreName: (String) -> Unit,
    onSetOwnerName: (String) -> Unit,
    onSetLogoUri: (String?) -> Unit,
    onSetPinEnabled: (Boolean) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit
) {
    val context = LocalContext.current

    // Estado local para editar sin saltos de cursor
    var storeNameLocal by rememberSaveable { mutableStateOf(state.storeName) }
    var ownerNameLocal by rememberSaveable { mutableStateOf(state.ownerName) }

    // Sincroniza cuando el estado externo cambia
    LaunchedEffect(state.storeName) { storeNameLocal = state.storeName }
    LaunchedEffect(state.ownerName) { ownerNameLocal = state.ownerName }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // Conserva permiso para leer el logo en reinicios
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* ya tenía permiso */ }
            onSetLogoUri(it.toString())
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Configuración", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = storeNameLocal,
            onValueChange = { storeNameLocal = it },   // <-- CORREGIDO
            label = { Text("Nombre de la tienda") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ownerNameLocal,
            onValueChange = { ownerNameLocal = it },   // <-- CORREGIDO
            label = { Text("Tu nombre") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { pickImage.launch(arrayOf("image/*")) }) { Text("Cambiar logo") }
            TextButton(onClick = { onSetLogoUri(null) }) { Text("Quitar logo") }
        }

        state.logoUri?.let {
            AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(72.dp))
        }

        Divider()

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("PIN activado")
            Switch(checked = state.pinEnabled, onCheckedChange = onSetPinEnabled)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Biometría activada")
            Switch(checked = state.biometricEnabled, onCheckedChange = onSetBiometricEnabled)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                onSetStoreName(storeNameLocal.trim())
                onSetOwnerName(ownerNameLocal.trim())
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar cambios") }
    }
}
