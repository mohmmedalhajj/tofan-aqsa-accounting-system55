package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qat_tax_entries")
data class TaxEntry(
    @PrimaryKey val date: String, // Format: yyyy-MM-dd
    val taxAmount: Double,
    val stallOutflow: Double = 0.0,
    val laborOutflow: Double = 0.0,
    val notes: String = ""
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Double,
    val buyPrice: Double,
    val sellPrice: Double,
    val lowStockThreshold: Double,
    val qatType: String = "",
    val notes: String = "",
    val dateAdded: String = "",
    val buyPriceCurrency: String = "ريال يمني"
)

@Entity(tableName = "sales_invoices")
data class SalesInvoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val date: String, // Format: yyyy-MM-dd HH:mm
    val totalAmount: Double,
    val taxApplied: Double,
    val discount: Double,
    val paidAmount: Double,
    val debtAmount: Double,
    val paymentMethod: String = "نقداً",
    val currency: String = "ريال يمني"
)

@Entity(tableName = "sales_invoice_items")
data class SalesInvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceId: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Double,
    val unitPrice: Double,
    val buyPrice: Double // Stored at the time of sale for accurate profit calculations
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val totalDebt: Double = 0.0,
    val address: String = "",
    val notes: String = ""
)

@Entity(tableName = "customer_payments")
data class CustomerPayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val amount: Double,
    val date: String, // Format: yyyy-MM-dd HH:mm
    val notes: String,
    val paymentMethod: String = "نقداً"
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val totalBalance: Double = 0.0
)

@Entity(tableName = "supplier_purchases")
data class SupplierPurchase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val supplierId: Int,
    val date: String, // Format: yyyy-MM-dd HH:mm
    val totalAmount: Double,
    val paidAmount: Double,
    val debtRemaining: Double,
    val paymentMethod: String = "نقداً"
)

@Entity(tableName = "supplier_purchase_items")
data class SupplierPurchaseItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val purchaseId: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Double,
    val unitPrice: Double
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val amount: Double,
    val date: String, // Format: yyyy-MM-dd HH:mm
    val notes: String
)

@Entity(tableName = "money_transfers")
data class MoneyTransfer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val sender: String = "عاهد الصبري",
    val receiver: String = "صرافة عبدالله المشرقي",
    val referenceNumber: String = "",
    val statement: String = "",
    val notes: String = "",
    val date: String, // Format: yyyy-MM-dd HH:mm
    val currency: String = "ريال يمني"
)
