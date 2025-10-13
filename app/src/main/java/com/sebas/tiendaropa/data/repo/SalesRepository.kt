package com.sebas.tiendaropa.data.repo

import com.sebas.tiendaropa.data.dao.SaleDao
import com.sebas.tiendaropa.data.dao.SaleWithDetails
import com.sebas.tiendaropa.data.entity.PaymentEntity
import com.sebas.tiendaropa.data.entity.SaleEntity
import com.sebas.tiendaropa.data.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

data class PaymentUpdate(
    val id: Long,
    val amountCents: Long,
    val description: String?
)

class SalesRepository(private val saleDao: SaleDao) {

    val sales: Flow<List<SaleWithDetails>> = saleDao.observeSales()

    suspend fun createSale(
        customerId: Long,
        totalCents: Long,
        createdAtMillis: Long,
        description: String?,
        items: List<SaleItemEntity>
    ): Long {
        val sale = SaleEntity(
            customerId = customerId,
            createdAtMillis = createdAtMillis,
            totalCents = totalCents,
            description = description?.ifBlank { null }
        )
        return saleDao.insertSaleWithItems(sale, items)
    }
    suspend fun updateSale(
        saleId: Long,
        createdAtMillis: Long,
        description: String?,
        paymentUpdates: List<PaymentUpdate> = emptyList()
    ) {
        saleDao.updateSaleDetails(
            id = saleId,
            createdAtMillis = createdAtMillis,
            description = description?.ifBlank { null }
        )
        if (paymentUpdates.isNotEmpty()) {
            paymentUpdates.forEach { update ->
                saleDao.updatePayment(
                    id = update.id,
                    amountCents = update.amountCents,
                    description = update.description?.ifBlank { null }
                )
            }
        }
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