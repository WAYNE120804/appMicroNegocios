package com.sebas.tiendaropa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sebas.tiendaropa.data.dao.CategoryDao
import com.sebas.tiendaropa.data.dao.CustomerDao
import com.sebas.tiendaropa.data.dao.ProductDao
import com.sebas.tiendaropa.data.dao.SaleDao
import com.sebas.tiendaropa.data.entity.CategoryEntity
import com.sebas.tiendaropa.data.entity.CustomerEntity
import com.sebas.tiendaropa.data.entity.PaymentEntity
import com.sebas.tiendaropa.data.entity.ProductEntity
import com.sebas.tiendaropa.data.entity.SaleEntity
import com.sebas.tiendaropa.data.entity.SaleItemEntity


@Database(
    entities = [
        CustomerEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PaymentEntity::class
               ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao() : CustomerDao
    abstract fun categoryDao() : CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao() : SaleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "store_db"
                ).build().also { INSTANCE = it }
            }
    }
}