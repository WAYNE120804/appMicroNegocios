package com.sebas.tiendaropa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sebas.tiendaropa.data.dao.CustomerDao
import com.sebas.tiendaropa.data.entity.CustomerEntity

@Database(entities = [CustomerEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao() : CustomerDao

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