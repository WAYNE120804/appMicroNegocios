package com.sebas.tiendaropa.data.repo

import com.sebas.tiendaropa.data.dao.SaleDao
import com.sebas.tiendaropa.data.dao.SaleWithDetails
import com.sebas.tiendaropa.data.entity.PaymentEntity
import com.sebas.tiendaropa.data.entity.SaleEntity
import com.sebas.tiendaropa.data.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

class SalesRepository(private val saleDao: SaleDao) {

    val sales: Flow<List<SaleWithDetails>> = saleDao.observeSales()

    suspend fun createSale(
        customerId: Long,
        totalCents: Long,
        items: List<SaleItemEntity>
    ): Long {
        val now = System.currentTimeMillis()
        val sale = SaleEntity(
            customerId = customerId,
            createdAtMillis = now,
            totalCents = totalCents
        )
        return saleDao.insertSaleWithItems(sale, items)
    }

    suspend fun registerPayment(saleId: Long, amountCents: Long, description: String?) {
        if (amountCents <= 0) return
        val payment = PaymentEntity(
            saleId = saleId,
            amountCents = amountCents,
            createdAtMillis = System.currentTimeMillis(),
            description = description?.ifBlank { null }
        )
        saleDao.insertPayment(payment)
    }

    suspend fun deleteSale(details: SaleWithDetails) {
        saleDao.deleteSale(details.sale)
    }

    suspend fun totalPaymentsFor(saleId: Long): Long = saleDao.totalPaymentsForSale(saleId)
}