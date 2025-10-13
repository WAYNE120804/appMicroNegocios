package com.sebas.tiendaropa.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebas.tiendaropa.data.prefs.SettingsRepository
import com.sebas.tiendaropa.data.prefs.SettingsState
import com.sebas.tiendaropa.util.SecurityUtils
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
    fun setPinEnabled(v: Boolean) = viewModelScope.launch {
        repo.setPinEnabled(v)
        if (!v) {
            repo.setBiometricEnabled(false)
            repo.clearPinHash()
        }
    }
    fun setBiometricEnabled(v: Boolean) = viewModelScope.launch {
        if (v) {
            val current = state.value
            if (current.pinHash != null) {
                repo.setBiometricEnabled(true)
            } else {
                repo.setBiometricEnabled(false)
            }
        } else {
            repo.setBiometricEnabled(false)
        }
    }

    fun savePin(pin: String) = viewModelScope.launch {
        repo.setPinHash(SecurityUtils.sha256(pin))
    }

    fun clearPin() = viewModelScope.launch { repo.clearPinHash() }

    fun setSecurityQuestion(question: String?) = viewModelScope.launch {
        repo.setSecurityQuestion(question)
        if (question == null) {
            repo.clearSecurityAnswer()
        }
    }

    fun saveSecurityAnswer(answer: String) = viewModelScope.launch {
        val normalized = answer.trim().lowercase()
        repo.setSecurityAnswerHash(SecurityUtils.sha256(normalized))
    }

    fun clearSecurityAnswer() = viewModelScope.launch { repo.clearSecurityAnswer() }

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
