package com.sebas.tiendaropa.data.prefs


import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

object StorePrefsKeys {
    val STORE_NAME = stringPreferencesKey("store_name")
    val OWNER_NAME = stringPreferencesKey("owner_name")
    val LOGO_URI = stringPreferencesKey("logo_uri")
    val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
    val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    // (dejamos listos para más tarde)
    val PIN_HASH = stringPreferencesKey("pin_hash")
    val SECURITY_Q = stringPreferencesKey("security_q")
    val SECURITY_A_HASH = stringPreferencesKey("security_a_hash")
    val HOME_DASHBOARD_PERIOD = stringPreferencesKey("home_dashboard_period")
    val HOME_DASHBOARD_CUSTOM_START = longPreferencesKey("home_dashboard_custom_start")
    val HOME_DASHBOARD_CUSTOM_END = longPreferencesKey("home_dashboard_custom_end")
}


val Context.dataStore by preferencesDataStore(name = "cloudstore_prefs")
