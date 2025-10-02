package com.sebas.tiendaropa.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Precios en centavos de COP para evitar errores de redondeo.
 * Ej: $12.345 -> 1_234_500.
 * categoryId es OBLIGATORIO (producto siempre pertenece a una categoría).
 */
@Entity(tableName = "products")
class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,   // texto libre
    val avisos: String? = null,        // texto libre (notas/alertas)
    val categoryId: Long,              // <-- obligatorio
    val valorCompraCents: Long,
    val valorVentaCents: Long
) {
    fun gananciaCents(): Long = valorVentaCents - valorCompraCents
}
