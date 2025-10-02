package com.sebas.tiendaropa.ui.products

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebas.tiendaropa.data.db.AppDatabase
import com.sebas.tiendaropa.data.entity.ProductEntity
import com.sebas.tiendaropa.data.repo.ProductsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductsViewModel(private val repo: ProductsRepository) : ViewModel() {

    // ---- Búsqueda (igual a CategoriesViewModel) ----
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    fun setQuery(q: String) { _query.value = q }

    val products = repo.products.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val visibleProducts: StateFlow<List<ProductEntity>> =
        _query
            .debounce(200)
            .flatMapLatest { q ->
                if (q.isBlank()) repo.products else repo.search(q.trim())
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    // ---- Edición (igual a CategoriesViewModel) ----
    private val _editing = MutableStateFlow<ProductEntity?>(null)
    val editing: StateFlow<ProductEntity?> = _editing.asStateFlow()

    fun startEdit(p: ProductEntity) { _editing.value = p }
    fun clearEdit() { _editing.value = null }

    // ---- Altas/Bajas/Cambios ----
    fun saveNew(
        name: String,
        description: String?,
        avisos: String?,
        categoryId: Long,       // obligatorio
        valorCompraPesos: String,
        valorVentaPesos: String
    ) = viewModelScope.launch {
        val compra = pesosToCents(valorCompraPesos)
        val venta  = pesosToCents(valorVentaPesos)
        if (name.isNotBlank() && compra != null && venta != null) {
            repo.add(name.trim(),
                description?.takeIf { it.isNotBlank() },
                avisos?.takeIf { it.isNotBlank() },
                categoryId,
                compra, venta
            )
        }
    }

    fun saveEdit(
        id: Long,
        name: String,
        description: String?,
        avisos: String?,
        categoryId: Long,
        valorCompraPesos: String,
        valorVentaPesos: String
    ) = viewModelScope.launch {
        val compra = pesosToCents(valorCompraPesos)
        val venta  = pesosToCents(valorVentaPesos)
        if (name.isNotBlank() && compra != null && venta != null) {
            repo.update(id, name.trim(),
                description?.takeIf { it.isNotBlank() },
                avisos?.takeIf { it.isNotBlank() },
                categoryId,
                compra, venta
            )
        }
    }

    fun remove(p: ProductEntity) = viewModelScope.launch { repo.remove(p) }

    // ---- Totales útiles para el dashboard ----
    val totalCompras: StateFlow<Long> =
        repo.observeTotalCompras().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L
        )

    val totalGanancia: StateFlow<Long> =
        repo.observeTotalGanancia().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L
        )

    // ---- Utils ----
    private fun pesosToCents(txt: String): Long? {
        // admite "12000", "12.000", "12,000" -> deja solo dígitos
        val digits = txt.filter { it.isDigit() }
        if (digits.isBlank()) return null
        return digits.toLong() * 100L
    }

    companion object {
        fun factory(ctx: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.get(ctx)
                val repo = ProductsRepository(db.productDao())
                @Suppress("UNCHECKED_CAST")
                return ProductsViewModel(repo) as T
            }
        }
    }
}

