package com.sebas.tiendaropa.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sebas.tiendaropa.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllSnapshot(): List<CategoryEntity>

    @Query("""
        SELECT * FROM categories
        WHERE name LIKE '%' || :q || '%'
        ORDER BY name ASC
    """)
    fun search(q: String): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insert(c: CategoryEntity)

    @Update
    suspend fun update(c: CategoryEntity)

    @Delete
    suspend fun delete(c: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
