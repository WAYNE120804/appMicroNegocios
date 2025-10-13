package com.sebas.tiendaropa.data.repo

import com.sebas.tiendaropa.data.dao.ExpenseDao
import com.sebas.tiendaropa.data.dao.ExpenseWithCategory
import com.sebas.tiendaropa.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpensesRepository(private val dao: ExpenseDao) {

    val expenses: Flow<List<ExpenseWithCategory>> = dao.observeAll()

    fun search(query: String): Flow<List<ExpenseWithCategory>> = dao.search("%${query}%")

    fun observeTotalAmount(): Flow<Long> = dao.observeTotalAmount()

    suspend fun add(
        concept: String,
        amountCents: Long,
        dateMillis: Long,
        categoryId: Long?,
        paymentMethod: String,
        description: String?,
        photoUri: String?
    ): Long = dao.insert(
        ExpenseEntity(
            concept = concept.trim(),
            amountCents = amountCents,
            dateMillis = dateMillis,
            categoryId = categoryId,
            paymentMethod = paymentMethod.trim(),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            photoUri = photoUri
        )
    )

    suspend fun update(
        id: Long,
        concept: String,
        amountCents: Long,
        dateMillis: Long,
        categoryId: Long?,
        paymentMethod: String,
        description: String?,
        photoUri: String?
    ) = dao.update(
        ExpenseEntity(
            id = id,
            concept = concept.trim(),
            amountCents = amountCents,
            dateMillis = dateMillis,
            categoryId = categoryId,
            paymentMethod = paymentMethod.trim(),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            photoUri = photoUri
        )
    )

    suspend fun remove(entity: ExpenseEntity) = dao.delete(entity)
}
