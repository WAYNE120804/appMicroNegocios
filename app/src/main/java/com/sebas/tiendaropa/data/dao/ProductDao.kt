package com.sebas.tiendaropa.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sebas.tiendaropa.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProductEntity): Long

    @Update
    suspend fun update(entity: ProductEntity)

    @Delete
    suspend fun delete(entity: ProductEntity)

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllSnapshot(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE soldSaleId IS NULL ORDER BY name ASC")
    fun observeAvailable(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE soldSaleId IS NOT NULL ORDER BY name ASC")
    fun observeSold(): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE name LIKE :q OR IFNULL(description,'') LIKE :q OR IFNULL(avisos,'') LIKE :q
        ORDER BY name ASC
    """)
    fun search(q: String): Flow<List<ProductEntity>>

    // Totales para dashboard
    @Query("SELECT COALESCE(SUM(valorCompraCents),0) FROM products")
    fun observeTotalCompras(): Flow<Long>

    @Query("SELECT COALESCE(SUM(valorVentaCents - valorCompraCents),0) FROM products")
    fun observeTotalGanancia(): Flow<Long>

    @Query("UPDATE products SET soldSaleId = :saleId WHERE id IN (:productIds)")
    suspend fun markAsSold(productIds: List<Long>, saleId: Long)

    @Query("UPDATE products SET soldSaleId = NULL WHERE id IN (:productIds)")
    suspend fun markAsAvailable(productIds: List<Long>)
}
