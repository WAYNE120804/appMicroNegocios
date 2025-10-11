package com.sebas.tiendaropa.data.repo

import com.sebas.tiendaropa.data.dao.ProductDao
import com.sebas.tiendaropa.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductsRepository(private val dao: ProductDao) {

    val products: Flow<List<ProductEntity>> = dao.observeAll()
    val availableProducts: Flow<List<ProductEntity>> = dao.observeAvailable()
    val soldProducts: Flow<List<ProductEntity>> = dao.observeSold()

    fun search(q: String): Flow<List<ProductEntity>> =
        dao.search("%${q}%")

    fun observeTotalCompras() = dao.observeTotalCompras()
    fun observeTotalGanancia() = dao.observeTotalGanancia()

    suspend fun add(
        name: String,
        description: String?,
        avisos: String?,
        categoryId: Long,            // obligatorio
        compraCents: Long,
        ventaCents: Long,
        imageUris: List<String>
    ): Long = dao.insert(
        ProductEntity(
            name = name,
            description = description,
            avisos = avisos,
            categoryId = categoryId,
            valorCompraCents = compraCents,
            valorVentaCents = ventaCents,
            imageUris = imageUris
        )
    )

    suspend fun update(
        id: Long,
        name: String,
        description: String?,
        avisos: String?,
        categoryId: Long,
        compraCents: Long,
        ventaCents: Long,
        imageUris: List<String>,
        soldSaleId: Long? = null
    ) = dao.update(
        ProductEntity(
            id = id,
            name = name,
            description = description,
            avisos = avisos,
            categoryId = categoryId,
            valorCompraCents = compraCents,
            valorVentaCents = ventaCents,
            imageUris = imageUris,
            soldSaleId = soldSaleId
        )
    )

    suspend fun remove(entity: ProductEntity) = dao.delete(entity)

    suspend fun markProductsAsSold(productIds: List<Long>, saleId: Long) {
        if (productIds.isEmpty()) return
        dao.markAsSold(productIds, saleId)
    }

    suspend fun markProductsAsAvailable(productIds: List<Long>) {
        if (productIds.isEmpty()) return
        dao.markAsAvailable(productIds)
    }
}
