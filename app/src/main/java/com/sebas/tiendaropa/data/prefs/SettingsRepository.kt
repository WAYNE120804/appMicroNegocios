package com.sebas.tiendaropa.data.prefs


import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DashboardPeriodType { TODAY, LAST_7_DAYS, THIS_MONTH, CUSTOM }

data class SettingsState(
    val storeName: String = "CloudStore",
    val ownerName: String = "Usuario",
    val logoUri: String? = null,
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val pinHash: String? = null,
    val securityQuestion: String? = null,
    val securityAnswerHash: String? = null,
    val dashboardPeriod: DashboardPeriodType = DashboardPeriodType.LAST_7_DAYS,
    val dashboardCustomStartMillis: Long? = null,
    val dashboardCustomEndMillis: Long? = null,
)

class SettingsRepository(private val context: Context) {

    val state: Flow<SettingsState> = context.dataStore.data.map { p ->
        SettingsState(
            storeName = p[StorePrefsKeys.STORE_NAME] ?: "CloudStore",
            ownerName = p[StorePrefsKeys.OWNER_NAME] ?: "Usuario",
            logoUri = p[StorePrefsKeys.LOGO_URI],
            pinEnabled = p[StorePrefsKeys.PIN_ENABLED] ?: false,
            biometricEnabled = p[StorePrefsKeys.BIOMETRIC_ENABLED] ?: false,
            pinHash = p[StorePrefsKeys.PIN_HASH],
            securityQuestion = p[StorePrefsKeys.SECURITY_Q],
            securityAnswerHash = p[StorePrefsKeys.SECURITY_A_HASH],
            dashboardPeriod =
                p[StorePrefsKeys.HOME_DASHBOARD_PERIOD]
                    ?.let { stored ->
                        runCatching { DashboardPeriodType.valueOf(stored) }.getOrNull()
                    }
                    ?: DashboardPeriodType.LAST_7_DAYS,
            dashboardCustomStartMillis =
                p[StorePrefsKeys.HOME_DASHBOARD_CUSTOM_START],
            dashboardCustomEndMillis =
                p[StorePrefsKeys.HOME_DASHBOARD_CUSTOM_END],
        )
    }

    suspend fun setStoreName(v: String) = context.dataStore.edit { it[StorePrefsKeys.STORE_NAME] = v }
    suspend fun setOwnerName(v: String) = context.dataStore.edit { it[StorePrefsKeys.OWNER_NAME] = v }
    suspend fun setLogoUri(v: String?) = context.dataStore.edit {
        if (v == null) it.remove(StorePrefsKeys.LOGO_URI) else it[StorePrefsKeys.LOGO_URI] = v
    }
    suspend fun setPinEnabled(v: Boolean) = context.dataStore.edit { it[StorePrefsKeys.PIN_ENABLED] = v }
    suspend fun setBiometricEnabled(v: Boolean) = context.dataStore.edit { it[StorePrefsKeys.BIOMETRIC_ENABLED] = v }

    suspend fun setPinHash(hash: String) = context.dataStore.edit { it[StorePrefsKeys.PIN_HASH] = hash }
    suspend fun clearPinHash() = context.dataStore.edit { it.remove(StorePrefsKeys.PIN_HASH) }
    suspend fun setSecurityQuestion(q: String?) = context.dataStore.edit {
        if (q == null) {
            it.remove(StorePrefsKeys.SECURITY_Q)
        } else {
            it[StorePrefsKeys.SECURITY_Q] = q
        }
    }
    suspend fun setSecurityAnswerHash(hash: String) = context.dataStore.edit {
        it[StorePrefsKeys.SECURITY_A_HASH] = hash
    }
    suspend fun clearSecurityAnswer() = context.dataStore.edit {
        it.remove(StorePrefsKeys.SECURITY_A_HASH)
    }

    suspend fun setDashboardPeriod(
        period: DashboardPeriodType,
        customStartMillis: Long?,
        customEndMillis: Long?,
    ) = context.dataStore.edit { prefs ->
        prefs[StorePrefsKeys.HOME_DASHBOARD_PERIOD] = period.name
        if (customStartMillis != null) {
            prefs[StorePrefsKeys.HOME_DASHBOARD_CUSTOM_START] = customStartMillis
        } else {
            prefs.remove(StorePrefsKeys.HOME_DASHBOARD_CUSTOM_START)
        }
        if (customEndMillis != null) {
            prefs[StorePrefsKeys.HOME_DASHBOARD_CUSTOM_END] = customEndMillis
        } else {
            prefs.remove(StorePrefsKeys.HOME_DASHBOARD_CUSTOM_END)
        }
    }
}
