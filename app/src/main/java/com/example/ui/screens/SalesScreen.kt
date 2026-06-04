package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryItem
import com.example.data.SalesInvoice
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import com.example.util.BluetoothPrinterManager
import com.example.util.Helpers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.example.ui.components.BluetoothPrintDialog
import com.example.ui.components.UnifiedPeriodSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val printerManager = remember { BluetoothPrinterManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    val allInvoices by viewModel.allInvoices.collectAsState()
    val allInvoiceItems by viewModel.allInvoiceItems.collectAsState()
    val allProducts by viewModel.inventoryItems.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()
    val allPayments by viewModel.allPayments.collectAsState()
    val todayTax by viewModel.todayTaxAmount.collectAsState()

    var searchInvoiceQuery by remember { mutableStateOf("") }
    var showReportsDialog by remember { mutableStateOf(false) }
    var selectedReportPeriod by remember { mutableStateOf("يومي") } // يومي, أسبوعي, شهري, سنوي
    var customReportType by remember { mutableStateOf("يوم") }
    var customReportValue by remember { mutableStateOf(Helpers.getCurrentDate()) }
    var customReportPrintDate by remember { mutableStateOf(Helpers.getCurrentDate()) }

    var isAddingInvoice by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf("ريال يمني") }

    // State for creating new invoice
    var customerNameInput by remember { mutableStateOf("") }
    var customerPhoneInput by remember { mutableStateOf("") } // Added customer phone for new customer creation
    val selectedItemsList = remember { mutableStateListOf<Pair<InventoryItem, Double>>() } // Item & Quantity sold
    var discountInput by remember { mutableStateOf("") }
    var paidAmountInput by remember { mutableStateOf("") }
    var invoicePaymentMethod by remember { mutableStateOf("نقداً") }
    
    // UI selections
    var showProductPicker by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    
    // Edit Invoice States
    var showEditInvoiceDialog by remember { mutableStateOf(false) }
    var editInvoiceCustomerName by remember { mutableStateOf("") }
    var editInvoicePaidAmount by remember { mutableStateOf("") }
    var editInvoicePaymentMethod by remember { mutableStateOf("نقداً") }
    
    // View Details dialog
    var viewingInvoice by remember { mutableStateOf<SalesInvoice?>(null) }
    var viewingInvoiceItems by remember { mutableStateOf<List<com.example.data.SalesInvoiceItem>>(emptyList()) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewingInvoice) {
        viewingInvoice?.let { inv ->
            viewModel.getSaleItemsFlow(inv.id).collect { items ->
                viewingInvoiceItems = items
            }
        }
    }

    // Direct inputs for new customer's extra details
    var customerAddressInput by remember { mutableStateOf("") }
    var customerNotesInput by remember { mutableStateOf("") }

    // Void Invoice confirmation
    var invoiceToVoid by remember { mutableStateOf<SalesInvoice?>(null) }
    var showVoidConfirm by remember { mutableStateOf(false) }

    var showBluetoothPrintTrigger by remember { mutableStateOf(false) }
    var bluetoothPrintText by remember { mutableStateOf("") }

    // Calculations for active sale creation
    val itemsSubtotal = selectedItemsList.sumOf { it.first.sellPrice * it.second }
    val finalTotal = itemsSubtotal

    LaunchedEffect(invoicePaymentMethod, finalTotal) {
        if (invoicePaymentMethod in listOf("نقداً", "تحويل", "إيداع")) {
            paidAmountInput = if (finalTotal == 0.0) "" else finalTotal.toString()
        } else if (invoicePaymentMethod == "آجل") {
            paidAmountInput = "0"
        } else if (invoicePaymentMethod == "دفع جزئي") {
            if (paidAmountInput == finalTotal.toString() || paidAmountInput == "0" || paidAmountInput == "0.0") {
                paidAmountInput = ""
            }
        }
    }

    val paidAmount = paidAmountInput.toDoubleOrNull() ?: 0.0
    val remainingDebt = if (finalTotal > paidAmount) finalTotal - paidAmount else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAddingInvoice) "إنشاء فاتورة بيع جديدة" else "إدارة المبيعات والفواتير", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isAddingInvoice) {
                            isAddingInvoice = false
                        } else {
                            viewModel.navigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PalWhitePure)
                    }
                },
                actions = {
                    if (!isAddingInvoice) {
                        IconButton(onClick = { showReportsDialog = true }) {
                            Icon(Icons.Default.Summarize, contentDescription = "تقارير المبيعات", tint = PalGoldCalligraphy)
                        }
                        IconButton(onClick = {
                            isAddingInvoice = true
                            customerNameInput = ""
                            customerPhoneInput = ""
                            customerAddressInput = ""
                            customerNotesInput = ""
                            selectedItemsList.clear()
                            discountInput = "0"
                            paidAmountInput = ""
                            selectedCurrency = "ريال يمني"
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "جديد", tint = PalGreenLight)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PalBlackNormal)
            )
        },
        containerColor = PalBlackDark
    ) { innerPadding ->
        if (isAddingInvoice) {
            // New design layout for making a sale
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer selection
                Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("بيانات العميل المشتري", color = PalWhiteMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = customerNameInput,
                                onValueChange = { customerNameInput = it },
                                placeholder = {
                                    val isCash = invoicePaymentMethod == "نقداً"
                                    val hintText = if (isCash) "اسم العميل (اختياري للبيع النقدي)" else "أدخل اسم العميل (أو انقر اختيار لتحديد عميل مسجل)"
                                    Text(hintText, color = PalWhiteMuted, fontSize = 12.sp)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PalGreenLight,
                                    unfocusedBorderColor = PalBlackLight,
                                    focusedTextColor = PalWhitePure,
                                    unfocusedTextColor = PalWhiteSoft
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showCustomerPicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PalBlackLight)
                            ) {
                                Text("إختيار", color = PalWhitePure)
                            }
                        }

                        val isCustomerNew = customerNameInput.trim().isNotEmpty() && allCustomers.none { it.name.trim().equals(customerNameInput.trim(), ignoreCase = true) }
                        AnimatedVisibility(visible = isCustomerNew) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = customerPhoneInput,
                                    onValueChange = { customerPhoneInput = it },
                                    placeholder = { Text("رقم هاتف العميل الجديد (تلقائي الحفظ)", color = PalWhiteMuted, fontSize = 12.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PalGreenLight,
                                        unfocusedBorderColor = PalBlackLight,
                                        focusedTextColor = PalWhitePure,
                                        unfocusedTextColor = PalWhiteSoft
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = customerAddressInput,
                                    onValueChange = { customerAddressInput = it },
                                    placeholder = { Text("عنوان العميل الجديد السكني/التجاري (اختياري)", color = PalWhiteMuted, fontSize = 12.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PalGreenLight,
                                        unfocusedBorderColor = PalBlackLight,
                                        focusedTextColor = PalWhitePure,
                                        unfocusedTextColor = PalWhiteSoft
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = customerNotesInput,
                                    onValueChange = { customerNotesInput = it },
                                    placeholder = { Text("ملاحظات خاصة ومميزة عن العميل الجديد (اختياري)", color = PalWhiteMuted, fontSize = 12.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PalGreenLight,
                                        unfocusedBorderColor = PalBlackLight,
                                        focusedTextColor = PalWhitePure,
                                        unfocusedTextColor = PalWhiteSoft
                                    ),
                                    singleLine = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Selected Products table
                Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("أصناف الفاتورة", color = PalWhiteMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Button(
                                onClick = { showProductPicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة صنف", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (selectedItemsList.isEmpty()) {
                            Text(
                                "لم يتم إدراج أي مواد قات بعد بالفاتورة",
                                color = PalWhiteMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        } else {
                            selectedItemsList.forEachIndexed { index, pair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text(pair.first.name, color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("المخزون المتاح: ${pair.first.quantity.toInt()} حزمة / حبة", color = Color(0xFFAAAAAA), fontSize = 10.sp)
                                    }
                                    
                                    // Quantity management (+/- buttons)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1.0f),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(onClick = {
                                            if (pair.second > 1) {
                                                selectedItemsList[index] = pair.first to (pair.second - 1)
                                            } else {
                                                selectedItemsList.removeAt(index)
                                            }
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Remove, contentDescription = null, tint = PalRedLight, modifier = Modifier.size(16.dp))
                                        }
                                        Text("${pair.second.toInt()}", color = PalWhiteSoft, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                        IconButton(onClick = {
                                            val currentItemStock = pair.first.quantity
                                            if (pair.second < currentItemStock) {
                                                selectedItemsList[index] = pair.first to (pair.second + 1)
                                            } else {
                                                android.widget.Toast.makeText(context, "الكمية المحددة تخطت المخزون المتاح (${currentItemStock.toInt()})!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = PalGreenLight, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Unit Sale Price dynamic input field
                                    var sellPriceInputText by remember(pair.first.sellPrice) { 
                                        mutableStateOf(if (pair.first.sellPrice == 0.0) "" else pair.first.sellPrice.toInt().toString()) 
                                    }
                                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        OutlinedTextField(
                                            value = sellPriceInputText,
                                            onValueChange = { input ->
                                                val clean = input.filter { it.isDigit() }
                                                sellPriceInputText = clean
                                                val price = clean.toDoubleOrNull() ?: 0.0
                                                selectedItemsList[index] = pair.first.copy(sellPrice = price) to pair.second
                                            },
                                            placeholder = { Text("سعر الحبة", color = PalWhiteMuted, fontSize = 10.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PalGreenLight,
                                                unfocusedBorderColor = PalBlackLight,
                                                focusedTextColor = PalWhitePure,
                                                unfocusedTextColor = PalWhiteSoft
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().height(42.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center)
                                        )
                                    }

                                    // Total for this item
                                    Text(
                                        Helpers.formatMoney(pair.first.sellPrice * pair.second),
                                        color = PalWhitePure,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1.0f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                Divider(color = PalBlackLight, thickness = 1.dp)
                            }
                        }
                    }
                }

                // Invoicing Financial Box (Discount, Tax, Totals)
                Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("التحاسب المالي", color = PalWhiteMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المجموع الفرعي للأصناف:", color = PalWhiteSoft, fontSize = 13.sp)
                            Text(Helpers.formatMoney(itemsSubtotal), color = PalWhitePure, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("عدد الأصناف المدرجة بالفاتورة:", color = PalWhiteSoft, fontSize = 13.sp)
                            Text("${selectedItemsList.size} أصناف", color = PalWhitePure, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي الكميات المباعة:", color = PalWhiteSoft, fontSize = 13.sp)
                            Text("${selectedItemsList.sumOf { it.second }.toInt()} حزمة", color = PalWhitePure, fontWeight = FontWeight.Bold)
                        }

                        Divider(color = PalBlackLight, thickness = 1.dp)

                        Text("طريقة التحصيل والدفع الكلي:", color = PalWhiteSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val salePaymentMethods = listOf("نقداً", "تحويل", "إيداع", "آجل", "دفع جزئي")
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            salePaymentMethods.forEach { method ->
                                val isSel = invoicePaymentMethod == method
                                val bg = if (isSel) PalGreenNormal else PalBlackLight
                                val fg = if (isSel) Color.White else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bg)
                                        .clickable { invoicePaymentMethod = method }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(text = method, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Inputs for cash paid based on method selected
                        val isPaidAmountEditable = invoicePaymentMethod == "دفع جزئي"
                        if (isPaidAmountEditable) {
                            OutlinedTextField(
                                value = paidAmountInput,
                                onValueChange = { paidAmountInput = it },
                                label = { Text("المبلغ المدفوع (ريال) *", color = PalWhiteMuted, fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            val placeholderLabel = when (invoicePaymentMethod) {
                                "آجل" -> "المبلغ المدفوع بقيمة 0 (شراء بالكامل بالآجل)"
                                else -> "المبلغ المدفوع بالكامل (معاملة مدفوعة بالكامل)"
                            }
                            OutlinedTextField(
                                value = if (invoicePaymentMethod == "آجل") "0" else finalTotal.toString(),
                                onValueChange = {},
                                enabled = false,
                                label = { Text(placeholderLabel, color = PalWhiteMuted, fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = PalBlackLight,
                                    disabledTextColor = PalWhiteMuted
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Divider(color = PalBlackLight, thickness = 1.dp)

                        Text("عملة الفاتورة المعتمدة:", color = PalWhiteSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("ريال يمني", "ريال سعودي").forEach { curr ->
                                val isSel = selectedCurrency == curr
                                val bg = if (isSel) PalGreenNormal else PalBlackLight
                                val fg = if (isSel) Color.White else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bg)
                                        .clickable { selectedCurrency = curr }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = curr, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Divider(color = PalBlackLight, thickness = 1.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي قيمة الفاتورة:", color = PalWhiteSoft, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(Helpers.formatMoney(finalTotal), color = PalGreenLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الآجل المتبقي (دين على العميل):", color = PalWhiteSoft, fontSize = 13.sp)
                            Text(Helpers.formatMoney(remainingDebt), color = if (remainingDebt > 0) PalRedLight else PalWhitePure, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Create Action Button
                Button(
                    onClick = {
                        val paidFull = paidAmount >= finalTotal
                        val isCashTransferDeposit = invoicePaymentMethod in listOf("نقداً", "تحويل", "إيداع")
                        val finalCustomerName = customerNameInput.trim().ifEmpty { "عميل نقدي" }

                        // Validate customer name based on payment and method
                        if (paidFull && isCashTransferDeposit) {
                            // Paid in full with cash/transfer/deposit: can save without customer name.
                        } else {
                            // Partial payment or credit (آجل): requires real customer name.
                            if (customerNameInput.trim().isEmpty()) {
                                android.widget.Toast.makeText(context, "البيع بالآجل أو الدفع الجزئي يتطلب تحديد اسم العميل لإثبات المديونية!", android.widget.Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (finalCustomerName == "عميل نقدي") {
                                android.widget.Toast.makeText(context, "معاملات الذمم والآجل والدفع الجزئي لا يمكن قيدها للعميل النقدي العام!", android.widget.Toast.LENGTH_LONG).show()
                                return@Button
                            }
                        }

                        if (invoicePaymentMethod == "دفع جزئي") {
                            if (paidAmount <= 0.0) {
                                android.widget.Toast.makeText(context, "المبلغ المدفوع يجب أن يكون أكبر من الصفر في الدفع الجزئي! أو حدد البيع بالآجل كحساب كلي.", android.widget.Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (paidAmount >= finalTotal) {
                                android.widget.Toast.makeText(context, "المبلغ المدفوع يجب أن يكون أقل من إجمالي الفاتورة في الدفع الجزئي! أو حدد خيار الدفع نقداً.", android.widget.Toast.LENGTH_LONG).show()
                                return@Button
                            }
                        }

                        if (selectedItemsList.isEmpty()) {
                            android.widget.Toast.makeText(context, "الرجاء تحديد الأطواق وإدراج صنف مبيعات واحد على الأقل!", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        // Save Sale action
                        viewModel.executeSale(
                            customerName = finalCustomerName,
                            customerPhone = customerPhoneInput.trim(),
                            customerAddress = customerAddressInput.trim(),
                            customerNotes = customerNotesInput.trim(),
                            items = selectedItemsList.toList(),
                            discount = 0.0,
                            paidAmount = paidAmount,
                            paymentMethod = invoicePaymentMethod,
                            currency = selectedCurrency,
                            onSuccess = { invoiceId ->
                                android.widget.Toast.makeText(context, "تم قيد الفاتورة وحفظ المعاملة وتحديث قيود الذمم والقات بنجاح", android.widget.Toast.LENGTH_LONG).show()
                                isAddingInvoice = false
                                customerNameInput = ""
                                customerPhoneInput = ""
                                customerAddressInput = ""
                                customerNotesInput = ""
                                selectedItemsList.clear()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("حفظ وتسجيل عملية البيع", color = PalWhitePure, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // General invoices history list
            val filteredInvoices = allInvoices.filter { inv ->
                val query = searchInvoiceQuery.trim()
                if (query.isEmpty()) return@filter true
                
                // Helper to normalize Arabic diacritics and letters for flawless matching
                fun normalizeArabic(text: String): String {
                    return text.trim()
                        .lowercase()
                        .replace("[ًٌٍَُِّْ]".toRegex(), "") // Remove diacritics
                        .replace("[أإآ]".toRegex(), "ا")   // Normalize Alefs to bare Alef
                        .replace("ة", "ه")                    // Normalize Teh Marbuta to Heh
                        .replace("ى", "ي")                    // Normalize Alef Maksura to Yeh
                }

                val queryNorm = normalizeArabic(query)

                // Robust payment method matching
                val matchesPaymentMethod = when {
                    queryNorm.contains("نقد") || queryNorm == "كاش" -> {
                        inv.paymentMethod == "نقداً"
                    }
                    queryNorm.contains("اجل") || queryNorm.contains("دين") || queryNorm.contains("ذم") -> {
                        inv.paymentMethod == "آجل"
                    }
                    queryNorm.contains("تحويل") || queryNorm.contains("حوال") -> {
                        inv.paymentMethod == "تحويل"
                    }
                    queryNorm.contains("ايداع") || queryNorm.contains("كريمي") -> {
                        inv.paymentMethod == "إيداع"
                    }
                    queryNorm.contains("جزي") || queryNorm.contains("قسط") || queryNorm.contains("دفع") || queryNorm.contains("بعض") -> {
                        inv.paymentMethod == "دفع جزئي"
                    }
                    else -> {
                        normalizeArabic(inv.paymentMethod).contains(queryNorm)
                    }
                }

                // Robust receipt/sened number search: extracts any digits (including Eastern Arabic digits) from the search query
                // so that typing e.g. "سند 4" or "فاتورة 4" or "سند ٤" or just "4" will properly match bill ID 4
                val cleanQuery = query.lowercase()
                val normalizedQueryForDigits = cleanQuery.map { ch ->
                    if (ch in '٠'..'٩') {
                        (ch - '٠' + '0'.code).toChar()
                    } else {
                        ch
                    }
                }.joinToString("")
                val digitsOnly = normalizedQueryForDigits.filter { it.isDigit() }
                val matchesReceipt = if (digitsOnly.isNotEmpty()) {
                    inv.id.toString() == digitsOnly || inv.id.toString().contains(digitsOnly)
                } else {
                    false
                }

                val matchesBasic = normalizeArabic(inv.customerName).contains(queryNorm) ||
                        inv.customerName.contains(query, ignoreCase = true) ||
                        inv.id.toString() == query ||
                        inv.id.toString() == digitsOnly ||
                        matchesReceipt ||
                        inv.date.contains(query) ||
                        matchesPaymentMethod ||
                        inv.totalAmount.toString().contains(query)
                
                if (matchesBasic) return@filter true
                
                // Also search items inside this invoice
                val matchesItems = allInvoiceItems.any { item ->
                    item.invoiceId == inv.id && (
                        normalizeArabic(item.itemName).contains(queryNorm) || 
                        item.itemName.contains(query, ignoreCase = true)
                    )
                }
                
                matchesBasic || matchesItems
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header with smart search hint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "محرك البحث والتحري الذكي",
                        color = PalGoldCalligraphy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "يدعم: رقم السند، اسم العميل، التاريخ، طريقة الدفع، المبلغ",
                        color = PalWhiteMuted,
                        fontSize = 9.sp
                    )
                }

                // Advanced Search Input
                OutlinedTextField(
                    value = searchInvoiceQuery,
                    onValueChange = { searchInvoiceQuery = it },
                    placeholder = { Text("ابحث برقم السند، العميل، التاريخ، المبلغ، طريقة الدفع...", color = PalWhiteMuted, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PalGreenLight, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchInvoiceQuery.isNotEmpty()) {
                            IconButton(onClick = { searchInvoiceQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = PalRedNormal, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PalGreenLight,
                        unfocusedBorderColor = PalBlackLight,
                        focusedTextColor = PalWhitePure,
                        unfocusedTextColor = PalWhiteSoft
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Advanced Filter Chips to instant-search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterChips = listOf("الكل", "نقداً", "دين آجل", "إيداع", "حوالة")
                    filterChips.forEach { label ->
                        val isSelected = if (label == "الكل") searchInvoiceQuery.isEmpty() else searchInvoiceQuery.equals(label, ignoreCase = true)
                        
                        Box(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PalGreenLight else PalWhiteMuted.copy(0.2f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .background(
                                    color = if (isSelected) PalGreenLight.copy(0.12f) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    searchInvoiceQuery = if (label == "الكل") "" else label
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) PalGreenLight else PalWhiteSoft,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Match counter
                if (filteredInvoices.isNotEmpty()) {
                    Text(
                        text = "عُثر على: ${filteredInvoices.size} حركات/فواتير مطابقة",
                        color = PalWhiteMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                }

                if (filteredInvoices.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(if (allInvoices.isEmpty()) "لا يوجد سجل فواتير مسجل حاليا" else "لا توجد نتائج بحث مطابقة", color = PalWhiteMuted, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredInvoices) { inv ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewingInvoice = inv
                                        showDetailsDialog = true
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("رقم السند: #${inv.id}", color = PalWhiteMuted, fontSize = 11.sp)
                                        Text(inv.date, color = PalWhiteMuted, fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "العميل: ${inv.customerName}",
                                        color = PalWhitePure,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("إجمالي الفاتورة", color = PalWhiteMuted, fontSize = 11.sp)
                                            Text(Helpers.formatMoney(inv.totalAmount) + " " + inv.currency, color = PalGreenLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("المدفوع نقدا", color = PalWhiteMuted, fontSize = 11.sp)
                                            Text(Helpers.formatMoney(inv.paidAmount) + " " + inv.currency, color = PalWhitePure, fontSize = 13.sp)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("الآجل / المتبقي", color = PalWhiteMuted, fontSize = 11.sp)
                                            Text(
                                                Helpers.formatMoney(inv.debtAmount) + " " + inv.currency,
                                                color = if (inv.debtAmount > 0) PalRedLight else PalWhitePure,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = {
                                            invoiceToVoid = inv
                                            showVoidConfirm = true
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "إلغاء الفاتورة", tint = PalRedLight)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Product Selection Picker Dialog ---
    if (showProductPicker) {
        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("تحديد صنف القات والكمية", color = PalWhitePure, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    var selectedProd by remember { mutableStateOf<InventoryItem?>(null) }
                    var sellQtyInput by remember { mutableStateOf("1") }
                    var searchProductQuery by remember { mutableStateOf("") }

                    Text("ابحث عن الصنف المتوفر:", color = PalWhiteMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    OutlinedTextField(
                        value = searchProductQuery,
                        onValueChange = { searchProductQuery = it },
                        placeholder = { Text("اكتب اسم الصنف للبحث...", color = PalWhiteMuted, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PalGreenLight, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            focusedContainerColor = PalBlackDark,
                            unfocusedContainerColor = PalBlackDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    val filteredProductsList = remember(allProducts, searchProductQuery) {
                        allProducts.filter { it.name.contains(searchProductQuery, ignoreCase = true) }
                    }

                    Text("اختر الصنف المتوفر من المخزن:", color = PalWhiteMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .border(1.dp, PalBlackLight, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column {
                            filteredProductsList.forEach { prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (selectedProd == prod) PalGreenDark else Color.Transparent)
                                        .clickable { selectedProd = prod }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(prod.name, color = PalWhitePure, fontSize = 13.sp)
                                    Text("المخزون المتوفر: ${prod.quantity.toInt()} حبة", color = PalWhiteMuted, fontSize = 11.sp)
                                }
                                Divider(color = PalBlackLight)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = sellQtyInput,
                        onValueChange = { sellQtyInput = it },
                        label = { Text("الكمية المطلوبة (حبة)", color = PalWhiteMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (selectedProd != null) {
                                val qtyToSell = sellQtyInput.toDoubleOrNull() ?: 1.0
                                if (qtyToSell > selectedProd!!.quantity) {
                                    android.widget.Toast.makeText(context, "فشل: الكمية المطلوبة غير متوفرة بالمخزن!", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    selectedItemsList.add(selectedProd!! to qtyToSell)
                                    showProductPicker = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إدراج بالفاتورة", color = PalWhitePure)
                    }
                }
            },
            confirmButton = {},
            containerColor = PalBlackNormal
        )
    }

    // --- Registered Customers Picker Dialog ---
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("تحديد عميل مسجل بمربع بحث", color = PalWhitePure, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    var searchCustomerQuery by remember { mutableStateOf("") }

                    Text("ابحث باسم العميل المسجل:", color = PalWhiteMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    OutlinedTextField(
                        value = searchCustomerQuery,
                        onValueChange = { searchCustomerQuery = it },
                        placeholder = { Text("اكتب اسم العميل للبحث السريع...", color = PalWhiteMuted, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PalGreenLight, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    val filteredCustomersList = remember(allCustomers, searchCustomerQuery) {
                        allCustomers.filter { it.name.contains(searchCustomerQuery, ignoreCase = true) }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .border(1.dp, PalBlackLight, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        if (filteredCustomersList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا توجد نتائج بحث مطابقة", color = PalWhiteMuted, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredCustomersList) { cust ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                customerNameInput = cust.name
                                                showCustomerPicker = false
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text(cust.name, color = PalWhitePure, fontWeight = FontWeight.Bold)
                                    }
                                    Divider(color = PalBlackLight)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = PalBlackNormal
        )
    }

    // --- Details Viewing Dialog ---
    if (showDetailsDialog && viewingInvoice != null) {
        val targetCustomer = allCustomers.find { it.name.trim() == viewingInvoice!!.customerName.trim() }
        val customerPayments = if (targetCustomer != null) {
            allPayments.filter { it.customerId == targetCustomer.id }
        } else {
            emptyList()
        }

        val msg = buildString {
            appendLine("وكالة عاهد الصبري")
            appendLine("فاتورة مبيعات للعميل: ${viewingInvoice!!.customerName}")
            appendLine("رقم الفاتورة: #${viewingInvoice!!.id}")
            appendLine("تاريخ التصدير: ${viewingInvoice!!.date}")
            appendLine("طريقة الدفع: ${viewingInvoice!!.paymentMethod}")
            appendLine("---------------------------------")
            for (item in viewingInvoiceItems) {
                appendLine("${item.itemName} x ${item.quantity.toInt()} حبة = ${Helpers.formatMoney(item.quantity * item.unitPrice)}")
            }
            appendLine("---------------------------------")
            appendLine("الإجمالي الصافي للفاتورة: ${Helpers.formatMoney(viewingInvoice!!.totalAmount)}")
            appendLine("المبلغ المدفوع عند الشراء: ${Helpers.formatMoney(viewingInvoice!!.paidAmount)}")
            
            if (customerPayments.isNotEmpty()) {
                appendLine("---------------------------------")
                appendLine("💰 سجل الدفعات والمسددات المقبوضة لاحقاً:")
                customerPayments.forEachIndexed { i, p ->
                    appendLine(" • دفعة #${i + 1}: بقيمة ${Helpers.formatMoney(p.amount)} ريال (${p.paymentMethod}) بتاريخ ${p.date}")
                    if (p.notes.isNotEmpty()) appendLine("   البيان: ${p.notes}")
                }
            }
            appendLine("---------------------------------")
            appendLine("رصيد الدين المتبقي المعلق: ${Helpers.formatMoney(viewingInvoice!!.debtAmount)}")
            appendLine("صاحب المفرش والوكالة: عاهد الصبري")
        }

        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("تفاصيل فاتورة البيع #${viewingInvoice!!.id}", color = PalWhitePure, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("العميل: ${viewingInvoice!!.customerName}", color = PalWhitePure, fontWeight = FontWeight.Bold)
                    Text("التاريخ والوقت: ${viewingInvoice!!.date}", color = PalWhiteMuted, fontSize = 12.sp)
                    Divider(color = PalBlackLight)

                    Text("الأصناف المتضمنة بالفاتورة:", color = PalWhiteMuted, fontSize = 12.sp)
                    viewingInvoiceItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.itemName, color = PalWhitePure, fontSize = 13.sp)
                            Text("${item.quantity} حبة بسعر ${Helpers.formatMoney(item.unitPrice)} ${viewingInvoice!!.currency}", color = PalWhiteSoft, fontSize = 12.sp)
                        }
                    }

                    Divider(color = PalBlackLight)

                    Text("الإجمالي الصافي: ${Helpers.formatMoney(viewingInvoice!!.totalAmount)} ${viewingInvoice!!.currency}", color = PalWhitePure, fontWeight = FontWeight.Bold)
                    Text("المبلغ المدفوع عند الشراء: ${Helpers.formatMoney(viewingInvoice!!.paidAmount)} ${viewingInvoice!!.currency} (${viewingInvoice!!.paymentMethod})", color = PalGreenLight)
                    
                    if (customerPayments.isNotEmpty()) {
                        Divider(color = PalBlackLight)
                        Text("💰 سجل حركات تسديد ديون العميل لاحقاً:", color = PalWhiteMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        customerPayments.forEachIndexed { i, p ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PalBlackDark, RoundedCornerShape(4.dp))
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("دفعة #${i + 1} (${p.paymentMethod})", color = PalWhiteSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(Helpers.formatMoney(p.amount) + " ريال", color = PalGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (p.notes.isNotEmpty()) {
                                    Text("البيان: ${p.notes}", color = PalWhiteMuted, fontSize = 10.sp)
                                }
                                Text("التاريخ: ${p.date}", color = PalWhiteMuted, fontSize = 9.sp)
                            }
                        }
                    }

                    Divider(color = PalBlackLight)
                    Text("المتبقي دين معلق حالياً: ${Helpers.formatMoney(viewingInvoice!!.debtAmount)} ${viewingInvoice!!.currency}", color = if (viewingInvoice!!.debtAmount > 0) PalRedLight else PalWhitePure, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                // PDF Generation
                                val headers = listOf("الصنف", "الكمية", "سعر الوحدة", "الإجمالي")
                                val rows = viewingInvoiceItems.map {
                                    listOf(it.itemName, "${it.quantity} حبة", Helpers.formatMoney(it.unitPrice) + " " + viewingInvoice!!.currency, Helpers.formatMoney(it.quantity * it.unitPrice) + " " + viewingInvoice!!.currency)
                                }
                                val totalsMap = mutableMapOf(
                                    "المجموع النهائي للفاتورة" to Helpers.formatMoney(viewingInvoice!!.totalAmount) + " " + viewingInvoice!!.currency,
                                    "المسدد نقداً عند الشراء" to Helpers.formatMoney(viewingInvoice!!.paidAmount) + " " + viewingInvoice!!.currency
                                )
                                customerPayments.forEachIndexed { i, p ->
                                    totalsMap["دفعة لاحقة #${i + 1} (${p.date.split(" ")[0]})"] = Helpers.formatMoney(p.amount) + " " + viewingInvoice!!.currency
                                }
                                totalsMap["المتبقي معلق في الذمة"] = Helpers.formatMoney(viewingInvoice!!.debtAmount) + " " + viewingInvoice!!.currency

                                Helpers.generatePdfAndShare(
                                    context = context,
                                    title = "فاتورة تفصيلية رقم #${viewingInvoice!!.id} لتسجيل الدفعات",
                                    headers = headers,
                                    rows = rows,
                                    totals = totalsMap
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("PDF", color = PalWhitePure, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                Helpers.shareViaWhatsApp(context, "", msg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("واتساب", color = PalWhitePure, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                bluetoothPrintText = msg
                                showBluetoothPrintTrigger = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGoldCalligraphy),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("طباعة 🖨️", color = PalWhitePure, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            editInvoiceCustomerName = viewingInvoice!!.customerName
                            editInvoicePaidAmount = viewingInvoice!!.paidAmount.toString()
                            editInvoicePaymentMethod = viewingInvoice!!.paymentMethod
                            showEditInvoiceDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                    ) {
                        Text("تعديل الفاتورة ✏️", color = PalWhitePure, fontSize = 12.sp)
                    }
                    TextButton(onClick = { showDetailsDialog = false }) {
                        Text("إغلاق", color = PalWhiteMuted)
                    }
                }
            },
            containerColor = PalBlackNormal
        )
    }

    // --- Edit Invoice Dialog ---
    if (showEditInvoiceDialog && viewingInvoice != null) {
        AlertDialog(
            onDismissRequest = { showEditInvoiceDialog = false },
            title = {
                Text(
                    text = "تعديل الفاتورة رقم #${viewingInvoice!!.id}",
                    color = PalWhitePure,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editInvoiceCustomerName,
                        onValueChange = { editInvoiceCustomerName = it },
                        label = { Text("اسم العميل", color = PalWhiteMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft,
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editInvoicePaidAmount,
                        onValueChange = { editInvoicePaidAmount = it },
                        label = { Text("المبلغ المدفوع (ريال)", color = PalWhiteMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft,
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("طريقة التحصيل:", color = PalWhiteSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val salePaymentMethods = listOf("نقداً", "آجل", "إيداع", "تحويل")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        salePaymentMethods.forEach { method ->
                            val isSel = editInvoicePaymentMethod == method
                            val bg = if (isSel) PalGreenNormal else PalBlackLight
                            val fg = if (isSel) Color.White else Color.Gray
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bg)
                                    .clickable { editInvoicePaymentMethod = method }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(method, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPaid = editInvoicePaidAmount.toDoubleOrNull() ?: 0.0
                        viewModel.updateSalesInvoice(
                            invoiceId = viewingInvoice!!.id,
                            newCustomerName = editInvoiceCustomerName,
                            newPaidAmount = newPaid,
                            newPaymentMethod = editInvoicePaymentMethod
                        )
                        showEditInvoiceDialog = false
                        showDetailsDialog = false
                        android.widget.Toast.makeText(context, "تم تعديل الفاتورة وتعديل ديون العميل بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                ) {
                    Text("حفظ التعديلات", color = PalWhitePure)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditInvoiceDialog = false }) {
                    Text("إلغاء", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    // --- Void (Delete) Invoice Confirm Dialog ---
    if (showVoidConfirm && invoiceToVoid != null) {
        AlertDialog(
            onDismissRequest = { showVoidConfirm = false },
            title = { Text("إلغاء وإعدام الفاتورة والمبيعات؟", color = PalWhitePure) },
            text = { Text("هل أنت متأكد من إلغاء الفاتورة #${invoiceToVoid!!.id} للعميل ${invoiceToVoid!!.customerName}؟ سيتم إرجاع كميات المواد للمخزن تلقائيا وإسقاط بقايا الديون.", color = PalWhiteMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.voidSale(invoiceToVoid!!.id)
                        showVoidConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal)
                ) {
                    Text("نعم، إلغاء السند", color = PalWhitePure)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoidConfirm = false }) {
                    Text("تراجع", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showReportsDialog) {
        var searchReportQuery by remember { mutableStateOf("") }
        val todayStr = Helpers.getCurrentDate()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val todayTime = sdf.parse(todayStr)?.time ?: 0L
        val isWeekly: (String) -> Boolean = { dateStr ->
            val itemDate = dateStr.take(10)
            try {
                val itemTime = sdf.parse(itemDate)?.time ?: 0L
                val diff = todayTime - itemTime
                diff in 0..(7L * 24 * 60 * 60 * 1000)
            } catch (e: Exception) {
                false
            }
        }

        val periodInvoices = when (selectedReportPeriod) {
            "يومي" -> allInvoices.filter { it.date.startsWith(todayStr) }
            "أسبوعي" -> allInvoices.filter { isWeekly(it.date) }
            "شهري" -> allInvoices.filter { it.date.startsWith(todayStr.substring(0, 7)) }
            "سنوي" -> allInvoices.filter { it.date.startsWith(todayStr.substring(0, 4)) }
            "تقويم مخصص" -> allInvoices.filter { it.date.startsWith(customReportValue) }
            else -> allInvoices
        }

        val totalSalesAmount = periodInvoices.sumOf { it.totalAmount }
        val totalPaidAmount = periodInvoices.sumOf { it.paidAmount }
        val totalDebtAmount = periodInvoices.sumOf { it.debtAmount }

        val periodInvoiceIds = periodInvoices.map { it.id }.toSet()
        val periodInvoiceItems = allInvoiceItems.filter { it.invoiceId in periodInvoiceIds }

        // Group by item name to show sales summary
        val itemSalesRows = periodInvoiceItems.groupBy { it.itemName }.map { (name, group) ->
            val qty = group.sumOf { it.quantity }
            val amount = group.sumOf { it.quantity * it.unitPrice }
            Pair(name, Pair(qty, amount))
        }.sortedByDescending { it.second.second }
        .filter {
            searchReportQuery.trim().isEmpty() ||
            it.first.contains(searchReportQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showReportsDialog = false },
            title = {
                Text(
                    text = "تقرير المبيعات والعمليات بالتفصيل",
                    color = PalWhitePure,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "اختر دورة التقرير الزمني لعرض وتصدير حركات وفواتير المبيعات أو حدد تقويماً مخصصاً:",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Period Toggles
                    val periods = listOf("يومي", "أسبوعي", "شهري", "سنوي", "تقويم مخصص")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        periods.forEach { period ->
                            val isSel = selectedReportPeriod == period
                            val bg = if (isSel) PalGreenNormal else PalBlackLight
                            val fg = if (isSel) Color.White else Color.Gray
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable { selectedReportPeriod = period }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (period) {
                                        "يومي" -> "يومي"
                                        "أسبوعي" -> "أسبوعي"
                                        "شهري" -> "شهري"
                                        "سنوي" -> "سنوي"
                                        "تقويم مخصص" -> "تقويم مخصص"
                                        else -> period
                                    },
                                    color = fg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (selectedReportPeriod == "تقويم مخصص") {
                        UnifiedPeriodSelector(
                            initialType = customReportType,
                            initialValue = customReportValue,
                            onPeriodChanged = { type, finalVal ->
                                customReportType = type
                                customReportValue = finalVal
                                customReportPrintDate = finalVal
                            }
                        )
                        
                        OutlinedTextField(
                            value = customReportPrintDate,
                            onValueChange = { customReportPrintDate = it },
                            label = { Text("تعيين وتخصيص تاريخ التقرير المطبوع", color = PalWhiteSoft) },
                            placeholder = { Text("اكتب تاريخ أو فترة تخصيص التقرير...", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PalGreenLight,
                                unfocusedBorderColor = PalBlackLight,
                                focusedTextColor = PalWhitePure,
                                unfocusedTextColor = PalWhiteSoft,
                                focusedContainerColor = PalBlackDark,
                                unfocusedContainerColor = PalBlackDark
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Divider(color = PalBlackLight)

                    // Financial Summary Banner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PalBlackDark, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي المبيعات بالفترة:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text(Helpers.formatMoney(totalSalesAmount), color = PalGreenLight, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المتحصل نقدياً:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text(Helpers.formatMoney(totalPaidAmount), color = PalWhitePure, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المتبقي كملحق ديون:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text(Helpers.formatMoney(totalDebtAmount), color = PalRedLight, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }
                    }

                    Text(
                        text = "تفاصيل الأصناف المباعة بالفترة:",
                        color = PalWhiteSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = searchReportQuery,
                        onValueChange = { searchReportQuery = it },
                        placeholder = { Text("ابحث عن صنف في هذا التقرير...", color = PalWhiteMuted, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PalWhiteMuted, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft,
                            focusedContainerColor = PalBlackDark,
                            unfocusedContainerColor = PalBlackDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(PalBlackDark, RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (itemSalesRows.isEmpty()) {
                                Text(
                                    text = "لا توجد معاملات مبيعات مسجلة في هذه الفترة الزمنية.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                                )
                            } else {
                                itemSalesRows.forEach { (name, stats) ->
                                    val (qty, amount) = stats
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(PalBlackNormal, RoundedCornerShape(4.dp))
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1.5f)) {
                                            Text(name, color = PalWhitePure, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text("الكمية: ${qty.toInt()} حبة", color = PalWhiteMuted, fontSize = 9.sp)
                                        }
                                        Text(Helpers.formatMoney(amount), color = PalGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val reportMsg = buildString {
                        appendLine("📝 وكالة عاهد الصبري للمبيعات والقات الماوية")
                        appendLine("📊 التقرير المالي لحركة المبيعات بالتفصيل")
                        val pStr = if (selectedReportPeriod == "تقويم مخصص") customReportPrintDate else selectedReportPeriod
                        appendLine("📅 فترة التقرير: $pStr")
                        appendLine("⏰ تاريخ التصدير: ${Helpers.getCurrentDateTime()}")
                        appendLine("💵 العملة والتعامل المالي: $selectedCurrency")
                        appendLine("--------------------------------")
                        if (periodInvoices.isNotEmpty()) {
                            appendLine("سجل الفواتير وعمليات البيع بالتفصيل:")
                            periodInvoices.forEach { inv ->
                                val invItems = allInvoiceItems.filter { it.invoiceId == inv.id }
                                val itemsSummary = invItems.joinToString(", ") { "${it.itemName} (${it.quantity.toInt()} حبة)" }
                                appendLine("• سند #${inv.id} | التاريخ: ${inv.date}")
                                appendLine("  العميل: ${inv.customerName} | الأصناف: $itemsSummary")
                                appendLine("  طريقة الدفع: ${inv.paymentMethod} | الإجمالي: ${Helpers.formatMoney(inv.totalAmount)} ${inv.currency}")
                                appendLine("  ----------------")
                            }
                        }
                        if (itemSalesRows.isNotEmpty()) {
                            appendLine("📊 مجموع كميات الأصناف المباعة:")
                            itemSalesRows.forEach { (name, stats) ->
                                val (qty, amt) = stats
                                appendLine("• $name: ${qty.toInt()} حبة (بإجمالي: ${Helpers.formatMoney(amt)} $selectedCurrency)")
                            }
                            appendLine("--------------------------------")
                        }
                        appendLine("💰 إجمالي قيمة المبيعات: ${Helpers.formatMoney(totalSalesAmount)} $selectedCurrency")
                        appendLine("💸 إجمالي المسدد والنقدي: ${Helpers.formatMoney(totalPaidAmount)} $selectedCurrency")
                        appendLine("⚠️ إجمالي الديون الآجلة الجديدة: ${Helpers.formatMoney(totalDebtAmount)} $selectedCurrency")
                        appendLine("--------------------------------")
                        appendLine("صاحب الوكالة: عاهد الصبري")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val tPeriodStr = when (selectedReportPeriod) {
                                    "يومي" -> "اليومي"
                                    "أسبوعي" -> "الأسبوعي"
                                    "شهري" -> "الشهري"
                                    "سنوي" -> "السنوي"
                                    "تقويم مخصص" -> "الخاص بالفترة المحدد ($customReportPrintDate)"
                                    else -> "الدوري"
                                }
                                val headers = listOf("رقم السند", "التاريخ والوقت بالتفصيل", "اسم العميل", "البيان والأصناف المباعة", "طريقة الدفع", "المبلغ الكلي")
                                val rows = periodInvoices.map { inv ->
                                    val invItems = allInvoiceItems.filter { it.invoiceId == inv.id }
                                    val itemsSummary = invItems.joinToString("\n") { "${it.itemName} (${it.quantity.toInt()} حبة)" }
                                    listOf(
                                        "سند #${inv.id}",
                                        inv.date,
                                        inv.customerName,
                                        itemsSummary,
                                        inv.paymentMethod,
                                        Helpers.formatMoney(inv.totalAmount) + " " + inv.currency
                                    )
                                }
                                Helpers.generatePdfAndShare(
                                    context = context,
                                    title = "تقرير المبيعات $tPeriodStr وكالة عاهد الصبري",
                                    headers = headers,
                                    rows = rows,
                                    totals = mapOf(
                                        "إجمالي قيمة المبيعات" to Helpers.formatMoney(totalSalesAmount) + " " + selectedCurrency,
                                        "إجمالي المقبوضات النقدية" to Helpers.formatMoney(totalPaidAmount) + " " + selectedCurrency,
                                        "بقايا الديون الآجلة المقيدة" to Helpers.formatMoney(totalDebtAmount) + " " + selectedCurrency
                                    ),
                                    customDate = if (selectedReportPeriod == "تقويم مخصص") customReportPrintDate else Helpers.getCurrentDateTime(),
                                    reportPeriod = if (selectedReportPeriod == "تقويم مخصص") "$customReportType : $customReportValue" else selectedReportPeriod
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                Helpers.shareViaWhatsApp(context, "", reportMsg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("واتساب", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                bluetoothPrintText = reportMsg
                                showBluetoothPrintTrigger = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGoldCalligraphy),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("طباعة 🖨️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReportsDialog = false }) {
                    Text("إغلاق", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showBluetoothPrintTrigger) {
        BluetoothPrintDialog(
            receiptText = bluetoothPrintText,
            onDismiss = { showBluetoothPrintTrigger = false }
        )
    }
}
