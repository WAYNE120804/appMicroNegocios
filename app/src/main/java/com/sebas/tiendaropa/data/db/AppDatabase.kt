package com.sebas.tiendaropa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.sebas.tiendaropa.data.dao.CategoryDao
import com.sebas.tiendaropa.data.dao.CustomerDao
import com.sebas.tiendaropa.data.dao.ExpenseCategoryDao
import com.sebas.tiendaropa.data.dao.ExpenseDao
import com.sebas.tiendaropa.data.dao.ProductDao
import com.sebas.tiendaropa.data.dao.SaleDao
import com.sebas.tiendaropa.data.entity.CategoryEntity
import com.sebas.tiendaropa.data.entity.CustomerEntity
import com.sebas.tiendaropa.data.entity.PaymentEntity
import com.sebas.tiendaropa.data.entity.ProductEntity
import com.sebas.tiendaropa.data.entity.ExpenseCategoryEntity
import com.sebas.tiendaropa.data.entity.ExpenseEntity
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
        PaymentEntity::class,
        ExpenseEntity::class,
        ExpenseCategoryEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao() : CustomerDao
    abstract fun categoryDao() : CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao() : SaleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS expense_categories (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL
                        )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS expenses (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            concept TEXT NOT NULL,
                            amountCents INTEGER NOT NULL,
                            dateMillis INTEGER NOT NULL,
                            categoryId INTEGER,
                            paymentMethod TEXT NOT NULL,
                            description TEXT,
                            photoUri TEXT,
                            FOREIGN KEY(categoryId) REFERENCES expense_categories(id) ON DELETE SET NULL
                        )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_categoryId ON expenses(categoryId)")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}