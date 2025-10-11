package com.sebas.tiendaropa.data.repo

import com.sebas.tiendaropa.data.dao.CustomerDao
import com.sebas.tiendaropa.data.entity.CustomerEntity

class CustomersRepository(private val dao: CustomerDao) {
    val customers = dao.observeAll()

    fun search(q: String) = dao.search(q)


    suspend fun add(name: String, address: String?, phone: String?, cedula: String?, description: String?) =
        dao.insert(
            CustomerEntity(
                name = name.trim(),
                address = address?.ifBlank { null },
                phone = phone?.ifBlank { null },
                cedula = cedula?.ifBlank { null },
                description = description?.ifBlank { null }
            )
        )

    suspend fun update(
        id: Long,
        name: String,
        address: String?,
        phone: String?,
        cedula: String?,
        description: String?
    ) =
        dao.update(
            CustomerEntity(
                id = id,
                name = name.trim(),
                address = address?.ifBlank { null },
                phone = phone?.ifBlank { null },
                cedula = cedula?.ifBlank { null },
                description = description?.ifBlank { null }
            )
        )

    suspend fun remove(c: CustomerEntity) = dao.delete(c)

    suspend fun removeById(id: Long) = dao.deleteById(id)
}
