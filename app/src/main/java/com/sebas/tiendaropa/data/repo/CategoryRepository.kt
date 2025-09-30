package com.sebas.tiendaropa.data.repo

import com.sebas.tiendaropa.data.dao.CategoryDao
import com.sebas.tiendaropa.data.entity.CategoryEntity

class CategoryRepository (private val dao: CategoryDao) {

    val categories = dao.observeAll()

    fun search(q: String) = dao.search(q)

    suspend fun add(name: String) =
        dao.insert(
            CategoryEntity(
                name = name.trim()
            )
        )

    suspend fun update(id: Long, name: String) =
        dao.update(
            CategoryEntity(
                id = id,
                name = name.trim()
            )
        )

    suspend fun remove(c: CategoryEntity) = dao.delete(c)

}