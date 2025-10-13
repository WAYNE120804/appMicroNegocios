package com.sebas.tiendaropa.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_categories")
data class ExpenseCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
