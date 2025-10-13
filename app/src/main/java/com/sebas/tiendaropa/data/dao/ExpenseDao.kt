package com.sebas.tiendaropa.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.sebas.tiendaropa.data.entity.ExpenseCategoryEntity
import com.sebas.tiendaropa.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpenseEntity): Long

    @Update
    suspend fun update(entity: ExpenseEntity)

    @Delete
    suspend fun delete(entity: ExpenseEntity)

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC, id DESC")
    suspend fun getAllSnapshot(): List<ExpenseWithCategory>

    @Transaction
    @Query(
        """
            SELECT * FROM expenses
            WHERE concept LIKE :query
                OR IFNULL(description, '') LIKE :query
                OR paymentMethod LIKE :query
            ORDER BY dateMillis DESC, id DESC
        """
    )
    fun search(query: String): Flow<List<ExpenseWithCategory>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM expenses")
    fun observeTotalAmount(): Flow<Long>
}

data class ExpenseWithCategory(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: ExpenseCategoryEntity?
)
