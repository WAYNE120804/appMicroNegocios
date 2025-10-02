package com.sebas.tiendaropa.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebas.tiendaropa.data.prefs.SettingsRepository
import com.sebas.tiendaropa.data.prefs.SettingsState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    val state: StateFlow<SettingsState> =
        repo.state.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    fun setStoreName(v: String) = viewModelScope.launch { repo.setStoreName(v) }
    fun setOwnerName(v: String) = viewModelScope.launch { repo.setOwnerName(v) }
    fun setLogoUri(v: String?) = viewModelScope.launch { repo.setLogoUri(v) }
    fun setPinEnabled(v: Boolean) = viewModelScope.launch { repo.setPinEnabled(v) }
    fun setBiometricEnabled(v: Boolean) = viewModelScope.launch { repo.setBiometricEnabled(v) }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repo = SettingsRepository(context)
                    return SettingsViewModel(repo) as T
                }
            }
    }
}
