package com.sebas.tiendaropa.data.prefs


import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SettingsState(
    val storeName: String = "CloudStore",
    val ownerName: String = "Usuario",
    val logoUri: String? = null,
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false
)

class SettingsRepository(private val context: Context) {

    val state: Flow<SettingsState> = context.dataStore.data.map { p ->
        SettingsState(
            storeName = p[StorePrefsKeys.STORE_NAME] ?: "CloudStore",
            ownerName = p[StorePrefsKeys.OWNER_NAME] ?: "Usuario",
            logoUri = p[StorePrefsKeys.LOGO_URI],
            pinEnabled = p[StorePrefsKeys.PIN_ENABLED] ?: false,
            biometricEnabled = p[StorePrefsKeys.BIOMETRIC_ENABLED] ?: false
        )
    }

    suspend fun setStoreName(v: String) = context.dataStore.edit { it[StorePrefsKeys.STORE_NAME] = v }
    suspend fun setOwnerName(v: String) = context.dataStore.edit { it[StorePrefsKeys.OWNER_NAME] = v }
    suspend fun setLogoUri(v: String?) = context.dataStore.edit {
        if (v == null) it.remove(StorePrefsKeys.LOGO_URI) else it[StorePrefsKeys.LOGO_URI] = v
    }
    suspend fun setPinEnabled(v: Boolean) = context.dataStore.edit { it[StorePrefsKeys.PIN_ENABLED] = v }
    suspend fun setBiometricEnabled(v: Boolean) = context.dataStore.edit { it[StorePrefsKeys.BIOMETRIC_ENABLED] = v }

    // Hooks para más adelante (PIN/seguridad)
    suspend fun setPinHash(hash: String) = context.dataStore.edit { it[StorePrefsKeys.PIN_HASH] = hash }
    suspend fun setSecurityQuestion(q: String) = context.dataStore.edit { it[StorePrefsKeys.SECURITY_Q] = q }
    suspend fun setSecurityAnswerHash(hash: String) = context.dataStore.edit { it[StorePrefsKeys.SECURITY_A_HASH] = hash }
}
