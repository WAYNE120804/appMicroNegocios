package com.sebas.tiendaropa.ui.settings

import android.content.Intent
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.sebas.tiendaropa.data.prefs.SettingsState
import com.sebas.tiendaropa.util.DataExportManager
import com.sebas.tiendaropa.util.SecurityUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onSetStoreName: (String) -> Unit,
    onSetOwnerName: (String) -> Unit,
    onSetLogoUri: (String?) -> Unit,
    onSetPinEnabled: (Boolean) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSavePin: (String) -> Unit,
    onSetSecurityQuestion: (String?) -> Unit,
    onSaveSecurityAnswer: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val applicationContext = context.applicationContext
    val dataExportManager = remember(applicationContext) { DataExportManager(applicationContext) }

    var exportingBackup by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }

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

    var pinNotice by remember { mutableStateOf<String?>(null) }
    var biometricNotice by remember { mutableStateOf<String?>(null) }
    var pinSetupRequested by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.pinHash) {
        if (state.pinHash != null) {
            pinNotice = null
            biometricNotice = null
            pinSetupRequested = false
        }
    }

    LaunchedEffect(state.pinEnabled) {
        if (!state.pinEnabled) {
            pinNotice = null
            pinSetupRequested = false
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
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
            Switch(
                checked = state.pinEnabled || pinSetupRequested,
                onCheckedChange = { enabled ->
                    pinNotice = null
                    biometricNotice = null
                    if (enabled) {
                        if (state.pinHash == null) {
                            pinSetupRequested = true
                            pinNotice = "Configura tu PIN de 4 dígitos para activarlo."
                        } else {
                            onSetPinEnabled(true)
                        }
                    } else {
                        onSetBiometricEnabled(false)
                        onSetPinEnabled(false)
                        pinNotice = "PIN desactivado. Se eliminó el PIN configurado."
                        pinSetupRequested = false
                    }
                }
            )
        }
        pinNotice?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Biometría activada")
            Switch(
                checked = state.biometricEnabled,
                onCheckedChange = { enabled ->
                    biometricNotice = null
                    if (enabled) {
                        if (state.pinHash != null && state.pinEnabled) {
                            onSetBiometricEnabled(true)
                        } else {
                            if (!state.pinEnabled) {
                                pinSetupRequested = true
                                pinNotice = "Configura tu PIN de 4 dígitos para activar la biometría."
                            }
                            biometricNotice = "Configura un PIN de 4 dígitos antes de activar la biometría."
                        }
                    } else {
                        onSetBiometricEnabled(false)
                    }
                }
            )
        }
        biometricNotice?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }

        if (state.pinEnabled || pinSetupRequested) {
            Divider()

            var pinOne by rememberSaveable { mutableStateOf("") }
            var pinTwo by rememberSaveable { mutableStateOf("") }
            var pinError by remember { mutableStateOf<String?>(null) }
            var pinSuccess by remember { mutableStateOf<String?>(null) }

            Text(
                "Configura tu PIN de 4 dígitos. Este PIN se usa como respaldo si la biometría falla.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = pinOne,
                onValueChange = {
                    if (it.length <= 4) pinOne = it.filter { ch -> ch.isDigit() }
                },
                label = { Text("Nuevo PIN (4 dígitos)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = pinTwo,
                onValueChange = {
                    if (it.length <= 4) pinTwo = it.filter { ch -> ch.isDigit() }
                },
                label = { Text("Confirmar PIN") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation()
            )

            pinError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            pinSuccess?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Button(
                onClick = {
                    pinSuccess = null
                    pinNotice = null
                    biometricNotice = null
                    when {
                        pinOne.isBlank() || pinTwo.isBlank() ->
                            pinError = "Ingresa y confirma tu PIN"
                        pinOne != pinTwo ->
                            pinError = "Los PIN ingresados no coinciden"
                        !SecurityUtils.isFourDigitPin(pinOne) ->
                            pinError = "El PIN debe tener exactamente 4 números"
                        else -> {
                            onSavePin(pinOne)
                            onSetPinEnabled(true)
                            pinSetupRequested = false
                            pinError = null
                            pinSuccess = "PIN actualizado correctamente"
                            pinOne = ""
                            pinTwo = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar PIN")
            }

            Spacer(Modifier.height(8.dp))

            val baseQuestions = listOf(
                "¿Cuál es el nombre de tu primera mascota?",
                "¿En qué ciudad naciste?",
                "¿Cuál es el nombre de tu escuela primaria?",
                "¿Cuál es tu comida favorita?",
                "¿En qué año te graduaste de la secundaria?"
            )

            var questionExpanded by remember { mutableStateOf(false) }
            var selectedQuestion by rememberSaveable { mutableStateOf(state.securityQuestion ?: baseQuestions.first()) }
            var answer by rememberSaveable { mutableStateOf("") }
            var securityError by remember { mutableStateOf<String?>(null) }
            var securitySuccess by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(state.securityQuestion) {
                val newQuestion = state.securityQuestion
                if (newQuestion != null) {
                    selectedQuestion = newQuestion
                }
            }

            Text(
                "Configura una pregunta de seguridad para restablecer el PIN en caso de olvido.",
                style = MaterialTheme.typography.bodyMedium
            )

            ExposedDropdownMenuBox(
                expanded = questionExpanded,
                onExpandedChange = { questionExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedQuestion,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text("Pregunta de seguridad") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = questionExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = questionExpanded,
                    onDismissRequest = { questionExpanded = false }
                ) {
                    baseQuestions.forEach { question ->
                        DropdownMenuItem(
                            text = { Text(question) },
                            onClick = {
                                selectedQuestion = question
                                questionExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it.take(60) },
                label = { Text("Respuesta (no se distingue mayúsculas/minúsculas)") },
                modifier = Modifier.fillMaxWidth()
            )

            securityError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            securitySuccess?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Button(
                onClick = {
                    securitySuccess = null
                    when {
                        answer.isBlank() -> securityError = "Escribe una respuesta para la pregunta seleccionada"
                        else -> {
                            onSetSecurityQuestion(selectedQuestion)
                            onSaveSecurityAnswer(answer)
                            securityError = null
                            securitySuccess = "Pregunta de seguridad guardada"
                            answer = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar pregunta de seguridad")
            }
        }

        Spacer(Modifier.height(8.dp))

        Divider()

        Text("Respaldo de datos", style = MaterialTheme.typography.titleMedium)

        Button(
            onClick = {
                coroutineScope.launch {
                    exportingBackup = true
                    exportError = null
                    try {
                        val backupFile = dataExportManager.createBackupZip()
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            backupFile
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            putExtra(Intent.EXTRA_SUBJECT, "Respaldo de datos")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Respaldo generado desde la app tiendaRopa."
                            )
                        }
                        val chooser = Intent.createChooser(shareIntent, "Compartir respaldo")
                        if (shareIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(chooser)
                        } else {
                            exportError = "No hay aplicaciones disponibles para compartir el respaldo."
                            Toast.makeText(
                                context,
                                "No hay aplicaciones disponibles para compartir el respaldo.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (t: Throwable) {
                        Log.e("SettingsScreen", "Error generando respaldo", t)
                        exportError = "No se pudo generar el respaldo."
                        Toast.makeText(
                            context,
                            "No se pudo generar el respaldo.",
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        exportingBackup = false
                    }
                }
            },
            enabled = !exportingBackup,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (exportingBackup) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Generando respaldo...")
            } else {
                Text("Exportar información")
            }
        }

        exportError?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
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
