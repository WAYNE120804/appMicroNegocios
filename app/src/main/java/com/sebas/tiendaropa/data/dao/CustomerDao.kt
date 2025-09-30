package com.sebas.tiendaropa.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sebas.tiendaropa.data.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Insert
    suspend fun insert(c: CustomerEntity)

    @Delete
    suspend fun delete(c: CustomerEntity)

    @Update
    suspend fun update(c: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: Long)
}