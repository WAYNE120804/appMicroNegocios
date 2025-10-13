package com.sebas.tiendaropa.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sebas.tiendaropa.data.entity.ExpenseCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseCategoryDao {

    @Query("SELECT * FROM expense_categories ORDER BY name ASC")
    fun observeAll(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories WHERE name LIKE :query ORDER BY name ASC")
    fun search(query: String): Flow<List<ExpenseCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpenseCategoryEntity): Long

    @Update
    suspend fun update(entity: ExpenseCategoryEntity)

    @Query("DELETE FROM expense_categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE expenses SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromExpenses(categoryId: Long)

    @Transaction
    suspend fun deleteAndClear(category: ExpenseCategoryEntity) {
        clearCategoryFromExpenses(category.id)
        deleteById(category.id)
    }
}
