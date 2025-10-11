package com.sebas.tiendaropa.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val cedula: String? = null,
    val description: String? = null
)