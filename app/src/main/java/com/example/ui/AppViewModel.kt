package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.Helpers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface Screen {
    object Splash : Screen
    object Login : Screen
    object Dashboard : Screen
    object Inventory : Screen
    object Sales : Screen
    object Accounts : Screen
    object Customers : Screen
    object Suppliers : Screen
    object Expenses : Screen
    object Transfers : Screen
    object Reports : Screen
    object Archive : Screen
    object Settings : Screen
}

class AppViewModel(
    application: Application,
    private val repository: QatRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val appPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // --- State-based navigation ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _navigationHistory = mutableListOf<Screen>()

    fun navigateTo(screen: Screen) {
        _navigationHistory.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateToDashboard() {
        _navigationHistory.clear()
        _currentScreen.value = Screen.Dashboard
    }

    fun navigateBack() {
        if (_navigationHistory.isNotEmpty()) {
            _currentScreen.value = _navigationHistory.removeAt(_navigationHistory.size - 1)
        } else {
            _currentScreen.value = Screen.Dashboard
        }
    }

    // --- Authentication ---
    var isLoggedIn = MutableStateFlow(false)
    var isPasswordVisible = MutableStateFlow(false)
    val adminUsername = "admin"
    val savedPasswordKey = "admin_password"

    fun getAdminPassword(): String {
        return appPrefs.getString(savedPasswordKey, "123456") ?: "123456"
    }

    fun changeAdminPassword(newPass: String) {
        appPrefs.edit().putString(savedPasswordKey, newPass).apply()
    }

    // --- Theme Settings ---
    val isDarkMode = MutableStateFlow(appPrefs.getBoolean("dark_mode", true))

    fun toggleTheme() {
        val newVal = !isDarkMode.value
        isDarkMode.value = newVal
        appPrefs.edit().putBoolean("dark_mode", newVal).apply()
    }

    // --- Passcode / Lock settings ---
    val isAppLockEnabled = MutableStateFlow(appPrefs.getBoolean("app_locked_v2", false))
    fun toggleAppLock(enabled: Boolean) {
        isAppLockEnabled.value = enabled
        appPrefs.edit().putBoolean("app_locked_v2", enabled).apply()
    }

    // --- Live Flows from Repository ---
    val inventoryItems: StateFlow<List<InventoryItem>> = repository.allInventoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoices: StateFlow<List<SalesInvoice>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<CustomerPayment>> = repository.allPaymentsCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers: StateFlow<List<Supplier>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<SupplierPurchase>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTaxHistory: StateFlow<List<TaxEntry>> = repository.allTax
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransfers: StateFlow<List<MoneyTransfer>> = repository.allTransfers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchaseItems: StateFlow<List<com.example.data.SupplierPurchaseItem>> = repository.allPurchaseItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoiceItems: StateFlow<List<com.example.data.SalesInvoiceItem>> = repository.allInvoiceItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Today Qat Tax Tracker ---
    val todayTaxAmount = MutableStateFlow(0.0)
    val todayTaxEntry = MutableStateFlow<TaxEntry?>(null)

    fun fetchTodayTax() {
        viewModelScope.launch {
            val entry = repository.getTaxEntryForDate(Helpers.getCurrentDate())
            todayTaxEntry.value = entry ?: TaxEntry(date = Helpers.getCurrentDate(), taxAmount = 0.0, stallOutflow = 0.0, laborOutflow = 0.0, notes = "")
            todayTaxAmount.value = entry?.taxAmount ?: 0.0
        }
    }

    fun saveTodayTax(amount: Double) {
        viewModelScope.launch {
            val current = todayTaxEntry.value ?: TaxEntry(date = Helpers.getCurrentDate(), taxAmount = 0.0)
            val updated = current.copy(taxAmount = amount)
            repository.saveTaxEntry(updated)
            todayTaxEntry.value = updated
            todayTaxAmount.value = amount
        }
    }

    fun saveTodayTaxAndOutflows(tax: Double, stall: Double, labor: Double, notes: String) {
        viewModelScope.launch {
            val entry = TaxEntry(
                date = Helpers.getCurrentDate(),
                taxAmount = tax,
                stallOutflow = stall,
                laborOutflow = labor,
                notes = notes
            )
            repository.saveTaxEntry(entry)
            todayTaxEntry.value = entry
            todayTaxAmount.value = tax
        }
    }

    // --- Inventory Operations ---
    fun saveInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.addOrUpdateInventoryItem(item)
        }
    }

    suspend fun saveInventoryItemDirectly(item: InventoryItem): Int {
        return repository.addOrUpdateInventoryItem(item).toInt()
    }

    fun deleteInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
        }
    }

    // --- Sales Operations ---
    fun executeSale(
        customerName: String,
        customerPhone: String = "",
        customerAddress: String = "",
        customerNotes: String = "",
        items: List<Pair<InventoryItem, Double>>,
        discount: Double,
        paidAmount: Double,
        paymentMethod: String = "نقداً",
        currency: String = "ريال يمني",
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val list = repository.allCustomers.firstOrNull() ?: emptyList()
            val exists = list.any { it.name.trim().equals(customerName.trim(), ignoreCase = true) }
            if (!exists && customerName.trim().isNotEmpty() && customerName.trim() != "عميل نقدي") {
                repository.addCustomer(Customer(name = customerName.trim(), phone = customerPhone, totalDebt = 0.0, address = customerAddress, notes = customerNotes))
            }
            val invoiceId = repository.makeSale(
                customerName = customerName,
                date = Helpers.getCurrentDateTime(),
                items = items,
                taxPercent = 0.0,
                discount = discount,
                paidAmount = paidAmount,
                paymentMethod = paymentMethod,
                currency = currency
            )
            onSuccess(invoiceId)
        }
    }

    fun updateSalesInvoice(
        invoiceId: Int,
        newCustomerName: String,
        newPaidAmount: Double,
        newPaymentMethod: String
    ) {
        viewModelScope.launch {
            repository.updateInvoice(invoiceId, newCustomerName, newPaidAmount, newPaymentMethod)
        }
    }

    fun voidSale(invoiceId: Int) {
        viewModelScope.launch {
            repository.deleteSale(invoiceId)
        }
    }

    fun getSaleItemsFlow(invoiceId: Int): Flow<List<SalesInvoiceItem>> {
        return repository.getSaleItems(invoiceId)
    }

    // --- Customer Operations ---
    fun createCustomer(name: String, phone: String, address: String = "", notes: String = "") {
        viewModelScope.launch {
            repository.addCustomer(Customer(name = name, phone = phone, totalDebt = 0.0, address = address, notes = notes))
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    fun addCustomerPayment(customerId: Int, amount: Double, notes: String, paymentMethod: String = "نقداً") {
        viewModelScope.launch {
            repository.payCustomerDebt(
                customerId = customerId,
                amount = amount,
                date = Helpers.getCurrentDateTime(),
                notes = notes,
                paymentMethod = paymentMethod
            )
        }
    }

    fun recordDirectDebt(
        customerName: String,
        amount: Double,
        statement: String,
        date: String = Helpers.getCurrentDateTime(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.recordDirectCustomerDebt(
                customerName = customerName,
                amount = amount,
                date = date,
                statement = statement,
                notes = notes
            )
        }
    }

    fun getPaymentsForCustomer(customerId: Int): Flow<List<CustomerPayment>> {
        return repository.getPaymentsForCustomer(customerId)
    }

    // --- Supplier Operations ---
    fun createSupplier(name: String, phone: String) {
        viewModelScope.launch {
            repository.addSupplier(Supplier(name = name, phone = phone, totalBalance = 0.0))
        }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.updateSupplier(supplier)
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
        }
    }

    fun executeSupplierPurchase(
        supplierId: Int,
        itemsPriceAndQty: List<Triple<InventoryItem, Double, Double>>, // Item, Qty, UnitPurchasePrice
        paidAmount: Double,
        paymentMethod: String = "نقداً"
    ) {
        viewModelScope.launch {
            repository.makeSupplierPurchase(
                supplierId = supplierId,
                date = Helpers.getCurrentDateTime(),
                items = emptyList(),
                itemsPriceAndQty = itemsPriceAndQty,
                paidAmount = paidAmount,
                paymentMethod = paymentMethod
            )
        }
    }

    fun executeSupplierReturn(
        supplierId: Int,
        itemId: Int,
        itemName: String,
        returnedQty: Double,
        returnedPrice: Double,
        returnDate: String,
        refundedAmount: Double,
        notes: String
    ) {
        viewModelScope.launch {
            repository.makeSupplierReturn(
                supplierId = supplierId,
                itemId = itemId,
                itemName = itemName,
                returnedQty = returnedQty,
                returnedPrice = returnedPrice,
                returnDate = returnDate,
                refundedAmount = refundedAmount,
                notes = notes
            )
        }
    }

    fun voidPurchase(purchaseId: Int) {
        viewModelScope.launch {
            repository.deletePurchase(purchaseId)
        }
    }

    fun updateSupplierPurchase(
        purchaseId: Int,
        newPaidAmount: Double,
        newPaymentMethod: String
    ) {
        viewModelScope.launch {
            repository.updatePurchaseInvoice(purchaseId, newPaidAmount, newPaymentMethod)
        }
    }

    fun editSupplierPurchase(
        purchaseId: Int,
        newSupplierId: Int,
        newDate: String,
        newItemsPriceAndQty: List<Triple<InventoryItem, Double, Double>>,
        newPaidAmount: Double,
        newPaymentMethod: String
    ) {
        viewModelScope.launch {
            repository.editSupplierPurchaseFull(
                purchaseId,
                newSupplierId,
                newDate,
                newItemsPriceAndQty,
                newPaidAmount,
                newPaymentMethod
            )
        }
    }

    fun getPurchaseItemsFlow(purchaseId: Int): Flow<List<SupplierPurchaseItem>> {
        return repository.getPurchaseItems(purchaseId)
    }

    // --- Expenses ---
    fun recordExpense(category: String, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.addExpense(
                Expense(category = category, amount = amount, date = Helpers.getCurrentDateTime(), notes = notes)
            )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.addExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // --- Transfers ---
    fun recordTransfer(amount: Double, sender: String, receiver: String, referenceNumber: String, statement: String, notes: String, currency: String = "ريال يمني") {
        viewModelScope.launch {
            repository.addTransfer(
                MoneyTransfer(
                    amount = amount,
                    sender = sender,
                    receiver = receiver,
                    referenceNumber = referenceNumber,
                    statement = statement,
                    notes = notes,
                    date = Helpers.getCurrentDateTime(),
                    currency = currency
                )
            )
        }
    }

    fun updateTransfer(transfer: MoneyTransfer) {
        viewModelScope.launch {
            repository.addTransfer(transfer)
        }
    }

    fun deleteTransfer(transfer: MoneyTransfer) {
        viewModelScope.launch {
            repository.deleteTransfer(transfer)
        }
    }

    // --- Splash cinematic timer ---
    init {
        viewModelScope.launch {
            fetchTodayTax()
            delay(2800) // Soft cinematic transition for splash screen
            val lastUser = appPrefs.getString("remembered_user", null)
            val isLocked = appPrefs.getBoolean("app_locked_v2", false)
            if (lastUser != null && !isLocked) {
                isLoggedIn.value = true
                _currentScreen.value = Screen.Dashboard
            } else {
                _currentScreen.value = Screen.Login
            }
        }
    }

    fun attemptLogin(username: String, pass: String, remember: Boolean): Boolean {
        val correctPass = getAdminPassword()
        if (username.trim() == adminUsername && pass == correctPass) {
            isLoggedIn.value = true
            if (remember) {
                appPrefs.edit().putString("remembered_user", username).apply()
            } else {
                appPrefs.edit().remove("remembered_user").apply()
            }
            _currentScreen.value = Screen.Dashboard
            return true
        }
        return false
    }

    fun logout() {
        isLoggedIn.value = false
        appPrefs.edit().remove("remembered_user").apply()
        _currentScreen.value = Screen.Login
    }

    fun runStressTestingAndLoad(invoiceCount: Int, customerCount: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.generateStressTestDataDirectly(invoiceCount, customerCount)
            onComplete()
        }
    }

    fun purgeSystemDatabase(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearDatabaseDirectly()
            onComplete()
        }
    }
}

class AppViewModelFactory(
    private val application: Application,
    private val repository: QatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
