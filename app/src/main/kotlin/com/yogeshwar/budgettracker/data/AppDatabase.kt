package com.yogeshwar.budgettracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CategoryEntity::class,
        MerchantEntity::class,
        AliasEntity::class,
        ExpenseEntity::class,
        ReviewEntity::class,
        IgnoredEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): LedgerDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "budget-tracker.db").build()
    }
}
