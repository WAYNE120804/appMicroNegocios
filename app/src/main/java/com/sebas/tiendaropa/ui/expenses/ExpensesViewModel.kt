package com.sebas.tiendaropa.ui.expenses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebas.tiendaropa.data.dao.ExpenseWithCategory
import com.sebas.tiendaropa.data.db.AppDatabase
import com.sebas.tiendaropa.data.entity.ExpenseEntity
import com.sebas.tiendaropa.data.entity.ExpenseCategoryEntity
import com.sebas.tiendaropa.data.repo.ExpenseCategoriesRepository
import com.sebas.tiendaropa.data.repo.ExpensesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpensesViewModel(
    private val expensesRepository: ExpensesRepository,
    private val categoriesRepository: ExpenseCategoriesRepository
) : ViewModel() {

    val expenses: StateFlow<List<ExpenseWithCategory>> = expensesRepository.expenses.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val totalAmountCents: StateFlow<Long> = expensesRepository.observeTotalAmount().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        0L
    )

    val categories: StateFlow<List<ExpenseCategoryEntity>> = categoriesRepository.categories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val visibleExpenses: StateFlow<List<ExpenseWithCategory>> = combine(
        expenses,
        _query.debounce(200)
    ) { expenses, query ->
        if (query.isBlank()) {
            expenses
        } else {
            val q = query.trim()
            expenses.filter { item ->
                item.expense.concept.contains(q, ignoreCase = true) ||
                    (item.expense.description?.contains(q, ignoreCase = true) == true) ||
                    item.expense.paymentMethod.contains(q, ignoreCase = true) ||
                    (item.category?.name?.contains(q, ignoreCase = true) == true)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun setQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun createExpense(
        concept: String,
        amountCents: Long?,
        dateMillis: Long,
        categoryId: Long?,
        paymentMethod: String,
        description: String?,
        photoUri: String?
    ) = viewModelScope.launch {
        val amount = amountCents ?: return@launch
        if (concept.isBlank() || paymentMethod.isBlank() || amount <= 0) return@launch
        expensesRepository.add(
            concept = concept,
            amountCents = amount,
            dateMillis = dateMillis,
            categoryId = categoryId,
            paymentMethod = paymentMethod,
            description = description,
            photoUri = photoUri
        )
    }

    fun updateExpense(
        id: Long,
        concept: String,
        amountCents: Long?,
        dateMillis: Long,
        categoryId: Long?,
        paymentMethod: String,
        description: String?,
        photoUri: String?
    ) = viewModelScope.launch {
        val amount = amountCents ?: return@launch
        if (concept.isBlank() || paymentMethod.isBlank() || amount <= 0) return@launch
        expensesRepository.update(
            id = id,
            concept = concept,
            amountCents = amount,
            dateMillis = dateMillis,
            categoryId = categoryId,
            paymentMethod = paymentMethod,
            description = description,
            photoUri = photoUri
        )
    }

    fun deleteExpense(expense: ExpenseEntity) = viewModelScope.launch {
        expensesRepository.remove(expense)
    }

    fun addCategory(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) categoriesRepository.add(name)
    }

    fun updateCategory(id: Long, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) categoriesRepository.update(id, name)
    }

    companion object {
        fun factory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.get(context)
                val expensesRepo = ExpensesRepository(db.expenseDao())
                val categoriesRepo = ExpenseCategoriesRepository(db.expenseCategoryDao())
                @Suppress("UNCHECKED_CAST")
                return ExpensesViewModel(expensesRepo, categoriesRepo) as T
            }
        }
    }
}
