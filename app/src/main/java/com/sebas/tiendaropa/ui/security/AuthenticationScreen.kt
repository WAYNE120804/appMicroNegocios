package com.sebas.tiendaropa.ui.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.sebas.tiendaropa.data.prefs.SettingsState
import com.sebas.tiendaropa.util.SecurityUtils
import kotlinx.coroutines.delay

private enum class AuthStep { PinEntry, SecurityQuestion, ResetPin }

@Composable
fun AuthenticationScreen(
    state: SettingsState,
    onUnlock: () -> Unit,
    onResetPin: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var authStep by rememberSaveable { mutableStateOf(AuthStep.PinEntry) }
    var pinValue by rememberSaveable { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var recoveryAnswer by rememberSaveable { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }
    var newPinValue by rememberSaveable { mutableStateOf("") }
    var newPinConfirm by rememberSaveable { mutableStateOf("") }
    var resetError by remember { mutableStateOf<String?>(null) }

    val onUnlockState = rememberUpdatedState(onUnlock)

    val canUseBiometric = remember(state.biometricEnabled, state.pinHash, activity) {
        if (!state.biometricEnabled || state.pinHash == null || activity == null) {
            false
        } else {
            val manager = BiometricManager.from(activity)
            val authResult = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            authResult == BiometricManager.BIOMETRIC_SUCCESS
        }
    }

    var biometricMessage by remember { mutableStateOf<String?>(null) }
    var biometricAttempted by rememberSaveable { mutableStateOf(false) }
    var biometricOptOut by rememberSaveable { mutableStateOf(false) }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear CloudStore")
            .setSubtitle("Usa tu huella para continuar")
            .setNegativeButtonText("Usar PIN")
            .build()
    }

    val biometricPrompt = remember(activity) {
        if (activity == null) {
            null
        } else {
            val executor = ContextCompat.getMainExecutor(activity)
            BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    biometricMessage = null
                    biometricAttempted = false
                    onUnlockState.value.invoke()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        biometricOptOut = true
                        biometricMessage = null
                    } else {
                        biometricMessage = errString.toString()
                    }
                    biometricAttempted = false
                }

                override fun onAuthenticationFailed() {
                    biometricMessage = "No se reconoció la huella, intenta nuevamente."
                }
            })
        }
    }

    LaunchedEffect(canUseBiometric, biometricAttempted, biometricOptOut) {
        if (canUseBiometric && biometricPrompt != null && !biometricAttempted && !biometricOptOut) {
            // Pequeño retraso para evitar lanzar el prompt antes de que la vista esté lista
            delay(200)
            biometricAttempted = true
            biometricPrompt.authenticate(promptInfo)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Seguridad", style = MaterialTheme.typography.headlineMedium)

        if (canUseBiometric) {
            Text("Puedes desbloquear con tu huella o con tu PIN de 4 dígitos.")
            Button(
                onClick = {
                    if (biometricPrompt != null) {
                        biometricOptOut = false
                        biometricAttempted = true
                        biometricMessage = null
                        biometricPrompt.authenticate(promptInfo)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Intentar con huella") }
        }

        biometricMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        when (authStep) {
            AuthStep.PinEntry -> {
                if (!state.pinEnabled) {
                    Text(
                        "Configura un PIN en la sección de Configuración para usarlo como respaldo de la biometría.",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = pinValue,
                    onValueChange = {
                        if (it.length <= 4) pinValue = it.filter { ch -> ch.isDigit() }
                    },
                    label = { Text("PIN de 4 dígitos") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )

                pinError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                infoMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

                Button(
                    onClick = {
                        pinError = null
                        infoMessage = null
                        val expected = state.pinHash
                        when {
                            pinValue.length != 4 -> pinError = "El PIN debe tener 4 números"
                            expected == null -> pinError = "Aún no se ha configurado un PIN en Configuración"
                            SecurityUtils.sha256(pinValue) != expected -> pinError = "PIN incorrecto"
                            else -> {
                                pinValue = ""
                                onUnlockState.value.invoke()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.pinEnabled
                ) { Text("Desbloquear") }

                TextButton(
                    onClick = {
                        if (state.securityQuestion != null && state.securityAnswerHash != null) {
                            authStep = AuthStep.SecurityQuestion
                            pinValue = ""
                            pinError = null
                            infoMessage = null
                            recoveryAnswer = ""
                            recoveryError = null
                            newPinValue = ""
                            newPinConfirm = ""
                            resetError = null
                        } else {
                            pinError = "Configura una pregunta de seguridad en Configuración para recuperar tu PIN"
                        }
                    }
                ) { Text("Olvidé mi PIN") }
            }

            AuthStep.SecurityQuestion -> {
                Text("Responde tu pregunta de seguridad para crear un nuevo PIN.")
                Text(state.securityQuestion ?: "Pregunta no configurada", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = recoveryAnswer,
                    onValueChange = { recoveryAnswer = it.take(60) },
                    label = { Text("Respuesta") },
                    modifier = Modifier.fillMaxWidth()
                )

                recoveryError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Button(
                    onClick = {
                        val expected = state.securityAnswerHash
                        when {
                            recoveryAnswer.isBlank() -> recoveryError = "Escribe tu respuesta"
                            expected == null -> recoveryError = "No hay respuesta guardada. Configúrala en Ajustes"
                            SecurityUtils.sha256(recoveryAnswer.trim().lowercase()) != expected -> recoveryError = "Respuesta incorrecta"
                            else -> {
                                recoveryError = null
                                authStep = AuthStep.ResetPin
                                newPinValue = ""
                                newPinConfirm = ""
                                resetError = null
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Validar respuesta") }

                TextButton(onClick = { authStep = AuthStep.PinEntry }) {
                    Text("Volver al PIN")
                }
            }

            AuthStep.ResetPin -> {
                Text("Crea un nuevo PIN de 4 dígitos.")

                OutlinedTextField(
                    value = newPinValue,
                    onValueChange = {
                        if (it.length <= 4) newPinValue = it.filter { ch -> ch.isDigit() }
                    },
                    label = { Text("Nuevo PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )

                OutlinedTextField(
                    value = newPinConfirm,
                    onValueChange = {
                        if (it.length <= 4) newPinConfirm = it.filter { ch -> ch.isDigit() }
                    },
                    label = { Text("Confirmar PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )

                resetError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Button(
                    onClick = {
                        resetError = null
                        when {
                            newPinValue.isBlank() || newPinConfirm.isBlank() -> resetError = "Completa ambos campos"
                            newPinValue != newPinConfirm -> resetError = "Los PIN no coinciden"
                            !SecurityUtils.isFourDigitPin(newPinValue) -> resetError = "El PIN debe tener exactamente 4 números"
                            else -> {
                                onResetPin(newPinValue)
                                authStep = AuthStep.PinEntry
                                infoMessage = "PIN actualizado, vuelve a iniciar sesión"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar nuevo PIN") }

                TextButton(onClick = { authStep = AuthStep.PinEntry }) {
                    Text("Cancelar")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Si tienes problemas para acceder, pide ayuda al administrador de la aplicación.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
