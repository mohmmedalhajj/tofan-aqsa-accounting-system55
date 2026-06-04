package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaxEntry::class,
        InventoryItem::class,
        SalesInvoice::class,
        SalesInvoiceItem::class,
        Customer::class,
        CustomerPayment::class,
        Supplier::class,
        SupplierPurchase::class,
        SupplierPurchaseItem::class,
        Expense::class,
        MoneyTransfer::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taxDao(): TaxDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun salesDao(): SalesDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tofan_al_aqsa_qat_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun checkpoint() {
            try {
                INSTANCE?.openHelper?.writableDatabase?.execSQL("PRAGMA wal_checkpoint(FULL)")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
