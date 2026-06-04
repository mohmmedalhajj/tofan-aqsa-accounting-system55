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
import com.example.data.Customer
import com.example.data.CustomerPayment
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import com.example.util.BluetoothPrinterManager
import com.example.util.Helpers
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.example.ui.components.BluetoothPrintDialog
import com.example.ui.components.UnifiedPeriodSelector

data class AccountMovement(
    val date: String,
    val type: String, // "invoice" or "payment"
    val reference: String,
    val notes: String,
    val amount: Double,
    val paid: Double,
    val debt: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val printerManager = remember { BluetoothPrinterManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    val customersList by viewModel.allCustomers.collectAsState()
    val invoicesList by viewModel.allInvoices.collectAsState()
    val paymentsList by viewModel.allPayments.collectAsState()
    
    var showDebtReportsDialog by remember { mutableStateOf(false) }
    var selectedReportPeriod by remember { mutableStateOf("يومي") } // يومي, شهري, سنوي, تقويم مخصص
    var customerGeneralReportDate by remember { mutableStateOf(Helpers.getCurrentDateTime()) }
    var customDebtReportType by remember { mutableStateOf("يوم") }
    var customDebtReportValue by remember { mutableStateOf(Helpers.getCurrentDate()) }

    var showBluetoothPrintTrigger by remember { mutableStateOf(false) }
    var bluetoothPrintText by remember { mutableStateOf("") }
    
    var showAddCustDialog by remember { mutableStateOf(false) }
    var selectedCust by remember { mutableStateOf<Customer?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Form inputs for customer
    var custName by remember { mutableStateOf("") }
    var custPhone by remember { mutableStateOf("") }
    var custAddress by remember { mutableStateOf("") }
    var custNotes by remember { mutableStateOf("") }

    // Direct debt registration state
    var showDirectDebtDialog by remember { mutableStateOf(false) }
    var directDebtAmountInput by remember { mutableStateOf("") }
    var directDebtStatementInput by remember { mutableStateOf("") }
    var directDebtNotesInput by remember { mutableStateOf("") }
    var directDebtDateInput by remember { mutableStateOf(Helpers.getCurrentDateTime()) }
    var directDebtDueDateInput by remember { mutableStateOf("") }
    var directDebtOpNumber by remember { mutableStateOf("") }

    var searchText by remember { mutableStateOf("") }

    // Payment registration state
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentAmountInput by remember { mutableStateOf("") }
    var paymentNotesInput by remember { mutableStateOf("") }
    var paymentMethodSelected by remember { mutableStateOf("نقداً") }

    // Account Statement details state
    var showStatementDialog by remember { mutableStateOf(false) }
    var selectedCustForStatement by remember { mutableStateOf<Customer?>(null) }
    var paymentsHistoryList by remember { mutableStateOf<List<CustomerPayment>>(emptyList()) }
    var selectedStatementPeriod by remember { mutableStateOf("الكل") } // الكل, يومي, اسبوعي, شهري, سنوي, تقويم مخصص
    var customerReportDate by remember { mutableStateOf(Helpers.getCurrentDateTime()) }
    var customStatementReportType by remember { mutableStateOf("يوم") }
    var customStatementReportValue by remember { mutableStateOf(Helpers.getCurrentDate()) }

    LaunchedEffect(selectedCustForStatement) {
        selectedCustForStatement?.let { customer ->
            viewModel.getPaymentsForCustomer(customer.id).collect { list ->
                paymentsHistoryList = list
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة حسابات العملاء والديون الآجلة", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PalWhitePure)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        selectedCust = null
                        custName = ""
                        custPhone = ""
                        custAddress = ""
                        custNotes = ""
                        showAddCustDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة عميل", tint = PalGreenLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PalBlackNormal)
            )
        },
        containerColor = PalBlackDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Outstanding Stats
            val totalDebtsOutstanding = customersList.sumOf { it.totalDebt }
            Card(
                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                border = BorderStroke(1.dp, PalRedLight.copy(0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إجمالي أموال الآجل والديون الخارجية للعملاء", color = PalWhiteMuted, fontSize = 12.sp)
                        Text(Helpers.formatMoney(totalDebtsOutstanding), color = PalRedLight, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(PalRedLight.copy(0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PalRedLight, modifier = Modifier.size(26.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showDebtReportsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إدارة ومشاركة تقارير الديون والتحصيلات الآجلة 🗓️", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            val filteredCustomers = remember(customersList, searchText) {
                if (searchText.trim().isEmpty()) {
                    customersList
                } else {
                    customersList.filter {
                        it.name.contains(searchText, ignoreCase = true) ||
                        it.phone.contains(searchText)
                    }
                }
            }

            // Beautiful interactive Search Box
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("بحث عن عميل بالاسم أو رقم الهاتف...", color = PalWhiteMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = PalGreenLight) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = PalWhiteMuted)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PalGreenNormal,
                    unfocusedBorderColor = PalBlackLight
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Customer list
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchText.isNotEmpty()) "لا توجد نتائج تطابق البحث" else "لا يوجد عملاء مسجلين حاليا. انقر فوق زر الزائد لإضافة عميل",
                        color = PalWhiteMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCustomers) { customer ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                            border = BorderStroke(1.dp, PalBlackLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(customer.name, color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(
                                            if (customer.phone.isNotEmpty()) "هاتف: ${customer.phone}" else "لا يوجد رقم هاتف",
                                            color = PalWhiteMuted,
                                            fontSize = 12.sp
                                        )
                                        if (customer.address.isNotEmpty()) {
                                            Text(
                                                "العنوان: ${customer.address}",
                                                color = Color(0xFFAAAAAA),
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        if (customer.notes.isNotEmpty()) {
                                            Text(
                                                "ملاحظات: ${customer.notes}",
                                                color = Color(0xFF888888),
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("رصيد الدين الحالي", color = PalWhiteMuted, fontSize = 11.sp)
                                        Text(
                                            Helpers.formatMoney(customer.totalDebt),
                                            color = if (customer.totalDebt > 0) PalRedLight else PalGreenLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Row 1: Direct payments & Direct Debt
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            selectedCust = customer
                                            paymentAmountInput = ""
                                            paymentNotesInput = ""
                                            showPaymentDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تسديد دفعة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            selectedCust = customer
                                            directDebtAmountInput = ""
                                            directDebtStatementInput = ""
                                            directDebtNotesInput = ""
                                            directDebtDateInput = Helpers.getCurrentDateTime()
                                            directDebtOpNumber = "INV-${(1000..9999).random()}"
                                            directDebtDueDateInput = ""
                                            showDirectDebtDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تسجيل دين جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Row 2: Statement and info editing
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            selectedCustForStatement = customer
                                            showStatementDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PalBlackLight),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1.5f)
                                    ) {
                                        Icon(Icons.Default.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("كشف الحساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(onClick = {
                                        selectedCust = customer
                                        custName = customer.name
                                        custPhone = customer.phone
                                        custAddress = customer.address
                                        custNotes = customer.notes
                                        showAddCustDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = PalWhiteMuted)
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(onClick = {
                                        val message = """
السلام عليكم ورحمة الله وبركاته، الأخ الكريم العميل المحترم: ${customer.name}
تحية طيبة محملة بالتقدير والاحترام من وكالة عاهد الصبري لأجواد انواع القات الماوية والورزاني بجميع أنواعها.

نود تذكيركم بلطف وعناية بأن رصيد مديونيتكم المتبقي لصالح الوكالة هو: ${Helpers.formatMoney(customer.totalDebt)}.
نأمل منكم التكرم بسداد هذا المبلغ في أقرب فرصة مناسبة لكم، وذلك لمساعدتنا في تنظيم الحسابات التشغيلية واليومية ومواصلة تقديم أفضل الخدمات لكم.

شاكرين لكم عظيم تفهمكم وحسن تعاونكم الدائم والمعهود منا ومنكم.
أخوكم وصاحب الوكالة / عاهد الصبري
                                        """.trimIndent()
                                        Helpers.shareText(context, message)
                                    }) {
                                        Icon(Icons.Default.Send, contentDescription = "إرسال تذكير بالدين عبر الواتساب", tint = PalGoldCalligraphy)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Add / Edit Customer Dialog ---
    if (showAddCustDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustDialog = false },
            title = {
                Text(
                    text = if (selectedCust == null) "إضافة عميل جديد" else "تعديل بيانات العميل",
                    color = PalWhitePure,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = custName,
                        onValueChange = { custName = it },
                        label = { Text("اسم العميل كامل", color = PalWhiteMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = custPhone,
                        onValueChange = { custPhone = it },
                        label = { Text("رقم هاتفه (للمشاركة التلقائية)", color = PalWhiteMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = custAddress,
                        onValueChange = { custAddress = it },
                        label = { Text("العنوان السكني / التجاري", color = PalWhiteMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = custNotes,
                        onValueChange = { custNotes = it },
                        label = { Text("أي ملاحظات أخرى", color = PalWhiteMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedCust != null) {
                        Button(
                            onClick = {
                                showDeleteConfirm = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal)
                        ) {
                            Text("حذف العميل", color = PalWhitePure)
                        }
                    }
                    Button(
                        onClick = {
                            if (custName.trim().isEmpty()) {
                                android.widget.Toast.makeText(context, "الرجاء إدخال اسم العميل!", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedCust == null) {
                                viewModel.createCustomer(custName, custPhone, custAddress, custNotes)
                            } else {
                                viewModel.updateCustomer(selectedCust!!.copy(name = custName, phone = custPhone, address = custAddress, notes = custNotes))
                            }
                            showAddCustDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                    ) {
                        Text("حفظ", color = PalWhitePure)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustDialog = false }) {
                    Text("إلغاء", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    // --- Payment Registration Dialog ---
    if (showPaymentDialog && selectedCust != null) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("تسجيل سداد دفعة مالية", color = PalWhitePure, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("سداد ديون للعميل: ${selectedCust!!.name}", color = PalWhitePure, fontWeight = FontWeight.SemiBold)
                    Text("الدين الكلي الحالي: ${Helpers.formatMoney(selectedCust!!.totalDebt)}", color = PalRedLight, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = { Text("المبلغ المدفوع المسدد (ريال)", color = PalWhiteMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                                value = paymentNotesInput,
                                onValueChange = { paymentNotesInput = it },
                                label = { Text("ملاحظات السداد", color = PalWhiteMuted) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("طريقة الدفع المسددة:", color = PalWhitePure, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            
                            val paymentMethodsList = listOf("نقداً", "آجل", "إيداع", "تحويل")
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                paymentMethodsList.forEach { method ->
                                     val isSel = paymentMethodSelected == method
                                     val bg = if (isSel) PalGreenNormal else PalBlackLight
                                     val fg = if (isSel) Color.White else Color.Gray
                                     Box(
                                         modifier = Modifier
                                             .clip(RoundedCornerShape(6.dp))
                                             .background(bg)
                                             .clickable { paymentMethodSelected = method }
                                             .padding(horizontal = 12.dp, vertical = 6.dp)
                                     ) {
                                         Text(text = method, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                     }
                                 }
                             }
                         }
                     },
                     confirmButton = {
                         Button(
                             onClick = {
                                 val amount = paymentAmountInput.toDoubleOrNull() ?: 0.0
                                 if (amount <= 0.0) {
                                     android.widget.Toast.makeText(context, "يرجى تحديد رصيد سداد صحيح أكبر من الصفر!", android.widget.Toast.LENGTH_SHORT).show()
                                     return@Button
                                 }
                                 viewModel.addCustomerPayment(
                                     customerId = selectedCust!!.id,
                                     amount = amount,
                                     notes = paymentNotesInput,
                                     paymentMethod = paymentMethodSelected
                                 )
                         android.widget.Toast.makeText(context, "تم قيد سداد الدفعة وحفظ السجل المالي", android.widget.Toast.LENGTH_SHORT).show()
                         showPaymentDialog = false
                     },
                     colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                 ) {
                     Text("تاكيد التسديد", color = PalWhitePure)
                 }
             },
             dismissButton = {
                 TextButton(onClick = { showPaymentDialog = false }) {
                     Text("تراجع", color = PalWhiteMuted)
                 }
             },
             containerColor = PalBlackNormal
         )
     }

     // --- Direct Customer Debt Registration Dialog ---
     if (showDirectDebtDialog && selectedCust != null) {
         AlertDialog(
             onDismissRequest = { showDirectDebtDialog = false },
             title = {
                 Text(
                     text = "تسجيل حركة دين مباشر جديد",
                     color = PalWhitePure,
                     modifier = Modifier.fillMaxWidth(),
                     textAlign = TextAlign.Right,
                     style = MaterialTheme.typography.titleLarge
                 )
             },
             text = {
                 Column(
                     modifier = Modifier
                         .fillMaxWidth()
                         .verticalScroll(rememberScrollState()),
                     verticalArrangement = Arrangement.spacedBy(10.dp)
                 ) {
                     Text("إضافة دين مباشر لحساب العميل: ${selectedCust!!.name}", color = PalWhitePure, fontWeight = FontWeight.SemiBold)
                     Text("الدين الحالي المسجل: ${Helpers.formatMoney(selectedCust!!.totalDebt)}", color = PalRedLight, fontSize = 13.sp)

                     Spacer(modifier = Modifier.height(6.dp))

                     OutlinedTextField(
                         value = directDebtOpNumber,
                         onValueChange = { directDebtOpNumber = it },
                         label = { Text("رقم مرجع العملية / الفاتورة", color = PalWhiteMuted) },
                         colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                         singleLine = true,
                         modifier = Modifier.fillMaxWidth()
                     )

                     OutlinedTextField(
                         value = directDebtAmountInput,
                         onValueChange = { directDebtAmountInput = it },
                         label = { Text("قيمة الدين (ريال) *", color = PalWhiteMuted) },
                         keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                         colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                         singleLine = true,
                         modifier = Modifier.fillMaxWidth()
                     )

                     OutlinedTextField(
                         value = directDebtStatementInput,
                         onValueChange = { directDebtStatementInput = it },
                         label = { Text("بيان الحركة المباشرة دائن/مدين *", color = PalWhiteMuted) },
                         colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                         singleLine = true,
                         modifier = Modifier.fillMaxWidth()
                     )

                     OutlinedTextField(
                         value = directDebtNotesInput,
                         onValueChange = { directDebtNotesInput = it },
                         label = { Text("ملاحظات وتفاصيل إضافية", color = PalWhiteMuted) },
                         colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                         singleLine = false,
                         modifier = Modifier.fillMaxWidth()
                     )

                     OutlinedTextField(
                         value = directDebtDateInput,
                         onValueChange = { directDebtDateInput = it },
                         label = { Text("تاريخ ووقت الحركة", color = PalWhiteMuted) },
                         colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                         singleLine = true,
                         modifier = Modifier.fillMaxWidth()
                     )

                     OutlinedTextField(
                         value = directDebtDueDateInput,
                         onValueChange = { directDebtDueDateInput = it },
                         label = { Text("تاريخ الاستحقاق (اختياري) - مثلاً: 2026-06-15", color = PalWhiteMuted) },
                         colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                         singleLine = true,
                         modifier = Modifier.fillMaxWidth()
                     )
                 }
             },
             confirmButton = {
                 Button(
                     onClick = {
                         val amount = directDebtAmountInput.toDoubleOrNull() ?: 0.0
                         if (amount <= 0.0) {
                             android.widget.Toast.makeText(context, "الرجاء كتابة قيمة دين صحيحة أكبر من الصفر!", android.widget.Toast.LENGTH_SHORT).show()
                             return@Button
                         }
                         if (directDebtStatementInput.trim().isEmpty()) {
                             android.widget.Toast.makeText(context, "الرجاء إدخال بيان لحركة الدين لشرح المعاملة!", android.widget.Toast.LENGTH_SHORT).show()
                             return@Button
                         }
                         
                         val statementText = directDebtStatementInput.trim()
                         val fullNotes = buildString {
                             append("رقم قيد: ")
                             append(directDebtOpNumber)
                             if (directDebtDueDateInput.trim().isNotEmpty()) {
                                 append(" • استحقاق: ")
                                 append(directDebtDueDateInput.trim())
                             }
                             if (directDebtNotesInput.trim().isNotEmpty()) {
                                 append(" • ملاحظات: ")
                                 append(directDebtNotesInput.trim())
                             }
                         }

                         viewModel.recordDirectDebt(
                             customerName = selectedCust!!.name,
                             amount = amount,
                             statement = statementText,
                             date = directDebtDateInput.trim().ifEmpty { Helpers.getCurrentDateTime() },
                             notes = fullNotes
                         )
                         
                         android.widget.Toast.makeText(context, "تم تسجيل وإثبات حركة الدين وإبلاغ كشف الحساب بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                         showDirectDebtDialog = false
                     },
                     colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal)
                 ) {
                     Text("حفظ الدين", color = PalWhitePure)
                 }
             },
             dismissButton = {
                 TextButton(onClick = { showDirectDebtDialog = false }) {
                     Text("إلغاء", color = PalWhiteMuted)
                 }
             },
             containerColor = PalBlackNormal
         )
     }

    // --- Customer Account Statement Dialog ---
    if (showStatementDialog && selectedCustForStatement != null) {
        val todayStr = Helpers.getCurrentDate()
        val curMonthStr = todayStr.substring(0, 7)
        val curYearStr = todayStr.substring(0, 4)

        // Filter invoices & payments based on selectedStatementPeriod using robust trimmed case-insensitive matching
        val filteredCustInvs = invoicesList.filter { 
            it.customerName.trim().equals(selectedCustForStatement!!.name.trim(), ignoreCase = true) 
        }.filter { inv ->
            when (selectedStatementPeriod) {
                "يومي" -> inv.date.startsWith(todayStr)
                "اسبوعي" -> Helpers.isWithinLast7Days(inv.date)
                "شهري" -> inv.date.startsWith(curMonthStr)
                "سنوي" -> inv.date.startsWith(curYearStr)
                "تقويم مخصص" -> inv.date.startsWith(customStatementReportValue)
                else -> true
            }
        }

        val filteredCustPays = paymentsHistoryList.filter { pay ->
            when (selectedStatementPeriod) {
                "يومي" -> pay.date.startsWith(todayStr)
                "اسبوعي" -> Helpers.isWithinLast7Days(pay.date)
                "شهري" -> pay.date.startsWith(curMonthStr)
                "سنوي" -> pay.date.startsWith(curYearStr)
                "تقويم مخصص" -> pay.date.startsWith(customStatementReportValue)
                else -> true
            }
        }

        val totalInvoicedLocal = filteredCustInvs.sumOf { it.totalAmount }
        val totalPaidLocal = filteredCustPays.sumOf { it.amount } + filteredCustInvs.sumOf { it.paidAmount }

        // Create a beautifully unified chronological list of account movements
        val movements = remember(filteredCustInvs, filteredCustPays) {
            val list = mutableListOf<AccountMovement>()
            filteredCustInvs.forEach { inv ->
                list.add(
                    AccountMovement(
                        date = inv.date,
                        type = "invoice",
                        reference = "مبيعات بموجب فاتورة #${inv.id}",
                        notes = "الدفع: ${inv.paymentMethod} (متبقي آجل: ${Helpers.formatMoney(inv.debtAmount)})",
                        amount = inv.totalAmount,
                        paid = inv.paidAmount,
                        debt = inv.debtAmount
                    )
                )
            }
            filteredCustPays.forEach { pay ->
                list.add(
                    AccountMovement(
                        date = pay.date,
                        type = "payment",
                        reference = "سند قبض تسديد دفعة #${pay.id}",
                        notes = pay.notes.ifEmpty { "سداد مباشر من العميل" } + " (${pay.paymentMethod})",
                        amount = pay.amount,
                        paid = pay.amount,
                        debt = 0.0
                    )
                )
            }
            list.sortedByDescending { it.date }
        }

        val msg = buildString {
            appendLine("وكالة عاهد الصبري")
            appendLine("كشف الحساب للعميل: ${selectedCustForStatement!!.name}")
            appendLine("رقم هاتف العميل: ${selectedCustForStatement!!.phone.ifEmpty { "غير محدد" }}")
            appendLine("الفترة المحددة: $selectedStatementPeriod")
            appendLine("تاريخ التقرير: $customerReportDate")
            appendLine("-------------------------")
            appendLine("💵 رصيد المديونية الكلي المتبقي حالياً:")
            appendLine("👈 ${Helpers.formatMoney(selectedCustForStatement!!.totalDebt)}")
            appendLine("-------------------------")
            if (movements.isNotEmpty()) {
                appendLine("📈 حركات الحساب التفصيلية بالفترة المعينة:")
                movements.forEach { mov ->
                    if (mov.type == "invoice") {
                        appendLine("• ${mov.date.split(" ")[0]} - فاتورة بقيمة ${Helpers.formatMoney(mov.amount)} (آجل: +${Helpers.formatMoney(mov.debt)})")
                    } else {
                        appendLine("• ${mov.date.split(" ")[0]} - تسديد بقيمة -${Helpers.formatMoney(mov.amount)} (${mov.notes})")
                    }
                }
                appendLine("-------------------------")
            }
            appendLine("إجمالي قيمة فواتير الفترة: ${Helpers.formatMoney(totalInvoicedLocal)}")
            appendLine("إجمالي مسدِدات الفترة: ${Helpers.formatMoney(totalPaidLocal)}")
            appendLine("-------------------------")
            appendLine("نرجو وسرعة سداد المبالغ وتصفية الحساب. شاكرين تعاونكم.")
            appendLine("صاحب الوكالة: عاهد الصبري")
        }

        AlertDialog(
            onDismissRequest = { showStatementDialog = false },
            title = { Text("كشف حساب وتفاصيل العميل", color = PalWhitePure, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("العميل: ${selectedCustForStatement!!.name}", color = PalWhitePure, fontWeight = FontWeight.Bold)
                    Text("الهاتف: ${selectedCustForStatement!!.phone.ifEmpty { "غير متوفر" }}", color = PalWhiteMuted, fontSize = 12.sp)
                    
                    Divider(color = PalBlackLight)

                    Text("رصيد الدين الإجمالي المتبقي: ${Helpers.formatMoney(selectedCustForStatement!!.totalDebt)}", color = PalRedLight, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("اختر فترة فلترة وتصفية العمليات أو حدد تقويماً مخصصاً:", color = PalWhiteSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        listOf("يومي", "اسبوعي", "شهري", "سنوي", "تقويم مخصص", "الكل").forEach { period ->
                            val selected = selectedStatementPeriod == period
                            val bg = if (selected) PalGreenNormal else PalBlackLight
                            val fg = if (selected) Color.White else Color.Gray
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bg)
                                    .clickable { selectedStatementPeriod = period }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(period, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (selectedStatementPeriod == "تقويم مخصص") {
                        UnifiedPeriodSelector(
                            initialType = customStatementReportType,
                            initialValue = customStatementReportValue,
                            onPeriodChanged = { type, finalVal ->
                                customStatementReportType = type
                                customStatementReportValue = finalVal
                                // Also update custom printing date dynamically
                                customerReportDate = "$finalVal"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customerReportDate,
                        onValueChange = { customerReportDate = it },
                        label = { Text("تعيين وتخصيص تاريخ كشف الحساب المطبوع", color = PalWhiteMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("customer_statement_date_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PalGreenLight) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft,
                            focusedContainerColor = PalBlackDark,
                            unfocusedContainerColor = PalBlackDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Divider(color = PalBlackLight.copy(0.4f))

                    Text("سجل حركات الحساب التفصيلية بالفترة المختارة:", color = PalWhiteMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    
                    if (movements.isEmpty()) {
                        Text("لا توجد حركات حساب مسجلة بالفترة المعينة", color = PalWhiteMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center)
                    } else {
                        movements.forEach { movement ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PalBlackDark, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val icon = if (movement.type == "invoice") Icons.Default.TrendingUp else Icons.Default.TrendingDown
                                    val iconTint = if (movement.type == "invoice") PalRedLight else PalGreenLight
                                    val bgTint = if (movement.type == "invoice") PalRedLight.copy(0.1f) else PalGreenLight.copy(0.1f)
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(bgTint, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                                    }
                                    
                                    Column {
                                        Text(movement.reference, color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(movement.notes, color = PalWhiteMuted, fontSize = 10.sp)
                                    }
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    val textValue = if (movement.type == "invoice") {
                                        "+${Helpers.formatMoney(movement.debt)}"
                                    } else {
                                        "-${Helpers.formatMoney(movement.amount)}"
                                    }
                                    val textColor = if (movement.type == "invoice") PalRedLight else PalGreenLight
                                    Text(textValue, color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(movement.date.split(" ")[0], color = PalWhiteMuted, fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                val headers = listOf("التاريخ", "الحركة / البيان المالي", "قيمة الدين المضاف", "قيمة السداد المخصوم")
                                val rows = mutableListOf<List<String>>()
                                movements.forEach { mov ->
                                    rows.add(
                                        listOf(
                                            mov.date.split(" ")[0],
                                            mov.reference + "\n(" + mov.notes + ")",
                                            if (mov.type == "invoice") Helpers.formatMoney(mov.debt) else "0 ريال",
                                            if (mov.type == "payment") Helpers.formatMoney(mov.amount) else "0 ريال"
                                        )
                                    )
                                }
                                Helpers.generatePdfAndShare(
                                    context = context,
                                    title = "كشف حساب عميل ($selectedStatementPeriod): ${selectedCustForStatement!!.name}",
                                    headers = headers,
                                    rows = rows,
                                    totals = mapOf(
                                        "إجمالي مديونية العميل الكلية المتبقية" to Helpers.formatMoney(selectedCustForStatement!!.totalDebt)
                                    ),
                                    customDate = customerReportDate
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            Text("PDF", color = PalWhitePure, fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                Helpers.shareViaWhatsApp(context, selectedCustForStatement!!.phone, msg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            Text("واتساب", color = PalWhitePure, fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                Helpers.shareViaSMS(context, selectedCustForStatement!!.phone, msg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            Text("SMS", color = PalWhitePure, fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                bluetoothPrintText = msg
                                showBluetoothPrintTrigger = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGoldCalligraphy),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            Text("طباعة", color = PalWhitePure, fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatementDialog = false }) {
                    Text("إغلاق", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showDeleteConfirm && selectedCust != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "تأكيد حذف العميل",
                    color = PalWhitePure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف العميل (${selectedCust!!.name}) نهائياً؟ سيتم مسح حساب العميل وأي سجلات مرتبطة به تماماً.",
                    color = PalWhiteSoft,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(selectedCust!!)
                        showDeleteConfirm = false
                        showAddCustDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تأكيد الحذف", color = PalWhitePure, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text("إلغاء", color = PalWhiteMuted, fontWeight = FontWeight.Normal)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showDebtReportsDialog) {
        val currentDateStr = Helpers.getCurrentDate()
        val prefix = when (selectedReportPeriod) {
            "يومي" -> currentDateStr
            "شهري" -> currentDateStr.substring(0, 7)
            "سنوي" -> currentDateStr.substring(0, 4)
            "تقويم مخصص" -> customDebtReportValue
            else -> currentDateStr
        }

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

        val periodInvoices = if (selectedReportPeriod == "أسبوعي") {
            invoicesList.filter { isWeekly(it.date) }
        } else {
            invoicesList.filter { it.date.startsWith(prefix) }
        }
        val periodNewDebt = periodInvoices.sumOf { it.debtAmount }

        val periodPayments = if (selectedReportPeriod == "أسبوعي") {
            paymentsList.filter { isWeekly(it.date) }
        } else {
            paymentsList.filter { it.date.startsWith(prefix) }
        }
        val periodPaymentsCollected = periodPayments.sumOf { it.amount }
        val totalDebtsOutstanding = customersList.sumOf { it.totalDebt }

        AlertDialog(
            onDismissRequest = { showDebtReportsDialog = false },
            title = {
                Text(
                    text = "تقارير حسابات الديون والتحصيلات",
                    color = PalWhitePure,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "اختر دورة أو فترة التقرير لتجهيز إحصائيات الديون وحركات الدفع أو حدد تقويماً مخصصاً:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right
                    )

                    // Period Selection Toggles
                    val periods = listOf("يومي", "أسبوعي", "شهري", "سنوي", "تقويم مخصص")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                            initialType = customDebtReportType,
                            initialValue = customDebtReportValue,
                            onPeriodChanged = { type, finalVal ->
                                customDebtReportType = type
                                customDebtReportValue = finalVal
                                // Also update custom printing date dynamically
                                customerGeneralReportDate = "$finalVal"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customerGeneralReportDate,
                        onValueChange = { customerGeneralReportDate = it },
                        label = { Text("تعيين وتخصيص تاريخ التقرير المطبوع", color = PalWhiteMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("customer_general_report_date_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PalGreenLight) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft,
                            focusedContainerColor = PalBlackDark,
                            unfocusedContainerColor = PalBlackDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Divider(color = PalBlackLight)

                    // Display Calculations
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PalBlackDark, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الديون الجديدة المقيدة:", color = PalWhiteSoft, fontSize = 12.sp)
                            Text(Helpers.formatMoney(periodNewDebt), color = PalRedLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المتحصلات والمسدد ونقداً:", color = PalWhiteSoft, fontSize = 12.sp)
                            Text(Helpers.formatMoney(periodPaymentsCollected), color = PalGreenLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Divider(color = PalBlackLight.copy(0.4f))
                        val netChange = periodNewDebt - periodPaymentsCollected
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("صافي التغير بالمديونية:", color = PalWhitePure, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            val colorChange = if (netChange >= 0) PalRedLight else PalGreenLight
                            val sign = if (netChange >= 0) "+" else ""
                            Text("$sign${Helpers.formatMoney(netChange)}", color = colorChange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "كشف مفصل بالعملاء والتواريخ ($selectedReportPeriod):",
                        color = PalWhiteSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PalBlackDark, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (periodInvoices.isEmpty() && periodPayments.isEmpty()) {
                            Text(
                                text = "لا توجد حركات ديون أو سداد مالي مسجلة في هذه الفترة.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            if (periodInvoices.isNotEmpty()) {
                                Text("📥 فواتير ديون جديدة:", color = PalRedLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                periodInvoices.forEach { inv ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${inv.customerName}: +${Helpers.formatMoney(inv.debtAmount)}", color = PalWhitePure, fontSize = 10.sp)
                                        Text(inv.date.split(" ")[0], color = PalWhiteMuted, fontSize = 9.sp)
                                    }
                                }
                            }
                            if (periodPayments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("💸 دفعات وتحصيلات المستلمين:", color = PalGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                periodPayments.forEach { pay ->
                                    val custName = customersList.find { it.id == pay.customerId }?.name ?: "عميل"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("$custName: -${Helpers.formatMoney(pay.amount)}", color = PalWhitePure, fontSize = 10.sp)
                                        Text(pay.date.split(" ")[0], color = PalWhiteMuted, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val reportMsg = buildString {
                        appendLine("وكالة عاهد الصبري")
                        appendLine("تقرير حسابات الديون والتحصيلات التفصيلي")
                        appendLine("فترة التقرير: $selectedReportPeriod")
                        appendLine("التاريخ والوقت: $customerGeneralReportDate")
                        appendLine("-------------------------")
                        if (periodInvoices.isNotEmpty()) {
                            appendLine("فواتير الديون الجديدة بالتفصيل للعملاء:")
                            periodInvoices.forEachIndexed { i, it ->
                                appendLine("${i+1}) العميل: ${it.customerName} | تاريخ: ${it.date} | دين جديد بقيمة: ${Helpers.formatMoney(it.debtAmount)}")
                            }
                            appendLine("-------------------------")
                        }
                        if (periodPayments.isNotEmpty()) {
                            appendLine("سجل التحصيلات والقبض نقداً:")
                            periodPayments.forEachIndexed { i, p ->
                                val name = customersList.find { it.id == p.customerId }?.name ?: "عميل"
                                appendLine("${i+1}) العميل: $name | تاريخ: ${p.date} | سدد نقداً: ${Helpers.formatMoney(p.amount)} | البيان: ${p.notes.ifEmpty { "مسدد الديون" }}")
                            }
                            appendLine("-------------------------")
                        }
                        appendLine("👈 إجمالي الديون الجديدة: ${Helpers.formatMoney(periodNewDebt)}")
                        appendLine("👈 إجمالي التحصيلات: ${Helpers.formatMoney(periodPaymentsCollected)}")
                        val change = periodNewDebt - periodPaymentsCollected
                        appendLine("👈 صافي التغير: ${if (change >= 0) "زيادة" else "تراجع"} قدره ${Helpers.formatMoney(kotlin.math.abs(change))}")
                        appendLine("-------------------------")
                        appendLine("إجمالي الذمم والديون الخارجية المعلقة حالياً:")
                        appendLine("💰 ${Helpers.formatMoney(totalDebtsOutstanding)}")
                        appendLine("صاحب الوكالة: عاهد الصبري")
                    }

                    Button(
                        onClick = {
                            val debtors = customersList.filter { it.totalDebt > 0 }
                            if (debtors.isEmpty()) {
                                android.widget.Toast.makeText(context, "لا يوجد أي عملاء مديونين حالياً لتذكيرهم!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                debtors.forEach { debtor ->
                                    val reminderMessage = """
السلام عليكم ورحمة الله وبركاته، الأخ العزيز والعميل الأكرم.
تحية طيبة محملة بالتقدير والاحترام مبعوثة لكم من وكالة عاهد الصبري.

نود تذكيركم بلطف وعناية بمراجعة حسابكم الجاري لدينا، والمبادرة الطيبة بسداد المديونية المتبقية بطرفكم بأقرب وقت متاح لكم، وذلك لمساعدتنا في تسيير المعاملات اليومية والحسابات بانتظام.

نشكركم جزيل الشكر والتقدير ولكم منا كل الامتنان لحسن تفهمكم وتعاونكم الدائم معنا.
أخوكم وصاحب الوكالة / عاهد الصبري
                                    """.trimIndent()
                                    Helpers.shareViaSMS(context, debtor.phone, reminderMessage)
                                }
                                android.widget.Toast.makeText(context, "تم تجهيز رسائل تذكير لـ ${debtors.size} عملاء مديونين", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("إرسال رسائل تذكير جماعية SMS للمديونين ✉️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val tPrefix = when (selectedReportPeriod) {
                                    "يومي" -> "تقرير يومي"
                                    "أسبوعي" -> "تقرير أسبوعي"
                                    "شهري" -> "تقرير شهري"
                                    "سنوي" -> "تقرير سنوي"
                                    else -> "تقرير دوري"
                                }
                                val headers = listOf("التاريخ والوقت", "نوع العملية والحركة", "اسم العميل", "المبلغ المقيد")
                                val rows = mutableListOf<List<String>>()

                                periodInvoices.forEach { inv ->
                                    rows.add(listOf(
                                        inv.date,
                                        "دين جديد (آجل)",
                                        inv.customerName,
                                        "+${Helpers.formatMoney(inv.debtAmount)}"
                                    ))
                                }
                                periodPayments.forEach { pay ->
                                    val name = customersList.find { it.id == pay.customerId }?.name ?: "عميل"
                                    rows.add(listOf(
                                        pay.date,
                                        "تحصيل وسداد نقدي",
                                        name,
                                        "-${Helpers.formatMoney(pay.amount)}"
                                    ))
                                }
                                if (rows.isEmpty()) {
                                    rows.add(listOf("-", "لا توجد معاملات مالية", "-", "0"))
                                }

                                Helpers.generatePdfAndShare(
                                    context = context,
                                    title = "$tPrefix ديون وتحصيلات وكالة عاهد الصبري",
                                    headers = headers,
                                    rows = rows,
                                    totals = mapOf(
                                        "إجمالي قيمة الديون الجديدة" to Helpers.formatMoney(periodNewDebt),
                                        "إجمالي قيمة المبالغ المقبوضة" to Helpers.formatMoney(periodPaymentsCollected),
                                        "إجمالي الديون المعلقة بطرفهم" to Helpers.formatMoney(totalDebtsOutstanding)
                                    ),
                                    customDate = customerGeneralReportDate
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                        ) {
                            Text("PDF 📄", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                Helpers.shareViaWhatsApp(context, "", reportMsg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                        ) {
                            Text("واتساب 💬", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                bluetoothPrintText = reportMsg
                                showBluetoothPrintTrigger = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGoldCalligraphy),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                        ) {
                            Text("طباعة 🖨️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDebtReportsDialog = false }) {
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
