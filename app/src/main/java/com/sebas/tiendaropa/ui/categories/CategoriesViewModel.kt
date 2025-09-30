package com.sebas.tiendaropa.ui.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebas.tiendaropa.data.db.AppDatabase

import com.sebas.tiendaropa.data.entity.CategoryEntity
import com.sebas.tiendaropa.data.repo.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class CategoriesViewModel (private val repo: CategoryRepository) : ViewModel() {

    private val _query = kotlinx.coroutines.flow.MutableStateFlow("")
    val query: kotlinx.coroutines.flow.StateFlow<String> = _query

    fun setQuery(q: String) { _query.value = q }

    val categories = repo.categories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val visibleCategories: StateFlow<List<CategoryEntity>> =
        _query
            .debounce(200)
            .flatMapLatest { q ->
                if (q.isBlank()) repo.categories else repo.search(q.trim())
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )


    private val _editing = kotlinx.coroutines.flow.MutableStateFlow<CategoryEntity?>(null)
    val editing = _editing.asStateFlow()

    fun startEdit(c: CategoryEntity) { _editing.value = c }

    fun clearEdit() { _editing.value = null }

    fun saveNew(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repo.add(name)
    }

    fun saveEdit(id: Long, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repo.update(id, name)
    }

    fun remove(c: CategoryEntity) = viewModelScope.launch { repo.remove(c) }

companion object{
    fun factory(ctx: Context) = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.get(ctx)
            val repo = CategoryRepository(db.categoryDao())
            @Suppress("UNCHECKED_CAST")
            return CategoriesViewModel(repo) as T
        }
    }

    }
}




