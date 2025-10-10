package com.sebas.tiendaropa.ui.sales

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebas.tiendaropa.data.db.AppDatabase
import com.sebas.tiendaropa.data.entity.CategoryEntity
import com.sebas.tiendaropa.data.entity.CustomerEntity
import com.sebas.tiendaropa.data.entity.ProductEntity
import com.sebas.tiendaropa.data.entity.SaleItemEntity
import com.sebas.tiendaropa.data.repo.CategoryRepository
import com.sebas.tiendaropa.data.repo.CustomersRepository
import com.sebas.tiendaropa.data.repo.ProductsRepository
import com.sebas.tiendaropa.data.repo.SalesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SalesViewModel(
    private val salesRepository: SalesRepository,
    private val productsRepository: ProductsRepository,
    private val customersRepository: CustomersRepository,
    private val categoriesRepository: CategoryRepository
) : ViewModel() {

    val sales = salesRepository.sales.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val products = productsRepository.products.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val customers = customersRepository.customers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val categories: StateFlow<List<CategoryEntity>> = categoriesRepository.categories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private val _draft = MutableStateFlow(SaleDraft())
    val draft: StateFlow<SaleDraft> = _draft.asStateFlow()

    private val _step = MutableStateFlow(AddSaleStep.PRODUCTS)
    val step: StateFlow<AddSaleStep> = _step.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isSavingPayment = MutableStateFlow(false)
    val isSavingPayment: StateFlow<Boolean> = _isSavingPayment.asStateFlow()

    val draftTotalCents: StateFlow<Long> = _draft
        .map { draft ->
            draft.items.sumOf { it.product.valorVentaCents * it.quantity }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val draftCanContinueToCustomer: StateFlow<Boolean> = _draft
        .map { it.items.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val draftCanFinish: StateFlow<Boolean> = _draft
        .map { it.items.isNotEmpty() && it.customer != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun addProduct(product: ProductEntity) {
        val current = _draft.value
        val updated = current.items.toMutableList()
        val index = updated.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = updated[index]
            updated[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            updated.add(SaleDraftItem(product = product, quantity = 1))
        }
        _draft.value = current.copy(items = updated)
    }

    fun updateQuantity(productId: Long, quantity: Int) {
        if (quantity <= 0) {
            removeProduct(productId)
            return
        }
        val current = _draft.value
        val updated = current.items.map {
            if (it.product.id == productId) it.copy(quantity = quantity) else it
        }
        _draft.value = current.copy(items = updated)
    }

    fun removeProduct(productId: Long) {
        val current = _draft.value
        val updated = current.items.filterNot { it.product.id == productId }
        _draft.value = current.copy(items = updated)
        if (updated.isEmpty()) {
            _step.value = AddSaleStep.PRODUCTS
        }
    }

    fun selectCustomer(customer: CustomerEntity) {
        _draft.value = _draft.value.copy(customer = customer)
    }

    fun clearCustomer() {
        _draft.value = _draft.value.copy(customer = null)
    }

    fun goToCustomerStep() {
        if (draftCanContinueToCustomer.value) {
            _step.value = AddSaleStep.CUSTOMER
        }
    }

    fun goToReview() {
        if (draftCanFinish.value) {
            _step.value = AddSaleStep.REVIEW
        }
    }

    fun back() {
        _step.value = when (_step.value) {
            AddSaleStep.PRODUCTS -> AddSaleStep.PRODUCTS
            AddSaleStep.CUSTOMER -> AddSaleStep.PRODUCTS
            AddSaleStep.REVIEW -> AddSaleStep.CUSTOMER
        }
    }

    fun resetDraft() {
        _draft.value = SaleDraft()
        _step.value = AddSaleStep.PRODUCTS
    }

    fun createProduct(
        name: String,
        description: String?,
        avisos: String?,
        categoryId: Long,
        valorCompraPesos: String,
        valorVentaPesos: String
    ) = viewModelScope.launch {
        val compra = pesosToCents(valorCompraPesos)
        val venta = pesosToCents(valorVentaPesos)
        if (name.isBlank() || compra == null || venta == null) return@launch
        productsRepository.add(
            name = name.trim(),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            avisos = avisos?.trim()?.takeIf { it.isNotBlank() },
            categoryId = categoryId,
            compraCents = compra,
            ventaCents = venta
        )
    }

    fun createCustomer(name: String, address: String?, phone: String?) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        customersRepository.add(
            name = name.trim(),
            address = address?.trim()?.takeIf { it.isNotBlank() },
            phone = phone?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    fun addPayment(saleId: Long, amountPesos: String, onSuccess: () -> Unit = {}) {
        val amountCents = pesosToCents(amountPesos)
        if (amountCents == null || amountCents <= 0) return
        viewModelScope.launch {
            _isSavingPayment.value = true
            try {
                salesRepository.registerPayment(saleId, amountCents)
                onSuccess()
            } finally {
                _isSavingPayment.value = false
            }
        }
    }

    fun confirmSale(onSuccess: (Long) -> Unit = {}) {
        val draftSnapshot = _draft.value
        val customer = draftSnapshot.customer ?: return
        val items = draftSnapshot.items
        if (items.isEmpty()) return
        val saleItems = items.map {
            SaleItemEntity(
                saleId = 0,
                productId = it.product.id,
                quantity = it.quantity,
                unitPriceCents = it.product.valorVentaCents
            )
        }
        val total = saleItems.sumOf { it.unitPriceCents * it.quantity }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val saleId = salesRepository.createSale(
                    customerId = customer.id,
                    totalCents = total,
                    items = saleItems
                )
                onSuccess(saleId)
                resetDraft()
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun pesosToCents(txt: String): Long? {
        val digits = txt.filter { it.isDigit() }
        if (digits.isBlank()) return null
        return digits.toLong() * 100L
    }

    companion object {
        fun factory(ctx: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.get(ctx)
                val sales = SalesRepository(db.saleDao())
                val products = ProductsRepository(db.productDao())
                val customers = CustomersRepository(db.customerDao())
                val categories = CategoryRepository(db.categoryDao())
                @Suppress("UNCHECKED_CAST")
                return SalesViewModel(sales, products, customers, categories) as T
            }
        }
    }
}

data class SaleDraft(
    val items: List<SaleDraftItem> = emptyList(),
    val customer: CustomerEntity? = null
)

data class SaleDraftItem(
    val product: ProductEntity,
    val quantity: Int
)

enum class AddSaleStep { PRODUCTS, CUSTOMER, REVIEW }