package com.sebas.tiendaropa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sebas.tiendaropa.data.dao.CategoryDao
import com.sebas.tiendaropa.data.dao.CustomerDao
import com.sebas.tiendaropa.data.dao.ProductDao
import com.sebas.tiendaropa.data.entity.CategoryEntity
import com.sebas.tiendaropa.data.entity.CustomerEntity
import com.sebas.tiendaropa.data.entity.ProductEntity

@Database(
    entities = [CustomerEntity::class,
        CategoryEntity::class,
        ProductEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao() : CustomerDao
    abstract fun categoryDao() : CategoryDao
    abstract fun productDao(): ProductDao

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