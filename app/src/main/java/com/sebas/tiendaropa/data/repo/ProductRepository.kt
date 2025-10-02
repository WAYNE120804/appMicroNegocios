package com.sebas.tiendaropa.data.repo

import com.sebas.tiendaropa.data.dao.ProductDao
import com.sebas.tiendaropa.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductsRepository(private val dao: ProductDao) {

    val products: Flow<List<ProductEntity>> = dao.observeAll()

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
        ventaCents: Long
    ): Long = dao.insert(
        ProductEntity(
            name = name,
            description = description,
            avisos = avisos,
            categoryId = categoryId,
            valorCompraCents = compraCents,
            valorVentaCents = ventaCents
        )
    )

    suspend fun update(
        id: Long,
        name: String,
        description: String?,
        avisos: String?,
        categoryId: Long,
        compraCents: Long,
        ventaCents: Long
    ) = dao.update(
        ProductEntity(
            id = id,
            name = name,
            description = description,
            avisos = avisos,
            categoryId = categoryId,
            valorCompraCents = compraCents,
            valorVentaCents = ventaCents
        )
    )

    suspend fun remove(entity: ProductEntity) = dao.delete(entity)
}
