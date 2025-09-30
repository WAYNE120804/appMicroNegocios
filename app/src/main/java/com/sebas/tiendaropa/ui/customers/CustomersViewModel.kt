package com.sebas.tiendaropa.ui.customers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebas.tiendaropa.data.db.AppDatabase
import com.sebas.tiendaropa.data.entity.CustomerEntity
import com.sebas.tiendaropa.data.repo.CustomersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomersViewModel(private val repo: CustomersRepository) : ViewModel() {

    val customers = repo.customers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    // Cliente seleccionado para edición (null = modo "nuevo")
    private val _editing = MutableStateFlow<CustomerEntity?>(null)
    val editing = _editing.asStateFlow()

    fun startEdit(c: CustomerEntity) { _editing.value = c }
    fun clearEdit() { _editing.value = null }

    fun saveNew(name: String, address: String?, phone: String?) = viewModelScope.launch {
        if (name.isNotBlank()) repo.add(name, address, phone)
    }

    fun saveEdit(id: Long, name: String, address: String?, phone: String?) = viewModelScope.launch {
        if (name.isNotBlank()) repo.update(id, name, address, phone)
    }

    fun remove(c: CustomerEntity) = viewModelScope.launch { repo.remove(c) }

    companion object {
        fun factory(ctx: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.get(ctx)
                val repo = CustomersRepository(db.customerDao())
                @Suppress("UNCHECKED_CAST")
                return CustomersViewModel(repo) as T
            }
        }
    }
}
