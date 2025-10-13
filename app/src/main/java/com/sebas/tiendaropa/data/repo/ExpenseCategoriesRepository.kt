package com.sebas.tiendaropa.data.repo

import com.sebas.tiendaropa.data.dao.ExpenseCategoryDao
import com.sebas.tiendaropa.data.entity.ExpenseCategoryEntity

class ExpenseCategoriesRepository(private val dao: ExpenseCategoryDao) {

    val categories = dao.observeAll()

    fun search(query: String) = dao.search("%${query}%")

    suspend fun add(name: String) =
        dao.insert(
            ExpenseCategoryEntity(
                name = name.trim()
            )
        )

    suspend fun update(id: Long, name: String) =
        dao.update(
            ExpenseCategoryEntity(
                id = id,
                name = name.trim()
            )
        )

    suspend fun remove(category: ExpenseCategoryEntity) = dao.deleteAndClear(category)
}
