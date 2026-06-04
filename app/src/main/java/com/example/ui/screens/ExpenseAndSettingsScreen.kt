package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import com.example.util.BluetoothPrinterManager
import com.example.util.Helpers
import androidx.compose.ui.platform.testTag
import java.util.*

import com.example.ui.components.UnifiedPeriodSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val expensesList by viewModel.allExpenses.collectAsState()

    var showExpenseDialog by remember { mutableStateOf(false) }
    var inputAmount by remember { mutableStateOf("") }
    var inputNotes by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var expenseReportDate by remember { mutableStateOf(Helpers.getCurrentDateTime()) }
    
    val categories = listOf("نقل ومواصلات", "إيجار المحل والجمارك", "تعبئة وتغليف وعمالة", "خدمات وتبرعات وطاقة", "مصاريف عامة أخرى")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf("الكل") } // يومي, شهري, سنوي, تقويم مخصص, الكل
    var customExpenseReportType by remember { mutableStateOf("يوم") }
    var customExpenseReportValue by remember { mutableStateOf(Helpers.getCurrentDate()) }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }

    var showBluetoothPrintTrigger by remember { mutableStateOf(false) }
    var bluetoothPrintText by remember { mutableStateOf("") }

    val todayStr = Helpers.getCurrentDate()
    val curMonthStr = todayStr.substring(0, 7)
    val curYearStr = todayStr.substring(0, 4)

    val filteredList = expensesList.filter { exp ->
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val todayTime = try { sdf.parse(todayStr)?.time ?: 0L } catch(e: Exception) { 0L }
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
        val matchesPeriod = when (selectedPeriod) {
            "يومي" -> exp.date.startsWith(todayStr)
            "أسبوعي" -> isWeekly(exp.date)
            "شهري" -> exp.date.startsWith(curMonthStr)
            "سنوي" -> exp.date.startsWith(curYearStr)
            "تقويم مخصص" -> {
                val expDate = exp.date.take(10)
                when (customExpenseReportType) {
                    "يوم" -> expDate == customExpenseReportValue
                    "شهر" -> expDate.startsWith(customExpenseReportValue)
                    "سنة" -> expDate.startsWith(customExpenseReportValue)
                    else -> true
                }
            }
            else -> true
        }
        val matchesCategory = selectedCategoryFilter == "الكل" || exp.category == selectedCategoryFilter
        val matchesSearch = searchQuery.trim().isEmpty() || 
                exp.notes.contains(searchQuery, ignoreCase = true) || 
                exp.category.contains(searchQuery, ignoreCase = true)
        
        matchesPeriod && matchesCategory && matchesSearch
    }

    val totalExpenseSum = filteredList.sumOf { it.amount }

    val printExpensesMsg = buildString {
        appendLine("وكالة عاهد الصبري")
        appendLine("كشف المصروفات والمنصرفات التشغيلية")
        appendLine("فترة البحث والتصفية: $selectedPeriod")
        appendLine("الفئة المحددة: $selectedCategoryFilter")
        appendLine("تاريخ الجرد: $expenseReportDate")
        appendLine("--------------------------------")
        if (filteredList.isEmpty()) {
            appendLine("لا توجد بنود مصروفات مسجلة مسبقاً لهذه الفئة.")
        } else {
            filteredList.forEach { exp ->
                appendLine("• الفئة: ${exp.category}")
                appendLine("  البيان: ${exp.notes.ifEmpty { "بدون شرح" }}")
                appendLine("  المبلغ: ${Helpers.formatMoney(exp.amount)} | التاريخ: ${exp.date.split(" ")[0]}")
                appendLine("- - - - - - - - - - - - - - - -")
            }
        }
        appendLine("--------------------------------")
        appendLine("إجمالي المصروفات المصدرة:")
        appendLine("💰 ${Helpers.formatMoney(totalExpenseSum)}")
        appendLine("صاحب الوكالة: عاهد الصبري")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("قسم المصروفات الإدارية والعمومية", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PalWhitePure)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        inputAmount = ""
                        inputNotes = ""
                        showExpenseDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة مصروف", tint = PalGreenLight)
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats Card
            Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إجمالي المصروفات المنصرفة بالفترة والبحث", color = PalWhiteMuted, fontSize = 11.sp)
                        Text(Helpers.formatMoney(totalExpenseSum), color = PalRedLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(PalRedLight.copy(0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = PalRedLight, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Search and Filters Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("البحث في المصروفات والمنصرفات...", color = PalWhiteMuted, fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PalWhiteMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Period Selection Tabs
            val periodsToggle = listOf("يومي", "أسبوعي", "شهري", "سنوي", "تقويم مخصص", "الكل")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                periodsToggle.forEach { period ->
                    val isSel = selectedPeriod == period
                    val bg = if (isSel) PalGreenNormal else PalBlackNormal
                    val fg = if (isSel) Color.White else Color.Gray
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable { selectedPeriod = period }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(period, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (selectedPeriod == "تقويم مخصص") {
                UnifiedPeriodSelector(
                    initialType = customExpenseReportType,
                    initialValue = customExpenseReportValue,
                    onPeriodChanged = { type, finalVal ->
                        customExpenseReportType = type
                        customExpenseReportValue = finalVal
                    }
                )
            }

            // Category Filter Scrollable Bar
            val fullCategoryFilters = listOf("الكل") + categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                fullCategoryFilters.forEach { cat ->
                    val isSel = selectedCategoryFilter == cat
                    val bg = if (isSel) PalGoldCalligraphy else PalBlackNormal
                    val fg = if (isSel) Color.White else Color.Gray
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(bg)
                            .clickable { selectedCategoryFilter = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cat, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            OutlinedTextField(
                value = expenseReportDate,
                onValueChange = { expenseReportDate = it },
                label = { Text("تعيين وتخصيص تاريخ التقرير المطبوع", color = PalWhiteMuted, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth().testTag("expenses_report_date_input"),
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

            Spacer(modifier = Modifier.height(2.dp))

            // Print / Export Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        val headers = listOf("التصنيف وفئة المصروف", "البيان والشرح", "المبلغ المقيد")
                        val rows = filteredList.map { exp ->
                            listOf(exp.category, exp.notes.ifEmpty { "إداري" }, Helpers.formatMoney(exp.amount))
                        }
                        Helpers.generatePdfAndShare(
                            context = context,
                            title = "كشف المصروفات والمنصرفات ($selectedPeriod)",
                            headers = headers,
                            rows = rows,
                            totals = mapOf("إجمالي المصروفات للفترة المختارة" to Helpers.formatMoney(totalExpenseSum)),
                            customDate = expenseReportDate,
                            reportPeriod = if (selectedPeriod == "تقويم مخصص") "$customExpenseReportType : $customExpenseReportValue" else selectedPeriod
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("PDF تصدير", color = Color.White, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        Helpers.shareViaWhatsApp(context, "", printExpensesMsg)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("واتساب", color = Color.White, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        Helpers.shareViaSMS(context, "", printExpensesMsg)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("إرسال SMS", color = Color.White, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        bluetoothPrintText = printExpensesMsg
                        showBluetoothPrintTrigger = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGoldCalligraphy),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("طباعة 🖨️", color = Color.White, fontSize = 11.sp)
                }
            }

            Divider(color = PalBlackLight)

            // Expenses List display
            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("لا توجد بنود مصروفات مطابقة للبحث مسبقاً", color = PalWhiteMuted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { exp ->
                        Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PalRedLight.copy(0.1f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = PalRedLight, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.category, color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("شرح: ${exp.notes.ifEmpty { "مصاريف إدارية عمومية" }}", color = PalWhiteMuted, fontSize = 10.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Helpers.formatMoney(exp.amount), color = PalRedLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(exp.date.split(" ")[0], color = PalWhiteMuted, fontSize = 9.sp)
                                }
                                IconButton(onClick = {
                                    editingExpense = exp
                                    selectedCategory = exp.category
                                    inputAmount = exp.amount.toInt().toString()
                                    inputNotes = exp.notes
                                    showExpenseDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل المصروف", tint = PalGreenLight)
                                }
                                IconButton(onClick = {
                                    expenseToDelete = exp
                                    showDeleteConfirm = true
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = PalWhiteMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBluetoothPrintTrigger) {
        com.example.ui.components.BluetoothPrintDialog(
            receiptText = bluetoothPrintText,
            onDismiss = { showBluetoothPrintTrigger = false }
        )
    }

    if (showExpenseDialog) {
        AlertDialog(
            onDismissRequest = { 
                showExpenseDialog = false
                editingExpense = null
            },
            title = { Text(if (editingExpense != null) "تعديل بند المصروف" else "قيد بند مصروفات تشغيلي", color = PalWhitePure, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تصنيف المصروف:", color = PalWhiteMuted, fontSize = 12.sp)
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedCategory == cat) PalGreenDark else Color.Transparent)
                                .clickable { selectedCategory = cat }
                                .padding(8.dp)
                        ) {
                            Text(cat, color = PalWhitePure, fontSize = 13.sp)
                        }
                        Divider(color = PalBlackLight)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it },
                        label = { Text("مبلغ المصروف (ريال)", color = PalWhiteMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputNotes,
                        onValueChange = { inputNotes = it },
                        label = { Text("ملاحظات أخرى وشرح بند الصادر", color = PalWhiteMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = inputAmount.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            android.widget.Toast.makeText(context, "الرجاء تحديد مبلغ مصروفات صحيح!", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (editingExpense != null) {
                            viewModel.updateExpense(editingExpense!!.copy(category = selectedCategory, amount = amount, notes = inputNotes))
                            editingExpense = null
                        } else {
                            viewModel.recordExpense(selectedCategory, amount, inputNotes)
                        }
                        showExpenseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                ) {
                    Text(if (editingExpense != null) "حفظ التعديل" else "قيد الصادر", color = PalWhitePure)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showDeleteConfirm && expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "تأكيد حذف المصروف",
                    color = PalWhitePure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف هذا المصروف بقيمة (${com.example.util.Helpers.formatMoney(expenseToDelete!!.amount)}) من فئة (${expenseToDelete!!.category})؟ لا يمكن التراجع عن هذه العملية.",
                    color = PalWhiteSoft,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(expenseToDelete!!)
                        showDeleteConfirm = false
                        expenseToDelete = null
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val invoices by viewModel.allInvoices.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val taxHistory by viewModel.allTaxHistory.collectAsState()
    val supplies by viewModel.allSuppliers.collectAsState()
    val items by viewModel.inventoryItems.collectAsState()
    val purchases by viewModel.allPurchases.collectAsState()

    var selectedPeriod by remember { mutableStateOf(0) } // 0: Daily, 1: Weekly, 2: Monthly, 3: Yearly, 4: Custom date
    var customPeriodType by remember { mutableStateOf("يوم") }
    var customPeriodValue by remember { mutableStateOf(Helpers.getCurrentDate()) }
    var accountingReportPrintDate by remember { mutableStateOf(Helpers.getCurrentDateTime()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التحاسب التجاري والتقارير العامة", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PalWhitePure)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedPeriod,
                containerColor = PalBlackNormal,
                contentColor = PalGreenLight,
                edgePadding = 8.dp
            ) {
                Tab(selected = selectedPeriod == 0, onClick = { selectedPeriod = 0 }, text = { Text("اليومية", fontSize = 12.sp) })
                Tab(selected = selectedPeriod == 1, onClick = { selectedPeriod = 1 }, text = { Text("أسبوعية", fontSize = 12.sp) })
                Tab(selected = selectedPeriod == 2, onClick = { selectedPeriod = 2 }, text = { Text("الشهرية", fontSize = 12.sp) })
                Tab(selected = selectedPeriod == 3, onClick = { selectedPeriod = 3 }, text = { Text("السنوية", fontSize = 12.sp) })
                Tab(selected = selectedPeriod == 4, onClick = { selectedPeriod = 4 }, text = { Text("تقويم مخصص", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            }

            if (selectedPeriod == 4) {
                UnifiedPeriodSelector(
                    initialType = customPeriodType,
                    initialValue = customPeriodValue,
                    onPeriodChanged = { type, finalVal ->
                        customPeriodType = type
                        customPeriodValue = finalVal
                    }
                )
            }

            // Calculations based on Period selection
            val todayStr = Helpers.getCurrentDate()
            val curMonthStr = todayStr.substring(0, 7)
            val curYearStr = todayStr.substring(0, 4)

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val todayTime = try { sdf.parse(todayStr)?.time ?: 0L } catch(e: Exception) { 0L }
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

            val filteredInvoices = when (selectedPeriod) {
                0 -> invoices.filter { it.date.startsWith(todayStr) }
                1 -> invoices.filter { isWeekly(it.date) }
                2 -> invoices.filter { it.date.startsWith(curMonthStr) }
                3 -> invoices.filter { it.date.startsWith(curYearStr) }
                else -> invoices.filter { it.date.startsWith(customPeriodValue) }
            }
            val filteredExpenses = when (selectedPeriod) {
                0 -> expenses.filter { it.date.startsWith(todayStr) }
                1 -> expenses.filter { isWeekly(it.date) }
                2 -> expenses.filter { it.date.startsWith(curMonthStr) }
                3 -> expenses.filter { it.date.startsWith(curYearStr) }
                else -> expenses.filter { it.date.startsWith(customPeriodValue) }
            }
            val filteredPurchases = when (selectedPeriod) {
                0 -> purchases.filter { it.date.startsWith(todayStr) }
                1 -> purchases.filter { isWeekly(it.date) }
                2 -> purchases.filter { it.date.startsWith(curMonthStr) }
                3 -> purchases.filter { it.date.startsWith(curYearStr) }
                else -> purchases.filter { it.date.startsWith(customPeriodValue) }
            }
            val filteredTaxHistory = when (selectedPeriod) {
                0 -> taxHistory.filter { it.date.startsWith(todayStr) }
                1 -> taxHistory.filter { isWeekly(it.date) }
                2 -> taxHistory.filter { it.date.startsWith(curMonthStr) }
                3 -> taxHistory.filter { it.date.startsWith(curYearStr) }
                else -> taxHistory.filter { it.date.startsWith(customPeriodValue) }
            }
            
            val totalSales = filteredInvoices.sumOf { it.totalAmount }
            val totalDisc = filteredInvoices.sumOf { it.discount }
            val totalExpenses = filteredExpenses.sumOf { it.amount }
            val totalPurchases = filteredPurchases.sumOf { it.totalAmount }
            val totalTaxAmount = filteredTaxHistory.sumOf { it.taxAmount }
            val totalStallOutflow = filteredTaxHistory.sumOf { it.stallOutflow }
            val totalLaborOutflow = filteredTaxHistory.sumOf { it.laborOutflow }

            // Precise Accounting Formula: Sales - Purchases - Tax - StallOutflow - LaborOutflow - Expenses
            val netApproxProfit = totalSales - totalPurchases - totalTaxAmount - totalStallOutflow - totalLaborOutflow - totalExpenses

            val pLabel = when (selectedPeriod) {
                0 -> "اليومية"
                1 -> "الأسبوعية"
                2 -> "الشهرية"
                3 -> "السنوية"
                else -> "تقويم مخصص"
            }
            val pValDetail = when (selectedPeriod) {
                0 -> todayStr
                1 -> "7 أيام الماضية"
                2 -> curMonthStr
                3 -> curYearStr
                else -> customPeriodValue
            }

            Text("ملخص أداء الفترة المختارة ($pLabel - $pValDetail)", color = PalWhitePure, fontWeight = FontWeight.Bold)

            Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي المبيعات (+):", color = PalWhiteSoft)
                        Text(Helpers.formatMoney(totalSales), color = PalGreenLight, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي المشتريات (-):", color = PalWhiteSoft)
                        Text("- ${Helpers.formatMoney(totalPurchases)}", color = PalRedLight)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي ضريبة القات (-):", color = PalWhiteSoft)
                        Text("- ${Helpers.formatMoney(totalTaxAmount)}", color = PalRedLight)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي خرج المفرش (-):", color = PalWhiteSoft)
                        Text("- ${Helpers.formatMoney(totalStallOutflow)}", color = PalRedLight)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي خرج العمال (-):", color = PalWhiteSoft)
                        Text("- ${Helpers.formatMoney(totalLaborOutflow)}", color = PalRedLight)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي مصروفات التشغيل الأخرى (-):", color = PalWhiteSoft)
                        Text("- ${Helpers.formatMoney(totalExpenses)}", color = PalRedLight)
                    }
                    Divider(color = PalBlackLight)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("صافي الأرباح والخسائر للوكالة:", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        val ratingColor = if (netApproxProfit >= 0) PalGoldCalligraphy else PalRedLight
                        Text(Helpers.formatMoney(netApproxProfit), color = ratingColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            OutlinedTextField(
                value = accountingReportPrintDate,
                onValueChange = { accountingReportPrintDate = it },
                label = { Text("تعيين وتخصيص تاريخ التقرير المطبوع", color = PalWhiteMuted, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth().testTag("accounts_report_date_input"),
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

            Button(
                onClick = {
                    val headers = listOf("البند المحاسبي", "المبلغ المالي")
                    val rows = listOf(
                        listOf("إجمالي المبيعات المحققة", Helpers.formatMoney(totalSales)),
                        listOf("إجمالي المشتريات والمخازن", Helpers.formatMoney(totalPurchases)),
                        listOf("ضرائب القات المسددة", Helpers.formatMoney(totalTaxAmount)),
                        listOf("خرج المفرش المعتمد", Helpers.formatMoney(totalStallOutflow)),
                        listOf("خرج العمال اليومي المعتمد", Helpers.formatMoney(totalLaborOutflow)),
                        listOf("إجمالي المصاريف والتشغيل", Helpers.formatMoney(totalExpenses))
                    )
                    Helpers.generatePdfAndShare(
                        context = context,
                        title = "التقرير المحاسبي العام ($pLabel) للوكالة",
                        headers = headers,
                        rows = rows,
                        totals = mapOf("صافي الأرباح والخسائر النهائي للوكالة" to Helpers.formatMoney(netApproxProfit)),
                        customDate = accountingReportPrintDate,
                        reportPeriod = if (selectedPeriod == 4) "$customPeriodType : $customPeriodValue" else pValDetail
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("تصدير التقرير المحاسبي PDF", color = PalWhitePure)
            }
        }
    }
}

sealed class ArchiveItem {
    abstract val id: Int
    abstract val date: String
    abstract val amount: Double
    abstract val typeArabic: String

    data class Invoice(val data: com.example.data.SalesInvoice) : ArchiveItem() {
        override val id = data.id
        override val date = data.date
        override val amount = data.totalAmount
        override val typeArabic = "مبيعات"
    }

    data class Purchase(val data: com.example.data.SupplierPurchase, val supplierName: String) : ArchiveItem() {
        override val id = data.id
        override val date = data.date
        override val amount = data.totalAmount
        override val typeArabic = "شراء"
    }

    data class GeneralExpense(val data: com.example.data.Expense) : ArchiveItem() {
        override val id = data.id
        override val date = data.date
        override val amount = data.amount
        override val typeArabic = "مصروف"
    }

    data class Transfer(val data: com.example.data.MoneyTransfer) : ArchiveItem() {
        override val id = data.id
        override val date = data.date
        override val amount = data.amount
        override val typeArabic = "حوالة"
    }

    data class CustomerDebt(val data: com.example.data.SalesInvoice) : ArchiveItem() {
        override val id = -data.id
        override val date = data.date
        override val amount = data.debtAmount
        override val typeArabic = "دين عميل"
    }

    data class SupplierDebt(val data: com.example.data.SupplierPurchase, val supplierName: String) : ArchiveItem() {
        override val id = -data.id - 100000
        override val date = data.date
        override val amount = data.debtRemaining
        override val typeArabic = "دين مورد"
    }

    data class Collection(val data: com.example.data.CustomerPayment, val customerName: String) : ArchiveItem() {
        override val id = data.id + 200000
        override val date = data.date
        override val amount = data.amount
        override val typeArabic = "تحصيل"
    }
}

data class ArchiveUIModel(
    val title: String,
    val sub: String,
    val valStr: String,
    val colorVal: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    
    val invoices by viewModel.allInvoices.collectAsState()
    val purchases by viewModel.allPurchases.collectAsState()
    val suppliers by viewModel.allSuppliers.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val transfers by viewModel.allTransfers.collectAsState()
    val payments by viewModel.allPayments.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()

    var searchStr by remember { mutableStateOf("") }
    var showShareArchiveDialog by remember { mutableStateOf(false) }
    var selectedSharePeriod by remember { mutableStateOf("الكل") } // الكل, يومي, شهري, سنوي, تقويم مخصص
    var selectedShareCategory by remember { mutableStateOf("الكل") } // الكل, المبيعات, المشتريات, المصروفات, الحوالات, الديون, التحصيلات
    var customShareType by remember { mutableStateOf("date") }
    var customShareValue by remember { mutableStateOf("") }
    var customSharePrintDate by remember { mutableStateOf(com.example.util.Helpers.getCurrentDate()) }
    
    var showBluetoothPrintTrigger by remember { mutableStateOf(false) }
    var bluetoothPrintText by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableStateOf(0) } // 0: الكل, 1: مبيعات, 2: مشتريات, 3: مصروفات, 4: حوالات, 5: ديون, 6: تحصيلات
    val categories = listOf("الكل", "المبيعات", "المشتريات", "المصروفات", "الحوالات", "الديون", "التحصيلات")

    // Compile into single list of ArchiveItems
    val allItems = remember(invoices, purchases, suppliers, expenses, transfers, payments, customers) {
        val list = mutableListOf<ArchiveItem>()
        invoices.forEach { list.add(ArchiveItem.Invoice(it)) }
        purchases.forEach { p ->
            val supName = suppliers.find { it.id == p.supplierId }?.name ?: "مورد غير معروف"
            list.add(ArchiveItem.Purchase(p, supName))
        }
        expenses.forEach { list.add(ArchiveItem.GeneralExpense(it)) }
        transfers.forEach { list.add(ArchiveItem.Transfer(it)) }

        // Add Debts (الديون)
        invoices.filter { it.debtAmount > 0 }.forEach { list.add(ArchiveItem.CustomerDebt(it)) }
        purchases.filter { it.debtRemaining > 0 }.forEach { p ->
            val supName = suppliers.find { it.id == p.supplierId }?.name ?: "مورد غير معروف"
            list.add(ArchiveItem.SupplierDebt(p, supName))
        }

        // Add Collections (التحصيلات)
        payments.forEach { pay ->
            val custName = customers.find { it.id == pay.customerId }?.name ?: "عميل غير معروف"
            list.add(ArchiveItem.Collection(pay, custName))
        }

        // Sort by date descending
        list.sortByDescending { it.date }
        list
    }

    // Filter by type & search text
    val filtered = remember(allItems, searchStr, selectedCategoryIndex) {
        allItems.filter { item ->
            // Category filter
            val matchesCategory = when (selectedCategoryIndex) {
                0 -> true
                1 -> item is ArchiveItem.Invoice
                2 -> item is ArchiveItem.Purchase
                3 -> item is ArchiveItem.GeneralExpense
                4 -> item is ArchiveItem.Transfer
                5 -> item is ArchiveItem.CustomerDebt || item is ArchiveItem.SupplierDebt
                6 -> item is ArchiveItem.Collection
                else -> true
            }
            if (!matchesCategory) return@filter false

            // Search filter
            if (searchStr.trim().isEmpty()) return@filter true
            
            val query = searchStr.trim().lowercase()
            val matchesSearch = when (item) {
                is ArchiveItem.Invoice -> {
                    item.data.customerName.lowercase().contains(query) ||
                            item.data.date.contains(query) ||
                            item.data.id.toString().contains(query) ||
                            item.data.totalAmount.toString().contains(query)
                }
                is ArchiveItem.Purchase -> {
                    item.supplierName.lowercase().contains(query) ||
                            item.data.date.contains(query) ||
                            item.data.id.toString().contains(query) ||
                            item.data.totalAmount.toString().contains(query)
                }
                is ArchiveItem.GeneralExpense -> {
                    item.data.category.lowercase().contains(query) ||
                            item.data.notes.lowercase().contains(query) ||
                            item.data.date.contains(query) ||
                            item.data.amount.toString().contains(query)
                }
                is ArchiveItem.Transfer -> {
                    item.data.sender.lowercase().contains(query) ||
                            item.data.receiver.lowercase().contains(query) ||
                            item.data.notes.lowercase().contains(query) ||
                            item.data.date.contains(query) ||
                            item.data.amount.toString().contains(query)
                }
                is ArchiveItem.CustomerDebt -> {
                    item.data.customerName.lowercase().contains(query) ||
                            item.data.date.contains(query) ||
                            item.data.id.toString().contains(query) ||
                            item.data.debtAmount.toString().contains(query)
                }
                is ArchiveItem.SupplierDebt -> {
                    item.supplierName.lowercase().contains(query) ||
                            item.data.date.contains(query) ||
                            item.data.id.toString().contains(query) ||
                            item.data.debtRemaining.toString().contains(query)
                }
                is ArchiveItem.Collection -> {
                    item.customerName.lowercase().contains(query) ||
                            item.data.notes.lowercase().contains(query) ||
                            item.data.date.contains(query) ||
                            item.data.amount.toString().contains(query)
                }
            }
            matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أرشيف ومحفوظات السجلات الكلي", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PalWhitePure)
                    }
                },
                actions = {
                    IconButton(onClick = { showShareArchiveDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "تصدير الأرشيف", tint = PalGreenLight)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchStr,
                onValueChange = { searchStr = it },
                placeholder = { Text("البحث في الأرشيف (بالاسم، التاريخ، المبلغ، الملاحظات)...", color = PalWhiteMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PalWhiteMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category filters horizontal scrollable list
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(categories) { index, catName ->
                    val isSelected = selectedCategoryIndex == index
                    AssistChip(
                        onClick = { selectedCategoryIndex = index },
                        label = { Text(catName, color = if (isSelected) PalBlackDark else PalWhitePure, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) PalGreenLight else PalBlackNormal
                        ),
                        border = if (isSelected) {
                            BorderStroke(1.dp, PalGreenLight)
                        } else {
                            BorderStroke(1.dp, PalBlackLight)
                        }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("لا توجد فواتير أو حركات مؤرشفة مطابقة لعملية البحث", color = PalWhiteMuted, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { item ->
                        val uiModel = when (item) {
                            is ArchiveItem.Invoice -> {
                                val cName = item.data.customerName.ifEmpty { "عميل نقدي" }
                                ArchiveUIModel(
                                    title = "فاتورة مبيعات للعميل: $cName",
                                    sub = "رقم الفاتورة: #${item.data.id} • طريقة الدفع: ${item.data.paymentMethod} • التاريخ: ${item.data.date}",
                                    valStr = "+ ${com.example.util.Helpers.formatMoney(item.data.totalAmount)}",
                                    colorVal = PalGreenLight,
                                    icon = Icons.Default.ReceiptLong
                                )
                            }
                            is ArchiveItem.Purchase -> {
                                ArchiveUIModel(
                                    title = "سند شراء بضاعة من المورد: ${item.supplierName}",
                                    sub = "رقم التوريد: #${item.data.id} • طريقة الدفع: ${item.data.paymentMethod} • التاريخ: ${item.data.date}",
                                    valStr = "- ${com.example.util.Helpers.formatMoney(item.data.totalAmount)}",
                                    colorVal = PalRedLight,
                                    icon = Icons.Default.ShoppingCart
                                )
                            }
                            is ArchiveItem.GeneralExpense -> {
                                ArchiveUIModel(
                                    title = "مصروف عام: ${item.data.category}",
                                    sub = "البيان: ${item.data.notes.ifEmpty { "لا يوجد" }} • التاريخ: ${item.data.date}",
                                    valStr = "- ${com.example.util.Helpers.formatMoney(item.data.amount)}",
                                    colorVal = PalWhiteMuted,
                                    icon = Icons.Default.TrendingDown
                                )
                            }
                            is ArchiveItem.Transfer -> {
                                ArchiveUIModel(
                                    title = "حوالة مالية: من (${item.data.sender}) إلى (${item.data.receiver})",
                                    sub = "رقم المعاملة: ${item.data.referenceNumber.ifEmpty { "غير محدد" }} • البيان: ${item.data.notes} • التاريخ: ${item.data.date}",
                                    valStr = com.example.util.Helpers.formatMoney(item.data.amount),
                                    colorVal = Color(0xFF3b82f6),
                                    icon = Icons.Default.CompareArrows
                                )
                            }
                            is ArchiveItem.CustomerDebt -> {
                                val cName = item.data.customerName.ifEmpty { "عميل" }
                                ArchiveUIModel(
                                    title = "دين آجل على العميل: $cName",
                                    sub = "رقم الفاتورة المرجعية: #${item.data.id} • التاريخ: ${item.data.date}",
                                    valStr = "- ${com.example.util.Helpers.formatMoney(item.data.debtAmount)}",
                                    colorVal = PalRedLight,
                                    icon = Icons.Default.TrendingDown
                                )
                            }
                            is ArchiveItem.SupplierDebt -> {
                                ArchiveUIModel(
                                    title = "دين آجل للمورد: ${item.supplierName}",
                                    sub = "رقم فاتورة الشراء: #${item.data.id} • التاريخ: ${item.data.date}",
                                    valStr = "- ${com.example.util.Helpers.formatMoney(item.data.debtRemaining)}",
                                    colorVal = PalRedLight,
                                    icon = Icons.Default.TrendingDown
                                )
                            }
                            is ArchiveItem.Collection -> {
                                ArchiveUIModel(
                                    title = "تحصيل دفعة مالية من العميل: ${item.customerName}",
                                    sub = "رقم السند: #${item.data.id} • البيان: ${item.data.notes} • التاريخ: ${item.data.date}",
                                    valStr = "+ ${com.example.util.Helpers.formatMoney(item.data.amount)}",
                                    colorVal = PalGreenLight,
                                    icon = Icons.Default.Payments
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(uiModel.colorVal.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(uiModel.icon, contentDescription = null, tint = uiModel.colorVal, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(uiModel.title, color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(uiModel.sub, color = PalWhiteMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                                Text(uiModel.valStr, color = uiModel.colorVal, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareArchiveDialog) {
        val today = com.example.util.Helpers.getCurrentDate()
        val sortedItems = remember(allItems, selectedSharePeriod, selectedShareCategory, customShareValue, customShareType) {
            var list = allItems.toList()
            val sdfStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val todayT = try { sdfStr.parse(today)?.time ?: 0L } catch(e: Exception) { 0L }
            val isWeeklyRecord: (String) -> Boolean = { dateStr ->
                val itemDate = dateStr.take(10)
                try {
                    val itemTime = sdfStr.parse(itemDate)?.time ?: 0L
                    val diff = todayT - itemTime
                    diff in 0..(7L * 24 * 60 * 60 * 1000)
                } catch (e: Exception) {
                    false
                }
            }

            // Filter by period
            list = when (selectedSharePeriod) {
                "يومي" -> list.filter { it.date.startsWith(today) }
                "أسبوعي" -> list.filter { isWeeklyRecord(it.date) }
                "شهري" -> list.filter { it.date.startsWith(today.substring(0, 7)) }
                "سنوي" -> list.filter { it.date.startsWith(today.substring(0, 4)) }
                "تقويم مخصص" -> {
                    val targetVal = customShareValue.ifEmpty { today }
                    list.filter { item ->
                        val itemDate = item.date.take(10)
                        when (customShareType) {
                            "date" -> itemDate == targetVal
                            "month" -> itemDate.startsWith(targetVal)
                            "year" -> itemDate.startsWith(targetVal)
                            else -> true
                        }
                    }
                }
                else -> list
            }
            // Filter by category
            list = when (selectedShareCategory) {
                "المبيعات" -> list.filter { it is ArchiveItem.Invoice }
                "المشتريات" -> list.filter { it is ArchiveItem.Purchase }
                "المصروفات" -> list.filter { it is ArchiveItem.GeneralExpense }
                "الحوالات" -> list.filter { it is ArchiveItem.Transfer }
                "الديون" -> list.filter { it is ArchiveItem.CustomerDebt || it is ArchiveItem.SupplierDebt }
                "التحصيلات" -> list.filter { it is ArchiveItem.Collection }
                else -> list
            }
            list.sortedByDescending { it.date }
        }

        // Calculations for totals
        val totalIn = sortedItems.filterIsInstance<ArchiveItem.Invoice>().sumOf { it.amount }
        val totalOutBuy = sortedItems.filterIsInstance<ArchiveItem.Purchase>().sumOf { it.amount }
        val totalOutExp = sortedItems.filterIsInstance<ArchiveItem.GeneralExpense>().sumOf { it.amount }
        val totalTransfers = sortedItems.filterIsInstance<ArchiveItem.Transfer>().sumOf { it.amount }
        val totalCustomerDebts = sortedItems.filterIsInstance<ArchiveItem.CustomerDebt>().sumOf { it.amount }
        val totalSupplierDebts = sortedItems.filterIsInstance<ArchiveItem.SupplierDebt>().sumOf { it.amount }
        val totalCollections = sortedItems.filterIsInstance<ArchiveItem.Collection>().sumOf { it.amount }

        AlertDialog(
            onDismissRequest = { showShareArchiveDialog = false },
            title = {
                Text(
                    text = "مشاركة وتصدير الأرشيف الكلي",
                    color = PalWhitePure,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
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
                        text = "فلترة وتصدير الأرشيف لمشاركتها عبر التقارير التفصيلية أو الواتساب أو الطباعة الحرارية المباشرة:",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(color = PalBlackLight)

                    // Period choices
                    Text("اختر المدى الزمني للتقرير بالأرشيف:", color = PalWhiteSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    val sharePeriods = listOf("الكل", "يومي", "أسبوعي", "شهري", "سنوي", "تقويم مخصص")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            sharePeriods.take(3).forEach { period ->
                                val isSel = selectedSharePeriod == period
                                val bg = if (isSel) PalGreenNormal else PalBlackLight
                                val fg = if (isSel) Color.White else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { selectedSharePeriod = period }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (period) {
                                            "الكل" -> "كامل السجلات"
                                            "يومي" -> "اليوم"
                                            "أسبوعي" -> "أسبوعي"
                                            else -> period
                                        },
                                        color = fg,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            sharePeriods.drop(3).forEach { period ->
                                val isSel = selectedSharePeriod == period
                                val bg = if (isSel) PalGreenNormal else PalBlackLight
                                val fg = if (isSel) Color.White else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { selectedSharePeriod = period }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (period) {
                                            "شهري" -> "الشهر"
                                            "سنوي" -> "العام"
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
                    }

                    if (selectedSharePeriod == "تقويم مخصص") {
                        UnifiedPeriodSelector(
                            initialType = if (customShareType == "date") "يوم" else if (customShareType == "month") "شهر" else "سنة",
                            initialValue = customShareValue.ifEmpty { com.example.util.Helpers.getCurrentDate() },
                            onPeriodChanged = { type, formattedValue ->
                                customShareType = when (type) {
                                    "يوم" -> "date"
                                    "شهر" -> "month"
                                    "سنة" -> "year"
                                    else -> "date"
                                }
                                customShareValue = formattedValue
                                customSharePrintDate = formattedValue
                            }
                        )
                        
                        OutlinedTextField(
                            value = customSharePrintDate,
                            onValueChange = { customSharePrintDate = it },
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

                    // Category choices
                    Text("اختر تجميع السجلات حسب الفئة:", color = PalWhiteSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    val shareCategories = listOf("الكل", "المبيعات", "المشتريات", "المصروفات", "الحوالات", "الديون", "التحصيلات")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            shareCategories.take(3).forEach { cat ->
                                val isSel = selectedShareCategory == cat
                                val bg = if (isSel) PalGreenNormal else PalBlackLight
                                val fg = if (isSel) Color.White else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { selectedShareCategory = cat }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            shareCategories.slice(3..4).forEach { cat ->
                                val isSel = selectedShareCategory == cat
                                val bg = if (isSel) PalGreenNormal else PalBlackLight
                                val fg = if (isSel) Color.White else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { selectedShareCategory = cat }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            shareCategories.drop(5).forEach { cat ->
                                val isSel = selectedShareCategory == cat
                                val bg = if (isSel) PalGreenNormal else PalBlackLight
                                val fg = if (isSel) Color.White else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { selectedShareCategory = cat }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Divider(color = PalBlackLight)

                    // Summary statistics
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PalBlackDark, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("ملخص حركات التصدير المحددة:", color = PalWhiteMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مجموع المبيعات المفلترة:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text(com.example.util.Helpers.formatMoney(totalIn), color = PalGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مجموع التزويد والمشتريات:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text("- " + com.example.util.Helpers.formatMoney(totalOutBuy), color = PalRedLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مجموع المصروفات المحددة:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text("- " + com.example.util.Helpers.formatMoney(totalOutExp), color = PalWhitePure, fontSize = 11.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("قيمة الحوالات المؤرشفة الكلية:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text(com.example.util.Helpers.formatMoney(totalTransfers), color = Color(0xFF3b82f6), fontSize = 11.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي الديون الآجلة للعملاء:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text(com.example.util.Helpers.formatMoney(totalCustomerDebts), color = PalRedLight, fontSize = 11.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي حركات آجل الموردين:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text(com.example.util.Helpers.formatMoney(totalSupplierDebts), color = PalRedLight, fontSize = 11.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي المبالغ والتحصيلات المستلمة:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text(com.example.util.Helpers.formatMoney(totalCollections), color = PalGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Divider(color = PalBlackLight)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مجموع العمليات المتبقية:", color = PalWhitePure, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${sortedItems.size} عملية", color = PalGoldCalligraphy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val shareMsgText = buildString {
                        appendLine("وكالة عاهد الصبري")
                        appendLine("تصدير الأرشيف ومحفوظات السجلات الموحد الكامل")
                        val pStr = if (selectedSharePeriod == "تقويم مخصص") customSharePrintDate else selectedSharePeriod
                        appendLine("نوع المدى الزمني: $pStr")
                        appendLine("نوع السجلات: $selectedShareCategory")
                        appendLine("التاريخ والوقت: ${com.example.util.Helpers.getCurrentDateTime()}")
                        appendLine("=================================")
                        if (sortedItems.isEmpty()) {
                            appendLine("لا توجد عمليات مسجلة متطابقة مع شروط الفلترة لهذه الفترة.")
                        } else {
                            sortedItems.forEachIndexed { idx, item ->
                                val dateClean = item.date
                                when (item) {
                                    is ArchiveItem.Invoice -> {
                                        val cName = item.data.customerName.ifEmpty { "عميل نقدي" }
                                        appendLine("${idx + 1}) [مبيعات] فاتورة بيع للعميل ($cName) #${item.data.id}")
                                        appendLine("   طريقة الدفع: ${item.data.paymentMethod} • التاريخ: $dateClean")
                                        appendLine("   المبلغ الصافي: + ${com.example.util.Helpers.formatMoney(item.data.totalAmount)}")
                                    }
                                    is ArchiveItem.Purchase -> {
                                        appendLine("${idx + 1}) [مشتريات] تزويد بضاعة من المورد (${item.supplierName}) #${item.data.id}")
                                        appendLine("   طريقة الدفع: ${item.data.paymentMethod} • التاريخ: $dateClean")
                                        appendLine("   المبلغ الصافي: - ${com.example.util.Helpers.formatMoney(item.data.totalAmount)}")
                                    }
                                    is ArchiveItem.GeneralExpense -> {
                                        appendLine("${idx + 1}) [مصروف] مصروف عام فئة: (${item.data.category})")
                                        appendLine("   ملاحظة: ${item.data.notes} • التاريخ: $dateClean")
                                        appendLine("   القيمة المخصومة: - ${com.example.util.Helpers.formatMoney(item.data.amount)}")
                                    }
                                    is ArchiveItem.Transfer -> {
                                        appendLine("${idx + 1}) [حوالة] حوالة من (${item.data.sender}) إلى (${item.data.receiver})")
                                        appendLine("   رقم المرجع: ${item.data.referenceNumber} • الملاحظة: ${item.data.notes}")
                                        appendLine("   الصافي: ${com.example.util.Helpers.formatMoney(item.data.amount)} • التاريخ: $dateClean")
                                    }
                                    is ArchiveItem.CustomerDebt -> {
                                        val cName = item.data.customerName.ifEmpty { "عميل" }
                                        appendLine("${idx + 1}) [دين عميل] دين آجل على العميل ($cName)")
                                        appendLine("   رقم الفاتورة: #${item.data.id} • التاريخ: $dateClean")
                                        appendLine("   قيمة الدين: - ${com.example.util.Helpers.formatMoney(item.data.debtAmount)}")
                                    }
                                    is ArchiveItem.SupplierDebt -> {
                                        appendLine("${idx + 1}) [دين مورد] دين آجل للمورد (${item.supplierName})")
                                        appendLine("   رقم الفاتورة: #${item.data.id} • التاريخ: $dateClean")
                                        appendLine("   قيمة الدين: - ${com.example.util.Helpers.formatMoney(item.data.debtRemaining)}")
                                    }
                                    is ArchiveItem.Collection -> {
                                        appendLine("${idx + 1}) [تحصيل] دفعة مالية مستلمة من العميل (${item.customerName})")
                                        appendLine("   رقم السند: #${item.data.id} • البيان: ${item.data.notes} • التاريخ: $dateClean")
                                        appendLine("   المبلغ المستلم: + ${com.example.util.Helpers.formatMoney(item.data.amount)}")
                                    }
                                }
                                appendLine("   -------------------")
                            }
                        }
                        appendLine("=================================")
                        appendLine("📊 إجمالي مبيعات (+): ${com.example.util.Helpers.formatMoney(totalIn)}")
                        appendLine("📊 إجمالي مشتريات وتوريد (-): ${com.example.util.Helpers.formatMoney(totalOutBuy)}")
                        appendLine("📊 إجمالي مصروفات تشغيلية (-): ${com.example.util.Helpers.formatMoney(totalOutExp)}")
                        appendLine("📊 إجمالي قيمة الحوالات: ${com.example.util.Helpers.formatMoney(totalTransfers)}")
                        appendLine("=================================")
                        appendLine("صاحب المفرش والوكالة: عاهد الصبري")
                    }

                    // Action buttons with spacing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                val printPeriod = if (selectedSharePeriod == "تقويم مخصص") customSharePrintDate else selectedSharePeriod
                                val titlePdf = "تصدير تفصيلي للأرشيف وكالة عاهد الصبري ($printPeriod)"
                                val headers = listOf("نوع السند", "البيان والتفاصيل", "القيمة المالية", "التاريخ والوقت")
                                val pdfRows = sortedItems.map { item ->
                                    val (typeStr, descStr, valueStr) = when (item) {
                                        is ArchiveItem.Invoice -> {
                                            val cName = item.data.customerName.ifEmpty { "عميل نقدي" }
                                            Triple("مبيعات", "فاتورة بيع للعميل: $cName (#${item.data.id})", "+ ${com.example.util.Helpers.formatMoney(item.data.totalAmount)}")
                                        }
                                        is ArchiveItem.Purchase -> {
                                            Triple("مشتريات", "شراء وتوريد بضاعة: ${item.supplierName} (#${item.data.id})", "- ${com.example.util.Helpers.formatMoney(item.data.totalAmount)}")
                                        }
                                        is ArchiveItem.GeneralExpense -> {
                                            Triple("مصروف", "مصروف عام: ${item.data.category} - ${item.data.notes}", "- ${com.example.util.Helpers.formatMoney(item.data.amount)}")
                                        }
                                        is ArchiveItem.Transfer -> {
                                            Triple("حوالة", "من ${item.data.sender} إلى ${item.data.receiver} - ${item.data.notes}", com.example.util.Helpers.formatMoney(item.data.amount))
                                        }
                                        is ArchiveItem.CustomerDebt -> {
                                            val cName = item.data.customerName.ifEmpty { "عميل" }
                                            Triple("دين عميل", "دين آجل على العميل: $cName (#${item.data.id})", "- ${com.example.util.Helpers.formatMoney(item.data.debtAmount)}")
                                        }
                                        is ArchiveItem.SupplierDebt -> {
                                            Triple("دين مورد", "دين آجل للمورد: ${item.supplierName} (#${item.data.id})", "- ${com.example.util.Helpers.formatMoney(item.data.debtRemaining)}")
                                        }
                                        is ArchiveItem.Collection -> {
                                            Triple("تحصيل", "تحصيل دفعة من العميل: ${item.customerName} (#${item.data.id}) - ${item.data.notes}", "+ ${com.example.util.Helpers.formatMoney(item.data.amount)}")
                                        }
                                    }
                                    listOf(typeStr, descStr, valueStr, item.date)
                                }
                                val pdfTotals = mapOf(
                                    "إجمالي مبيعات محصلة" to com.example.util.Helpers.formatMoney(totalIn),
                                    "إجمالي مشتريات وتوريد" to com.example.util.Helpers.formatMoney(totalOutBuy),
                                    "إجمالي مصروفات تشغيلية" to com.example.util.Helpers.formatMoney(totalOutExp),
                                    "إجمالي قيمة الحوالات الكلي" to com.example.util.Helpers.formatMoney(totalTransfers)
                                )
                                com.example.util.Helpers.generatePdfAndShare(
                                    context = context,
                                    title = titlePdf,
                                    headers = headers,
                                    rows = pdfRows,
                                    totals = pdfTotals,
                                    customDate = if (selectedSharePeriod == "تقويم مخصص") customSharePrintDate else com.example.util.Helpers.getCurrentDateTime(),
                                    reportPeriod = if (selectedSharePeriod == "تقويم مخصص") {
                                        val displayType = when (customShareType) {
                                            "date" -> "يوم"
                                            "month" -> "شهر"
                                            "year" -> "سنة"
                                            else -> customShareType
                                        }
                                        "$displayType : $customShareValue"
                                    } else {
                                        selectedSharePeriod
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                            modifier = Modifier.weight(1.3f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("تصدير PDF 📄", color = Color.White, fontSize = 9.sp)
                        }

                        Button(
                            onClick = {
                                com.example.util.Helpers.shareViaWhatsApp(context, "", shareMsgText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("واتساب 💬", color = Color.White, fontSize = 9.sp)
                        }

                        Button(
                            onClick = {
                                bluetoothPrintText = shareMsgText
                                showBluetoothPrintTrigger = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGoldCalligraphy),
                            modifier = Modifier.weight(1.1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("طباعة 🖨️", color = Color.White, fontSize = 9.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareArchiveDialog = false }) {
                    Text("إغلاق", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showBluetoothPrintTrigger) {
        com.example.ui.components.BluetoothPrintDialog(
            receiptText = bluetoothPrintText,
            onDismiss = { showBluetoothPrintTrigger = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val printerManager = remember { BluetoothPrinterManager.getInstance(context) }
    
    var showPassDialog by remember { mutableStateOf(false) }
    var inputNewPass by remember { mutableStateOf("") }

    val printers = remember { printerManager.getBondedPrinters() }
    var selectedPrinterName by remember { mutableStateOf(printerManager.connectedDeviceName) }

    val appLockEnabled by viewModel.isAppLockEnabled.collectAsState()

    var localBackupsList by remember { mutableStateOf(emptyList<java.io.File>()) }
    var showBackupRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showBackupDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedBackupForAction by remember { mutableStateOf<java.io.File?>(null) }

    val refreshBackupsList = {
        val backupDir = java.io.File(context.filesDir, "tofan_backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        localBackupsList = backupDir.listFiles { f -> f.isFile && f.name.endsWith(".db") }?.toList()?.sortedByDescending { f -> f.lastModified() } ?: emptyList()
    }

    LaunchedEffect(Unit) {
        refreshBackupsList()
    }

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    var isGeneratingStressData by remember { mutableStateOf(false) }
    var isPurgingDatabase by remember { mutableStateOf(false) }
    var showStressTestingConfirmDialog by remember { mutableStateOf(false) }
    var showPurgeConfirmDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim_transition")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )
    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_rotation"
    )
    val bounceOffsetYAnimFloat by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_bounce"
    )

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val success = Helpers.restoreDatabase(context, uri)
            if (success) {
                android.widget.Toast.makeText(context, "تم استعادة قاعدة البيانات بنجاح! جارٍ إعادة تشغيل التطبيق لتحديث السجلات.", android.widget.Toast.LENGTH_LONG).show()
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(intent)
                (context as? android.app.Activity)?.finish()
                kotlin.system.exitProcess(0)
            } else {
                android.widget.Toast.makeText(context, "فشل استعادة قاعدة البيانات! تأكد من ملف النسخة الاحتياطية الصحيح.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات نظام وكالة عاهد الصبري", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PalWhitePure)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // App Logo & Info Header Card (Moving HD Logo & Information)
            Card(
                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                border = BorderStroke(1.dp, PalGoldCalligraphy.copy(0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.img_agency_logo_1780160497858),
                            contentDescription = "شعار وكالة عاهد الصبري",
                            modifier = Modifier
                                .size(115.dp)
                                .graphicsLayer(
                                    scaleX = scaleAnim,
                                    scaleY = scaleAnim,
                                    rotationZ = rotationAnim
                                )
                                .offset(y = bounceOffsetYAnimFloat.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(2.dp, PalGoldCalligraphy, RoundedCornerShape(22.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = "نظام وكالة عاهد الصبري",
                        color = PalWhitePure,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "لأجود أنواع القات الماوية والورزاني بجميع أنواعها 🍃",
                        color = PalGoldCalligraphy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        color = PalWhiteMuted.copy(0.15f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Text(
                        text = "معلومات الإصدار والنظام المحاسبي:",
                        color = PalWhiteSoft,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "v3.5.0 (نسخة HD المطورة)", color = PalGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "🛡️ رقم إصدار النظام:", color = PalWhiteMuted, fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "عاهد الصبري", color = PalWhitePure, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "👤 المالك وصاحب الوكالة:", color = PalWhiteMuted, fontSize = 11.sp)
                        }
                        Text(
                            text = "تطبيق محاسبي ذكي مصمم خصيصاً لإدارة المبيعات والتوريد، تتبع الديون الخارجية والتحصيلات النقدية، إدارة الحسابات المالية اللحظية، وتصدير التقارير وسندات الصرف وقبض المشتريات للطباعة والمشاركة.",
                            color = PalWhiteSoft,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            }

            Text("المظهر والألوان", color = PalWhitePure, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الوضع الداكن (Dark Mode)", color = PalWhitePure)
                    Switch(
                        checked = viewModel.isDarkMode.collectAsState().value,
                        onCheckedChange = { viewModel.toggleTheme() },
                        colors = SwitchDefaults.colors(checkedThumbColor = PalGreenLight)
                    )
                }
            }

            Text("أمن التطبيق وصلاحيات الدخول", color = PalWhitePure, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("قفل التطبيق بكلمة مرور عند الفتح", color = PalWhitePure)
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { viewModel.toggleAppLock(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PalGreenLight)
                        )
                    }
                    Divider(color = PalBlackLight)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPassDialog = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تغيير كلمة مرور المدير الأدمن", color = PalWhitePure)
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = PalWhiteMuted)
                    }
                }
            }

            Text("إعدادات الطابعة الحرارية الفعلية", color = PalWhitePure, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("اختر طابعة البلوتوث المتاحة:", color = PalWhiteMuted, fontSize = 12.sp)
                    
                    printers.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedPrinterName.contains(p.first)) PalGreenDark else Color.Transparent)
                                .clickable {
                                    val (deviceType, isPrinter) = printerManager.getDeviceTypeNameAndIsPrinter(p.first, p.second)
                                    if (!isPrinter) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "عذراً! الجهاز المختار هو ($deviceType) وليس طابعة حرارية مدعومة ليتم تعيينه كطابعة افتراضية.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "جاري تحديد الطابعة الافتراضية...", android.widget.Toast.LENGTH_SHORT).show()
                                        printerManager.connectPrinter(p.first, p.second) { success ->
                                            if (success) {
                                                selectedPrinterName = "${p.first} (${p.second})"
                                                android.widget.Toast.makeText(context, "تم تحديد الطابعة بنجاح كجهاز افتراضي، وبدء الطباعة...", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(p.first, color = PalWhitePure, fontSize = 13.sp)
                            Text(p.second, color = PalWhiteMuted, fontSize = 11.sp)
                        }
                        Divider(color = PalBlackLight.copy(0.4f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("عرض ورق الطباعة الافتراضي المعتمد:", color = PalWhiteMuted, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { printerManager.savePrinterConfig(58, "") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (printerManager.currentPrinterSize == 58) PalGreenNormal else PalBlackLight
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("58mm")
                        }
                        Button(
                            onClick = { printerManager.savePrinterConfig(80, "") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (printerManager.currentPrinterSize == 80) PalGreenNormal else PalBlackLight
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("80mm")
                        }
                    }
                }
            }

            Text("النسخ الاحتياطي للأمان والأرشفة", color = PalWhitePure, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Create a New Backup
                    Button(
                        onClick = {
                            com.example.data.AppDatabase.checkpoint()
                            val backupDir = java.io.File(context.filesDir, "tofan_backups")
                            if (!backupDir.exists()) {
                                backupDir.mkdirs()
                            }
                            val datePart = Helpers.getCurrentDate().replace("-", "_")
                            val timePart = System.currentTimeMillis()
                            val backupName = "tofan_backup_${datePart}_${timePart}.db"
                            val backupFile = java.io.File(backupDir, backupName)
                            
                            val dbFile = context.getDatabasePath("tofan_al_aqsa_qat_db")
                            try {
                                if (dbFile.exists()) {
                                    dbFile.inputStream().use { input ->
                                        backupFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    refreshBackupsList()
                                    android.widget.Toast.makeText(context, "تم حفظ نسخة احتياطية محلية جديدة بنجاح! 📁", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "لم يندمج ملف قاعدة البيانات حالياً!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "فشل إنشاء النسخة: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنشاء نسخة احتياطية محلية جديدة 📁", color = PalWhitePure)
                    }

                    Button(
                        onClick = {
                            val ok = Helpers.backupDatabase(context)
                            if (ok) {
                                android.widget.Toast.makeText(context, "تم تصدير نسخة احتياطية ومشاركتها سحابياً بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                android.widget.Toast.makeText(context, "فشل تصدير نسخة احتياطية!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalBlackLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = PalGoldCalligraphy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير ومشاركة نسخة احتياطية خارجيًا 📤", color = PalWhitePure)
                    }

                    Button(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalBlackLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استيراد من ملف خارجي (Restore)", color = PalWhitePure)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("قائمة السجلات والنسخ المحفوظة بالكامل (${localBackupsList.size}):", color = PalWhiteSoft, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    if (localBackupsList.isEmpty()) {
                        Text(
                            text = "لا توجد أي نسخ احتياطية محفوظة حالياً في الإعدادات.",
                            color = PalWhiteMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            localBackupsList.forEach { file ->
                                val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))
                                val sizeStr = if (file.length() < 1024) "${file.length()} B" else String.format("%.1f KB", file.length() / 1024.0)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PalBlackLight),
                                    border = BorderStroke(0.5.dp, PalWhiteMuted.copy(0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(text = "نسخة احتياطية: ${file.name.substringBeforeLast(".db")}", color = PalWhitePure, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = "التاريخ: $dateStr", color = PalWhiteSoft, fontSize = 9.sp)
                                                Text(text = "الحجم: $sizeStr", color = PalGreenLight, fontSize = 9.sp)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Button(
                                                    onClick = {
                                                        selectedBackupForAction = file
                                                        showBackupRestoreConfirmDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenDark),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("استعادة 🔄", fontSize = 10.sp, color = PalWhitePure)
                                                }
                                                Button(
                                                    onClick = {
                                                        selectedBackupForAction = file
                                                        showBackupDeleteConfirmDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("حذف 🗑️", fontSize = 10.sp, color = PalWhitePure)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Developer Signature and Copyright Section - المهندس محمد امين ردمان عبدالله الحاج
            Text(
                text = "مطور النظام والدعم الفني",
                color = PalWhitePure,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                border = BorderStroke(1.5.dp, PalGoldCalligraphy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مصمم وبناء وتطوير النظام",
                            color = PalGoldCalligraphy,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = PalGoldCalligraphy,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Developer Name
                    Text(
                        text = "المهندس محمد امين ردمان عبدالله الحاج",
                        color = PalWhitePure,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Work description Custom text
                    Text(
                        text = "منشئ ومصمم وبناء وتطوير التطبيق المحاسبي والاداري والمالي المتكامل لوكالة عاهد الصبري لتجارة وتوريد أصناف القات الماوية والورزانية بجميع أنواعها.",
                        color = PalWhiteSoft,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        color = PalWhiteMuted.copy(0.12f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Phone & Copyright labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+967780961823",
                            color = PalGreenLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الحقوق محفوظة © 2026",
                            color = PalWhiteMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Large interactive WhatsApp button
                    Button(
                        onClick = {
                            val whatsappUrl = "https://wa.me/967780961823"
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(whatsappUrl)
                                ).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "تعذر فتح الواتساب تلقائياً، يرجى مراسلة الرقم: +967780961823",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ارسل رسالة مباشرة عبر الواتساب",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stress, performance, and clear data panel
            Text(
                text = "أدوات مطور النظام واختبارات الأداء",
                color = PalWhitePure,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                border = BorderStroke(1.dp, PalWhiteMuted.copy(0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "قسم اختبارات تحمل ومقاومة النظام للضغط والبيانات الضخمة (Stress Testing Panel)",
                        color = PalGoldCalligraphy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "هذه الأدوات مخصصة لمحاكاة الاستخدام الفعلي وتوليد آلاف القيود والعمليات الحسابية والفواتير والتحقق من ترابط وسرعة أرشفة وسرعة بحث التقارير 100% دون أي بطء أو تعطيل.",
                        color = PalWhiteSoft,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isGeneratingStressData) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = PalGreenLight, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "جاري تعبئة وتوليد 1,500 حركة مالية وفاتورة بلحظات...",
                                color = PalGreenLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isPurgingDatabase) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = PalRedNormal, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "جاري تصفير قاعدة البيانات وتجديد الاستقرار...",
                                color = PalRedNormal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showStressTestingConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("توليد 1500 فاتورة ضغط", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showPurgeConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal.copy(0.15f)),
                                border = BorderStroke(1.dp, PalRedNormal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = PalRedNormal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تطهير وتصفير القيود", color = PalRedNormal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showLogoutConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("تسجيل الخروج من النظام المحاسبي", color = PalWhitePure, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = "تأكّيد تسجيل الخروج",
                    color = PalWhitePure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في تسجيل الخروج خارج نظام وكالة عاهد الصبري المحاسبي؟",
                    color = PalWhiteSoft,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، خروج", color = PalWhitePure, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirmDialog = false }
                ) {
                    Text("إلغاء", color = PalWhiteMuted, fontWeight = FontWeight.Normal)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showStressTestingConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStressTestingConfirmDialog = false },
            title = {
                Text(
                    text = "بدء اختبار الضغط الأقصى",
                    color = PalWhitePure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Text(
                    text = "سيقوم النظام فوراً بتوليد 1,500 حركة مالية حقيقية (فواتير بيع مع تفاصيلها، ديون، تحصيلات، موردين، مصروفات وحوالات) مقسمة على آخر شهر، لإثبات ودراسة ثبات واستقرار وتجاوب النظام وقواعد البيانات تحت الضغوطات والكميات الضخمة من البيانات المحوسبة. هل تريد البدء؟",
                    color = PalWhiteSoft,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStressTestingConfirmDialog = false
                        isGeneratingStressData = true
                        viewModel.runStressTestingAndLoad(invoiceCount = 800, customerCount = 150) {
                            isGeneratingStressData = false
                            android.widget.Toast.makeText(context, "تم توليد بيانات اختبار الضغط المتكاملة بنجاح واستقرار فائق 100%!", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، ابدأ المحاكاة", color = PalWhitePure, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStressTestingConfirmDialog = false }
                ) {
                    Text("إلغاء", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showPurgeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            title = {
                Text(
                    text = "تأكيد مسح وتطهير النظام",
                    color = PalRedNormal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Text(
                    text = "تحذير: سيتم حذف وتصفير جميع فواتير البيع والمشتريات والديون والعملاء والمصروفات المسجلة بقاعدة البيانات نهائياً لتطهير النظام. هل أنت متأكد بنسبة 100% من رغبتك في الحذف الكامل؟",
                    color = PalWhiteSoft,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPurgeConfirmDialog = false
                        isPurgingDatabase = true
                        viewModel.purgeSystemDatabase {
                            isPurgingDatabase = false
                            android.widget.Toast.makeText(context, "تم تطهير وتصفير قاعدة بيانات الوكالة بالكامل رغبة من المطور!", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، تصفير كلي", color = PalWhitePure, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPurgeConfirmDialog = false }
                ) {
                    Text("إلغاء", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showPassDialog) {
        AlertDialog(
            onDismissRequest = { showPassDialog = false },
            title = { Text("تغيير كلمة المرور", color = PalWhitePure, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
            text = {
                OutlinedTextField(
                    value = inputNewPass,
                    onValueChange = { inputNewPass = it },
                    label = { Text("أدخل كلمة المرور الجديدة للأدمن", color = PalWhiteMuted) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputNewPass.trim().isNotEmpty()) {
                            viewModel.changeAdminPassword(inputNewPass)
                            android.widget.Toast.makeText(context, "تم تحديث كلمة المرور الجديدة بنجاح!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showPassDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                ) {
                    Text("تأكيد وحفظ", color = PalWhitePure)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showBackupRestoreConfirmDialog && selectedBackupForAction != null) {
        AlertDialog(
            onDismissRequest = { showBackupRestoreConfirmDialog = false },
            title = {
                Text(
                    text = "تأكيد استعادة قاعدة البيانات 🔄",
                    color = PalWhitePure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في استعادة هذا الأرشيف بالكامل؟ سيتم استبدال قاعدة البيانات الحالية النشطة بالكامل ببيانات هذه النسخة، وسيتم إعادة تشغيل التطبيق تلقائياً لتطبيق التغييرات بشكل صحيح وقاطع.",
                    color = PalWhiteSoft,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = selectedBackupForAction!!
                        val dbFile = context.getDatabasePath("tofan_al_aqsa_qat_db")
                        val dbWal = java.io.File(dbFile.path + "-wal")
                        val dbShm = java.io.File(dbFile.path + "-shm")
                        try {
                            // Safely close active Room connection and reset singleton instance
                            try {
                                com.example.data.AppDatabase.closeDatabase()
                            } catch(ex: Exception) {
                                // ignore
                            }

                            val tempFile = java.io.File.createTempFile("db_restore", null, context.cacheDir)
                            file.inputStream().use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            if (!tempFile.exists() || tempFile.length() == 0L) {
                                throw Exception("فشل قراءة ملف النسخة الاحتياطية أو الملف فارغ")
                            }

                            if (dbFile.exists()) dbFile.delete()
                            if (dbWal.exists()) dbWal.delete()
                            if (dbShm.exists()) dbShm.delete()

                            tempFile.copyTo(dbFile, overwrite = true)
                            tempFile.delete()
                            
                            android.widget.Toast.makeText(context, "تمت استعادة النسخة بنجاح! جاري جلب ومزامنة أحدث السجلات والمشتريات وإعادة تشغيل النظام المحاسبي...", android.widget.Toast.LENGTH_LONG).show()
                            
                            // Re-launch App clean
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            (context as? android.app.Activity)?.finish()
                            java.lang.Runtime.getRuntime().exit(0)
                        } catch(e: Exception) {
                            android.widget.Toast.makeText(context, "فشل استعادة قاعدة البيانات: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                        showBackupRestoreConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("استعادة وإعادة التشغيل 🔄", color = PalWhitePure, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackupRestoreConfirmDialog = false }
                ) {
                    Text("إلغاء", color = PalWhiteMuted, fontWeight = FontWeight.Normal)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showBackupDeleteConfirmDialog && selectedBackupForAction != null) {
        AlertDialog(
            onDismissRequest = { showBackupDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "تأكيد حذف النسخة الاحتياطية 🗑️",
                    color = PalWhitePure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف ملف هذه النسخة الاحتياطية نهائياً من أرشيف التطبيق؟ لا يمكن التراجع عن هذه العملية.",
                    color = PalWhiteSoft,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = selectedBackupForAction!!
                        if (file.exists()) {
                            file.delete()
                            refreshBackupsList()
                            android.widget.Toast.makeText(context, "تم حذف ملف النسخة الاحتياطية بنجاح بنسق نهائي! 🗑️", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showBackupDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تأكيد الحذف 🗑️", color = PalWhitePure, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackupDeleteConfirmDialog = false }
                ) {
                    Text("إلغاء", color = PalWhiteMuted, fontWeight = FontWeight.Normal)
                }
            },
            containerColor = PalBlackNormal
        )
    }
}
