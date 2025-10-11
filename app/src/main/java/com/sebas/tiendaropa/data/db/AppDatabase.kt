package com.sebas.tiendaropa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
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
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(
    entities = [
        CustomerEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PaymentEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao() : CustomerDao
    abstract fun categoryDao() : CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao() : SaleDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN cedula TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN description TEXT")
                db.execSQL("ALTER TABLE payments ADD COLUMN description TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN soldSaleId INTEGER")
                db.execSQL("ALTER TABLE products ADD COLUMN imageUris TEXT")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "store_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}