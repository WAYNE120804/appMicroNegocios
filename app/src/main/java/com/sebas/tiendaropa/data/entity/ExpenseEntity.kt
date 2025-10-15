package com.sebas.tiendaropa.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey



@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val concept: String,
    val amountCents: Long,
    val dateMillis: Long,
    val categoryId: Long?,
    val paymentMethod: String,
    val description: String?,
    val photoUri: String?
)
