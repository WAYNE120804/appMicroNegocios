package com.sebas.tiendaropa.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.sebas.tiendaropa.data.entity.CustomerEntity
import com.sebas.tiendaropa.data.entity.PaymentEntity
import com.sebas.tiendaropa.data.entity.ProductEntity
import com.sebas.tiendaropa.data.entity.SaleEntity
import com.sebas.tiendaropa.data.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(entity: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Delete
    suspend fun deleteSale(entity: SaleEntity)

    @Transaction
    suspend fun insertSaleWithItems(sale: SaleEntity, items: List<SaleItemEntity>): Long {
        val saleId = insertSale(sale)
        if (items.isNotEmpty()) {
            insertItems(items.map { it.copy(saleId = saleId) })
        }
        return saleId
    }

    @Transaction
    @Query("SELECT * FROM sales ORDER BY createdAtMillis DESC")
    fun observeSales(): Flow<List<SaleWithDetails>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM payments WHERE saleId = :saleId")
    suspend fun totalPaymentsForSale(saleId: Long): Long
}

data class SaleWithDetails(
    @Embedded val sale: SaleEntity,
    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    val customer: CustomerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId",
        entity = SaleItemEntity::class
    )
    val items: List<SaleItemWithProduct>,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val payments: List<PaymentEntity>
) {
    val totalPaidCents: Long get() = payments.sumOf { it.amountCents }
    val amountDueCents: Long get() = (sale.totalCents - totalPaidCents).coerceAtLeast(0)
}

data class SaleItemWithProduct(
    @Embedded val item: SaleItemEntity,
    @Relation(
        parentColumn = "productId",
        entityColumn = "id",
        entity = ProductEntity::class
    )
    val product: ProductEntity
) {
    val lineTotalCents: Long get() = item.unitPriceCents * item.quantity
}