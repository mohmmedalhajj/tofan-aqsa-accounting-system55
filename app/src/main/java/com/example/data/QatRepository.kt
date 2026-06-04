package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class QatRepository(private val db: AppDatabase) {

    private val taxDao = db.taxDao()
    private val inventoryDao = db.inventoryDao()
    private val salesDao = db.salesDao()
    private val customerDao = db.customerDao()
    private val supplierDao = db.supplierDao()
    private val expenseDao = db.expenseDao()
    private val transferDao = db.transferDao()

    // --- Flows ---
    val allTax: Flow<List<TaxEntry>> = taxDao.getAllTax()
    val allInventoryItems: Flow<List<InventoryItem>> = inventoryDao.getAllItems()
    val allInvoices: Flow<List<SalesInvoice>> = salesDao.getAllInvoices()
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    val allPurchases: Flow<List<SupplierPurchase>> = supplierDao.getAllPurchases()
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allPaymentsCode: Flow<List<CustomerPayment>> = customerDao.getAllPayments()
    val allTransfers: Flow<List<MoneyTransfer>> = transferDao.getAllTransfers()
    val allPurchaseItems: Flow<List<SupplierPurchaseItem>> = supplierDao.getAllPurchaseItems()
    val allInvoiceItems: Flow<List<SalesInvoiceItem>> = salesDao.getAllInvoiceItems()

    // --- Tax Operations ---
    suspend fun getTaxAmountForDate(date: String): Double {
        return taxDao.getTaxByDate(date)?.taxAmount ?: 0.0
    }

    suspend fun setTaxForDate(date: String, amount: Double) {
        val existing = taxDao.getTaxByDate(date)
        if (existing != null) {
            taxDao.insertTax(existing.copy(taxAmount = amount))
        } else {
            taxDao.insertTax(TaxEntry(date = date, taxAmount = amount))
        }
    }

    suspend fun getTaxEntryForDate(date: String): TaxEntry? {
        return taxDao.getTaxByDate(date)
    }

    suspend fun saveTaxEntry(taxEntry: TaxEntry) {
        taxDao.insertTax(taxEntry)
    }

    // --- Inventory Operations ---
    suspend fun addOrUpdateInventoryItem(item: InventoryItem): Long {
        return inventoryDao.insertItem(item)
    }

    suspend fun deleteInventoryItem(item: InventoryItem) {
        inventoryDao.deleteItemById(item.id)
    }

    suspend fun getItemById(id: Int): InventoryItem? {
        return inventoryDao.getItemById(id)
    }

    // --- Sales Operations with stock reduction & debt accumulation ---
    suspend fun makeSale(
        customerName: String,
        date: String, // yyyy-MM-dd HH:mm
        items: List<Pair<InventoryItem, Double>>, // Item & Quantity sold
        taxPercent: Double, // Daily flat tax applied or custom
        discount: Double,
        paidAmount: Double,
        paymentMethod: String = "نقداً",
        currency: String = "ريال يمني"
    ): Long {
        return db.withTransaction {
            var subtotal = 0.0
            for (p in items) {
                subtotal += p.first.sellPrice * p.second
            }
            
            val debt = if (subtotal > paidAmount) subtotal - paidAmount else 0.0

            // Create Invoice
            val invoice = SalesInvoice(
                customerName = customerName,
                date = date,
                totalAmount = subtotal,
                taxApplied = 0.0,
                discount = 0.0,
                paidAmount = paidAmount,
                debtAmount = debt,
                paymentMethod = paymentMethod,
                currency = currency
            )
            val invoiceId = salesDao.insertInvoice(invoice).toInt()

            // Save items & update stock
            for (p in items) {
                val stockItem = p.first
                val qty = p.second
                val saleItem = SalesInvoiceItem(
                    invoiceId = invoiceId,
                    itemId = stockItem.id,
                    itemName = stockItem.name,
                    quantity = qty,
                    unitPrice = stockItem.sellPrice,
                    buyPrice = stockItem.buyPrice
                )
                salesDao.insertInvoiceItem(saleItem)
                
                // Deduct stock
                inventoryDao.updateQuantity(stockItem.id, -qty)
            }

            // Update customer debt if customer name matches a registered customer
            if (customerName.trim().isNotEmpty() && customerName.trim() != "عميل نقدي") {
                val clientList = customerDao.getAllCustomersDirect()
                val registeredCust = clientList.find { it.name.trim().equals(customerName.trim(), ignoreCase = true) }
                if (registeredCust != null) {
                    if (debt > 0.0) {
                        customerDao.updateCustomerDebt(registeredCust.id, debt)
                    }
                } else {
                    // Create customer automatically!
                    customerDao.insertCustomer(
                        Customer(name = customerName.trim(), phone = "", totalDebt = debt)
                    )
                }
            }

            invoiceId.toLong()
        }
    }

    suspend fun deleteSale(invoiceId: Int) {
        db.withTransaction {
            val invoice = salesDao.getInvoiceById(invoiceId) ?: return@withTransaction
            val items = salesDao.getInvoiceItemsDirect(invoiceId)
            
            // Revert stock changes
            for (item in items) {
                inventoryDao.updateQuantity(item.itemId, item.quantity)
            }

            // Revert customer debt
            if (invoice.debtAmount > 0.0) {
                val clientList = customerDao.getAllCustomersDirect()
                val registeredCust = clientList.find { it.name.trim().equals(invoice.customerName.trim(), ignoreCase = true) }
                if (registeredCust != null) {
                    customerDao.updateCustomerDebt(registeredCust.id, -invoice.debtAmount)
                }
            }

            // Wipe records
            salesDao.deleteInvoiceById(invoiceId)
            salesDao.deleteInvoiceItemsByInvoiceId(invoiceId)
        }
    }

    fun getSaleItems(invoiceId: Int): Flow<List<SalesInvoiceItem>> {
        return salesDao.getInvoiceItems(invoiceId)
    }

    suspend fun updateInvoice(
        invoiceId: Int,
        newCustomerName: String,
        newPaidAmount: Double,
        newPaymentMethod: String
    ) {
        db.withTransaction {
            val invoice = salesDao.getInvoiceById(invoiceId) ?: return@withTransaction
            val oldDebt = invoice.debtAmount
            
            // Recalculate debt
            val total = invoice.totalAmount
            val newDebt = if (total > newPaidAmount) total - newPaidAmount else 0.0
            
            // Update customer balance if debt changed
            val clientList = customerDao.getAllCustomersDirect()
            
            // 1. Revert old debt from old customer
            val oldCust = clientList.find { it.name.trim().equals(invoice.customerName.trim(), ignoreCase = true) }
            if (oldCust != null && oldDebt > 0.0) {
                customerDao.updateCustomerDebt(oldCust.id, -oldDebt)
            }
            
            // 2. Apply new debt to new customer
            val newCust = clientList.find { it.name.trim().equals(newCustomerName.trim(), ignoreCase = true) }
            if (newCust != null && newDebt > 0.0) {
                customerDao.updateCustomerDebt(newCust.id, newDebt)
            } else if (newDebt > 0.0 && newCustomerName.trim().isNotEmpty() && newCustomerName.trim() != "عميل نقدي") {
                // Create new customer
                customerDao.insertCustomer(
                    Customer(name = newCustomerName.trim(), phone = "", totalDebt = newDebt)
                )
            }
            
            // 3. Update the invoice record
            val updatedInvoice = invoice.copy(
                customerName = newCustomerName,
                paidAmount = newPaidAmount,
                debtAmount = newDebt,
                paymentMethod = newPaymentMethod
            )
            salesDao.insertInvoice(updatedInvoice)
        }
    }

    // --- Customer operations ---
    suspend fun getAllCustomersDirect(): List<Customer> {
        return customerDao.getAllCustomersDirect()
    }

    suspend fun addCustomer(customer: Customer): Long {
        return customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomerById(customer.id)
    }

    suspend fun recordDirectCustomerDebt(
        customerName: String,
        amount: Double,
        date: String,
        statement: String,
        notes: String
    ): Long {
        return db.withTransaction {
            val paymentMethod = if (notes.trim().isNotEmpty()) {
                "دين مباشر: $statement ($notes)"
            } else {
                "دين مباشر: $statement"
            }
            val invoice = SalesInvoice(
                customerName = customerName,
                date = date,
                totalAmount = amount,
                taxApplied = 0.0,
                discount = 0.0,
                paidAmount = 0.0,
                debtAmount = amount,
                paymentMethod = paymentMethod
            )
            val invoiceId = salesDao.insertInvoice(invoice)
            
            // Update customer debt
            if (customerName.trim().isNotEmpty() && customerName.trim() != "عميل نقدي") {
                val clientList = customerDao.getAllCustomersDirect()
                val registeredCust = clientList.find { it.name.trim().equals(customerName.trim(), ignoreCase = true) }
                if (registeredCust != null) {
                    customerDao.updateCustomerDebt(registeredCust.id, amount)
                } else {
                    customerDao.insertCustomer(
                        Customer(name = customerName.trim(), phone = "", totalDebt = amount)
                    )
                }
            }
            invoiceId
        }
    }

    suspend fun payCustomerDebt(customerId: Int, amount: Double, date: String, notes: String, paymentMethod: String = "نقداً") {
        db.withTransaction {
            val pay = CustomerPayment(
                customerId = customerId,
                amount = amount,
                date = date,
                notes = notes,
                paymentMethod = paymentMethod
            )
            customerDao.insertPayment(pay)
            customerDao.updateCustomerDebt(customerId, -amount)
        }
    }

    fun getPaymentsForCustomer(customerId: Int): Flow<List<CustomerPayment>> {
        return customerDao.getPaymentsForCustomer(customerId)
    }

    // --- Supplier & purchase operations ---
    suspend fun addSupplier(supplier: Supplier): Long {
        return supplierDao.insertSupplier(supplier)
    }

    suspend fun updateSupplier(supplier: Supplier) {
        supplierDao.updateSupplier(supplier)
    }

    suspend fun deleteSupplier(supplier: Supplier) {
        supplierDao.deleteSupplierById(supplier.id)
    }

    suspend fun makeSupplierPurchase(
        supplierId: Int,
        date: String,
        items: List<Triple<Int, String, Double>>, // Item ID, Name, Quantity, UnitPrice (supplied items list)
        itemsPriceAndQty: List<Triple<InventoryItem, Double, Double>>, // Item, Quantity, PurchaseUnitPrice
        paidAmount: Double,
        paymentMethod: String = "نقداً"
    ): Long {
        return db.withTransaction {
            var totalBill = 0.0
            for (trip in itemsPriceAndQty) {
                totalBill += trip.second * trip.third
            }

            val debt = if (totalBill > paidAmount) totalBill - paidAmount else 0.0

            val purchase = SupplierPurchase(
                supplierId = supplierId,
                date = date,
                totalAmount = totalBill,
                paidAmount = paidAmount,
                debtRemaining = debt,
                paymentMethod = paymentMethod
            )
            val purchaseId = supplierDao.insertPurchase(purchase).toInt()

            for (trip in itemsPriceAndQty) {
                val stockItem = trip.first
                val qty = trip.second
                val buyPrice = trip.third
                
                // Add purchase item record
                val pItem = SupplierPurchaseItem(
                    purchaseId = purchaseId,
                    itemId = stockItem.id,
                    itemName = stockItem.name,
                    quantity = qty,
                    unitPrice = buyPrice
                )
                supplierDao.insertPurchaseItem(pItem)

                // Update stock and update average buyPrice!
                val existing = inventoryDao.getItemById(stockItem.id)
                if (existing != null) {
                    val newQuantity = existing.quantity + qty
                    val newBuyPrice = if (newQuantity > 0) {
                        ((existing.quantity * existing.buyPrice) + (qty * buyPrice)) / newQuantity
                    } else buyPrice
                    
                    inventoryDao.updateItem(
                        existing.copy(
                            quantity = newQuantity,
                            buyPrice = newBuyPrice
                        )
                    )
                } else {
                    // Create new stock item
                    inventoryDao.insertItem(
                        InventoryItem(
                            id = stockItem.id,
                            name = stockItem.name,
                            quantity = qty,
                            buyPrice = buyPrice,
                            sellPrice = stockItem.sellPrice,
                            lowStockThreshold = stockItem.lowStockThreshold
                        )
                    )
                }
            }

            if (debt > 0.0) {
                supplierDao.updateSupplierBalance(supplierId, debt)
            }

            purchaseId.toLong()
        }
    }

    suspend fun makeSupplierReturn(
        supplierId: Int,
        itemId: Int,
        itemName: String,
        returnedQty: Double,
        returnedPrice: Double,
        returnDate: String,
        refundedAmount: Double,
        notes: String
    ): Long {
        return db.withTransaction {
            val totalValue = returnedQty * returnedPrice
            val debtChange = -(totalValue - refundedAmount)

            val returnPurchase = SupplierPurchase(
                supplierId = supplierId,
                date = returnDate,
                totalAmount = -totalValue,
                paidAmount = -refundedAmount,
                debtRemaining = debtChange,
                paymentMethod = if (notes.trim().isNotEmpty()) "مرتجع: $notes" else "مرتجع بضاعة"
            )
            val purchaseId = supplierDao.insertPurchase(returnPurchase).toInt()

            val pItem = SupplierPurchaseItem(
                purchaseId = purchaseId,
                itemId = itemId,
                itemName = itemName,
                quantity = -returnedQty,
                unitPrice = returnedPrice
            )
            supplierDao.insertPurchaseItem(pItem)

            inventoryDao.updateQuantity(itemId, -returnedQty)
            supplierDao.updateSupplierBalance(supplierId, debtChange)

            purchaseId.toLong()
        }
    }

    suspend fun deletePurchase(purchaseId: Int) {
        db.withTransaction {
            val p = supplierDao.getAllPurchases().firstOrNull()?.find { it.id == purchaseId } ?: return@withTransaction
            val items = supplierDao.getPurchaseItemsDirect(purchaseId)

            for (item in items) {
                // Return stock
                inventoryDao.updateQuantity(item.itemId, -item.quantity)
            }

            if (p.debtRemaining > 0.0) {
                supplierDao.updateSupplierBalance(p.supplierId, -p.debtRemaining)
            }

            supplierDao.deletePurchaseById(purchaseId)
            supplierDao.deletePurchaseItemsByPurchaseId(purchaseId)
        }
    }

    suspend fun updatePurchaseInvoice(
        purchaseId: Int,
        newPaidAmount: Double,
        newPaymentMethod: String
    ) {
        db.withTransaction {
            val p = supplierDao.getAllPurchases().firstOrNull()?.find { it.id == purchaseId } ?: return@withTransaction
            
            // 1. Revert previous supplier debt from supplier balance
            if (p.debtRemaining > 0.0) {
                supplierDao.updateSupplierBalance(p.supplierId, -p.debtRemaining)
            }
            
            // 2. Compute new remaining debt
            val newDebt = if (p.totalAmount > newPaidAmount) p.totalAmount - newPaidAmount else 0.0
            
            // 3. Apply new remaining debt to supplier balance
            if (newDebt > 0.0) {
                supplierDao.updateSupplierBalance(p.supplierId, newDebt)
            }
            
            // 4. Save updated purchase record
            val updated = p.copy(
                paidAmount = newPaidAmount,
                debtRemaining = newDebt,
                paymentMethod = newPaymentMethod
            )
            supplierDao.insertPurchase(updated)
        }
    }

    suspend fun editSupplierPurchaseFull(
        purchaseId: Int,
        newSupplierId: Int,
        newDate: String,
        newItemsPriceAndQty: List<Triple<InventoryItem, Double, Double>>,
        newPaidAmount: Double,
        newPaymentMethod: String
    ) {
        db.withTransaction {
            val oldPurchase = supplierDao.getAllPurchases().firstOrNull()?.find { it.id == purchaseId } ?: return@withTransaction
            val oldItems = supplierDao.getPurchaseItemsDirect(purchaseId)
            
            // Revert old stock changes
            for (item in oldItems) {
                inventoryDao.updateQuantity(item.itemId, -item.quantity)
            }
            
            // Revert old debt from old supplier
            if (oldPurchase.debtRemaining > 0.0) {
                supplierDao.updateSupplierBalance(oldPurchase.supplierId, -oldPurchase.debtRemaining)
            }
            
            var newTotalBill = 0.0
            for (trip in newItemsPriceAndQty) {
                newTotalBill += trip.second * trip.third
            }
            val newDebt = if (newTotalBill > newPaidAmount) newTotalBill - newPaidAmount else 0.0
            
            val updatedPurchase = oldPurchase.copy(
                supplierId = newSupplierId,
                date = newDate,
                totalAmount = newTotalBill,
                paidAmount = newPaidAmount,
                debtRemaining = newDebt,
                paymentMethod = newPaymentMethod
            )
            supplierDao.insertPurchase(updatedPurchase)
            
            supplierDao.deletePurchaseItemsByPurchaseId(purchaseId)
            
            for (trip in newItemsPriceAndQty) {
                val stockItem = trip.first
                val qty = trip.second
                val buyPrice = trip.third
                
                val pItem = SupplierPurchaseItem(
                    purchaseId = purchaseId,
                    itemId = stockItem.id,
                    itemName = stockItem.name,
                    quantity = qty,
                    unitPrice = buyPrice
                )
                supplierDao.insertPurchaseItem(pItem)
                
                val existing = inventoryDao.getItemById(stockItem.id)
                if (existing != null) {
                    val newQuantity = existing.quantity + qty
                    val newBuyPrice = if (newQuantity > 0) {
                        ((existing.quantity * existing.buyPrice) + (qty * buyPrice)) / newQuantity
                    } else buyPrice
                    
                    inventoryDao.updateItem(
                        existing.copy(
                            quantity = newQuantity,
                            buyPrice = newBuyPrice
                        )
                    )
                } else {
                    inventoryDao.insertItem(
                        InventoryItem(
                            id = stockItem.id,
                            name = stockItem.name,
                            quantity = qty,
                            buyPrice = buyPrice,
                            sellPrice = stockItem.sellPrice,
                            lowStockThreshold = stockItem.lowStockThreshold
                        )
                    )
                }
            }
            
            if (newDebt > 0.0) {
                supplierDao.updateSupplierBalance(newSupplierId, newDebt)
            }
        }
    }

    fun getPurchaseItems(purchaseId: Int): Flow<List<SupplierPurchaseItem>> {
        return supplierDao.getPurchaseItems(purchaseId)
    }

    // --- Expense Operations ---
    suspend fun addExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpenseById(expense.id)
    }

    // --- Transfer Operations ---
    suspend fun addTransfer(transfer: MoneyTransfer): Long {
        return transferDao.insertTransfer(transfer)
    }

    suspend fun deleteTransfer(transfer: MoneyTransfer) {
        transferDao.deleteTransferById(transfer.id)
    }

    suspend fun clearDatabaseDirectly() {
        db.withTransaction {
            db.clearAllTables()
        }
    }

    suspend fun generateStressTestDataDirectly(invoiceCount: Int, customerCount: Int) {
        db.withTransaction {
            // 1. Create default items in inventory
            val qatNames = listOf("قات ماوي سوبر", "قات ورزاني ممتاز", "قات أرحب قطاف", "قات بلدي أبيض", "قات وادي رجام")
            val itemsSaved = mutableListOf<InventoryItem>()
            for (i in qatNames.indices) {
                val item = InventoryItem(
                    id = i + 1,
                    name = qatNames[i],
                    quantity = 250.0 + (i * 50),
                    buyPrice = 4500.0 + (i * 500),
                    sellPrice = 6000.0 + (i * 700),
                    lowStockThreshold = 10.0,
                    qatType = if (i % 2 == 0) "ماوي" else "ورزاني",
                    notes = "صنف ممتاز للتوزيع والبيع اليومي المباشر فائق الجودة",
                    dateAdded = "2026-05-01 10:00"
                )
                inventoryDao.insertItem(item)
                itemsSaved.add(item)
            }

            // 2. Create default suppliers
            val supplierNames = listOf("المورد عادل الهصبري", "المورد جمال الشميري", "المورد غانم الذيفاني")
            val supplierIds = mutableListOf<Int>()
            for (i in supplierNames.indices) {
                val supId = supplierDao.insertSupplier(Supplier(id = i + 1, name = supplierNames[i], phone = "77000000$i", totalBalance = 150000.0 * i))
                supplierIds.add(supId.toInt())
            }

            // 3. Create customers
            val customerNamePrefixes = listOf("الشيخ صالح", "الحاج ردمان", "الأستاذ نبيل", "المقدم علي", "العميد سنان", "التاجر ياسر", "المشرف إبراهيم")
            val customerNameSuffixes = listOf("الصلوي", "الحيمي", "الصبري", "الهمداني", "اليريمي", "الذبحاني", "المقطري")
            val customerIds = mutableListOf<Int>()
            for (i in 1..customerCount) {
                val name = "${customerNamePrefixes[i % customerNamePrefixes.size]} ${customerNameSuffixes[(i + 3) % customerNameSuffixes.size]} ($i)"
                val phone = "77${(1000000..9999999).random()}"
                val debt = (10000..120000).random().toDouble()
                val custId = customerDao.insertCustomer(Customer(name = name, phone = phone, totalDebt = debt, address = "صنعاء - شارع تعز", notes = "عميل دائم مسجل في فحص الضغط"))
                customerIds.add(custId.toInt())
            }

            // 4. Create Invoices
            val paymentMethods = listOf("نقداً", "كريمي شيك", "حوالة نجم", "دين آجل")
            val baseDate = "2026-05-"
            for (i in 1..invoiceCount) {
                val day = String.format("%02d", (1..31).random())
                val hour = String.format("%02d", (8..21).random())
                val minute = String.format("%02d", (0..59).random())
                val invoiceDate = "$baseDate$day $hour:$minute"
                
                // Pick a random customer name
                val custName = if (i % 5 == 0) "عميل نقدي" else {
                    val randomPrefix = customerNamePrefixes.random()
                    val randomSuffix = customerNameSuffixes.random()
                    val randIdx = (1..customerCount).random()
                    "$randomPrefix $randomSuffix ($randIdx)"
                }

                // Random invoice items
                val numItems = (1..3).random()
                var totalAmount = 0.0
                val selectedItems = mutableListOf<SalesInvoiceItem>()
                for (j in 1..numItems) {
                    val item = itemsSaved.random()
                    val qty = (1..5).random().toDouble()
                    val itemTotal = qty * item.sellPrice
                    totalAmount += itemTotal
                }

                val discount = if (totalAmount > 10000 && i % 4 == 0) 500.0 else 0.0
                val netAmount = totalAmount - discount
                val payMethod = paymentMethods.random()
                val paidAmount = if (payMethod == "دين آجل") 0.0 else if (i % 3 == 0) netAmount / 2 else netAmount
                val debtAmount = netAmount - paidAmount

                val invoice = SalesInvoice(
                    customerName = custName,
                    date = invoiceDate,
                    totalAmount = netAmount,
                    taxApplied = netAmount * 0.01,
                    discount = discount,
                    paidAmount = paidAmount,
                    debtAmount = debtAmount,
                    paymentMethod = payMethod
                )
                val invoiceId = salesDao.insertInvoice(invoice).toInt()

                // Insert invoice items
                for (j in 1..numItems) {
                    val item = itemsSaved.random()
                    val qty = (1..5).random().toDouble()
                    val saleItem = SalesInvoiceItem(
                        invoiceId = invoiceId,
                        itemId = item.id,
                        itemName = item.name,
                        quantity = qty,
                        unitPrice = item.sellPrice,
                        buyPrice = item.buyPrice
                    )
                    salesDao.insertInvoiceItem(saleItem)
                }
            }

            // 5. Create some Customer Payments
            for (i in 1..50) {
                val day = String.format("%02d", (1..31).random())
                val dateStr = "$baseDate$day 16:30"
                val randomCustId = if (customerIds.isNotEmpty()) customerIds.random() else 1
                customerDao.insertPayment(
                    CustomerPayment(
                        customerId = randomCustId,
                        amount = (5000..30000).random().toDouble(),
                        date = dateStr,
                        notes = "دفعة تحت الحساب أثناء اختبار الضغط والاستقرار للمحاسب المطور",
                        paymentMethod = paymentMethods.random()
                    )
                )
            }

            // 6. Create some Expenses
            val expenseCategories = listOf("إيجار المحل", "أجور عمال", "ضريبة بلديات", "تغذية ومصاريف", "كهرباء ومياه", "نقل وتوريد القات")
            for (i in 1..40) {
                val day = String.format("%02d", (1..31).random())
                val dateStr = "$baseDate$day 11:45"
                expenseDao.insertExpense(
                    Expense(
                        category = expenseCategories.random(),
                        amount = (2000..50000).random().toDouble(),
                        date = dateStr,
                        notes = "مصروف تشغيلي دوري مسجل تلقائياً"
                    )
                )
            }

            // 7. Create some Money Transfers
            val receivers = listOf("صرافة عبدالله المشرقي", "شركة الكريمي للصرافة", "صرافة السنيدار", "حوالة إلى عميل")
            for (i in 1..30) {
                val day = String.format("%02d", (1..31).random())
                val dateStr = "$baseDate$day 14:15"
                transferDao.insertTransfer(
                    MoneyTransfer(
                        amount = (50000..800000).random().toDouble(),
                        sender = "عاهد الصبري",
                        receiver = receivers.random(),
                        referenceNumber = "REF-${(100000..999999).random()}",
                        notes = "حوالة مالية روتينية يومية مقيدة للمطابقة",
                        date = dateStr
                    )
                )
            }
        }
    }
}
