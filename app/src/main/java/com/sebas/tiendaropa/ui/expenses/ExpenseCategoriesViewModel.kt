package com.sebas.tiendaropa.ui.expenses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebas.tiendaropa.data.db.AppDatabase
import com.sebas.tiendaropa.data.entity.ExpenseCategoryEntity
import com.sebas.tiendaropa.data.repo.ExpenseCategoriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseCategoriesViewModel(
    private val repository: ExpenseCategoriesRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val categories = repository.categories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val visibleCategories: StateFlow<List<ExpenseCategoryEntity>> = _query
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) repository.categories else repository.search(query.trim())
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    private val _editing = MutableStateFlow<ExpenseCategoryEntity?>(null)
    val editing: StateFlow<ExpenseCategoryEntity?> = _editing.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
    }

    fun startEdit(category: ExpenseCategoryEntity) {
        _editing.value = category
    }

    fun clearEdit() {
        _editing.value = null
    }

    fun saveNew(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.add(name)
    }

    fun saveEdit(id: Long, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.update(id, name)
    }

    fun remove(category: ExpenseCategoryEntity) = viewModelScope.launch {
        repository.remove(category)
    }

    companion object {
        fun factory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.get(context)
                val repo = ExpenseCategoriesRepository(db.expenseCategoryDao())
                @Suppress("UNCHECKED_CAST")
                return ExpenseCategoriesViewModel(repo) as T
            }
        }
    }
}
