package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryItem
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import com.example.ui.components.UnifiedPeriodSelector
import com.example.util.BluetoothPrinterManager
import com.example.util.Helpers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val printerManager = remember { BluetoothPrinterManager.getInstance(context) }
    
    val allItems by viewModel.inventoryItems.collectAsState()
    val allInvoices by viewModel.allInvoices.collectAsState()
    val allInvoiceItems by viewModel.allInvoiceItems.collectAsState()
    val allPurchases by viewModel.allPurchases.collectAsState()
    val allPurchaseItems by viewModel.allPurchaseItems.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    
    var showBluetoothPrintTrigger by remember { mutableStateOf(false) }
    var bluetoothPrintText by remember { mutableStateOf("") }
    
    var showReportsDialog by remember { mutableStateOf(false) }
    var selectedReportPeriod by remember { mutableStateOf("يومي") } // يومي, أسبوعي, شهري, سنوي, تقويم مخصص
    var customReportType by remember { mutableStateOf("يوم") }
    var customReportValue by remember { mutableStateOf(Helpers.getCurrentDate()) }
    var customReportPrintDate by remember { mutableStateOf(Helpers.getCurrentDate()) }

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var showMatchingDialog by remember { mutableStateOf(false) }
    var physicalCounts by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var matchingReportDate by remember { mutableStateOf(Helpers.getCurrentDateTime()) }
    
    // Dialog inputs
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("") }
    var itemBuyPrice by remember { mutableStateOf("") }
    var itemSellPrice by remember { mutableStateOf("") }
    var itemLowThreshold by remember { mutableStateOf("") }
    var itemBuyPriceCurrency by remember { mutableStateOf("ريال يمني") }

    val filteredItems = allItems.filter {
        val matchesSearch = searchQuery.trim().isEmpty() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.buyPrice.toString().contains(searchQuery) ||
                (searchQuery.trim() == "حرج" && it.quantity <= it.lowStockThreshold) ||
                (searchQuery.trim() == "نقص" && it.quantity <= it.lowStockThreshold) ||
                (searchQuery.trim() == "نواقص" && it.quantity <= it.lowStockThreshold)
        matchesSearch
    }

    val lowStockCount = allItems.count { it.quantity <= it.lowStockThreshold }
    val totalInventoryValue = allItems.sumOf { it.quantity * it.buyPrice }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المخزون والاصناف", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PalWhitePure)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Export PDF with dateAdded shown in detail
                        val headers = listOf("الصنف", "تاريخ الإضافة بالتفصيل", "الكمية المتوفرة", "سعر الشراء")
                        val rows = filteredItems.map {
                            val qtyText = if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else it.quantity.toString()
                            val dateStr = it.dateAdded.ifEmpty { "غير محدد" }
                            listOf(it.name, dateStr, "$qtyText حبة", Helpers.formatMoney(it.buyPrice))
                        }
                        Helpers.generatePdfAndShare(
                            context = context,
                            title = "كشف جرد المخزون التفصيلي",
                            headers = headers,
                            rows = rows,
                            totals = mapOf("إجمالي القيمة المالية للمخزون" to Helpers.formatMoney(totalInventoryValue))
                        )
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "تصدير PDF", tint = PalRedLight)
                    }
                    IconButton(onClick = {
                        // Thermal Print Inventory
                        val recordText = buildString {
                            appendLine("       كشف جرد المخزون")
                            appendLine("وكالة عاهد الصبري للقات الماوية والورزاني")
                            appendLine("تاريخ الجرد: ${Helpers.getCurrentDateTime()}")
                            appendLine("--------------------------------")
                            appendLine("الصنف          الكمية     السعر")
                            appendLine("--------------------------------")
                            for (item in filteredItems) {
                                val cleanName = if (item.name.length > 12) item.name.substring(0, 11) else item.name
                                appendLine("${cleanName.padEnd(12)} ${item.quantity.toString().padEnd(8)} ${Helpers.formatMoney(item.buyPrice)}")
                            }
                            appendLine("--------------------------------")
                            appendLine("إجمالي قيمة المخزون الدفترية:")
                            appendLine("💰 ${Helpers.formatMoney(totalInventoryValue)}")
                            appendLine("--------------------------------")
                            appendLine("صاحب الوكالة: عاهد الصبري")
                        }
                        bluetoothPrintText = recordText
                        showBluetoothPrintTrigger = true
                    }) {
                        Icon(Icons.Default.Print, contentDescription = "طباعة حرارية", tint = PalGreenLight)
                    }
                    IconButton(onClick = { showMatchingDialog = true }) {
                        Icon(Icons.Default.Balance, contentDescription = "مطابقة الجرد الفعلي والحيود", tint = PalGreenLight)
                    }
                    IconButton(onClick = { showReportsDialog = true }) {
                        Icon(Icons.Default.Summarize, contentDescription = "تقارير الأصناف والعمليات", tint = PalGoldCalligraphy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PalBlackNormal)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedItem = null
                    itemName = ""
                    itemQty = ""
                    itemBuyPrice = ""
                    itemSellPrice = ""
                    itemLowThreshold = "5"
                    itemBuyPriceCurrency = "ريال يمني"
                    showEditDialog = true
                },
                containerColor = PalGreenNormal,
                contentColor = PalWhitePure
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة صنف")
            }
        },
        containerColor = PalBlackDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            
            // Header Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("أنواع الأصناف", color = PalWhiteMuted, fontSize = 11.sp)
                        Text("${allItems.size}", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("قيمة المخزون المالية", color = PalWhiteMuted, fontSize = 11.sp)
                        Text(Helpers.formatMoney(totalInventoryValue), color = PalGreenLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (lowStockCount > 0) PalRedNormal.copy(0.15f) else PalBlackNormal
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("النواقص", color = PalWhiteMuted, fontSize = 11.sp)
                        Text("$lowStockCount", color = if (lowStockCount > 0) PalRedLight else PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("البحث عن صنف...", color = PalWhiteMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PalWhiteMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PalGreenLight,
                    unfocusedBorderColor = PalBlackLight,
                    focusedTextColor = PalWhitePure,
                    unfocusedTextColor = PalWhiteSoft
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد أصناف تطابق البحث", color = PalWhiteMuted, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems) { item ->
                        val isLowStock = item.quantity <= item.lowStockThreshold
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                            border = BorderStroke(1.dp, if (isLowStock) PalRedLight.copy(0.5f) else PalBlackLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedItem = item
                                    itemName = item.name
                                    itemQty = item.quantity.toString()
                                    itemBuyPrice = item.buyPrice.toString()
                                    itemSellPrice = item.sellPrice.toString()
                                    itemLowThreshold = item.lowStockThreshold.toString()
                                    itemBuyPriceCurrency = item.buyPriceCurrency.ifEmpty { "ريال يمني" }
                                    showEditDialog = true
                                }
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(45.dp)
                                            .background(
                                                if (isLowStock) PalRedLight.copy(0.12f) else PalGreenDark.copy(0.12f),
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isLowStock) Icons.Default.Warning else Icons.Default.Inventory,
                                            contentDescription = null,
                                            tint = if (isLowStock) PalRedLight else PalGreenLight
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text("سعر شراء الحبة: ${Helpers.formatMoney(item.buyPrice)} ${item.buyPriceCurrency.ifEmpty { "ريال يمني" }}", color = PalGoldCalligraphy, fontSize = 12.sp)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${item.quantity} حبة",
                                            color = if (isLowStock) PalRedLight else PalWhitePure,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        if (isLowStock) {
                                            Text("مخزون حرج", color = PalRedLight, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        } else {
                                            Text("متوفر جيدا", color = PalGreenLight, fontSize = 10.sp)
                                        }
                                    }
                                }

                                Divider(color = PalBlackLight.copy(0.4f), thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val itemShareMsg = """
وكالة عاهد الصبري
---------------------------------
اسم الصنف: ${item.name}
الكمية المتوفرة: ${item.quantity} حبة
سعر الشراء: ${Helpers.formatMoney(item.buyPrice)}
حالة المخزون: ${if (isLowStock) "مخزون حرج" else "متوفر وجاهز"}
---------------------------------
صاحب الوكالة والمفرش: عاهد الصبري
                                            """.trimIndent()
                                            Helpers.shareViaWhatsApp(context, "", itemShareMsg)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "مشاركة عبر واتساب",
                                            tint = PalGreenLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    IconButton(
                                        onClick = {
                                            val itemReceipt = """
     سند بيانات صنف مخزني
وكالة عاهد الصبري للقات الماوية والورزاني
صنف: ${item.name}
المخزون الحالي: ${item.quantity} حبة
سعر الشراء المعتمد: ${Helpers.formatMoney(item.buyPrice)}
حالة التوفر: ${if (isLowStock) "مخزون حرج" else "متوفر وجاهز"}
--------------------------------
صاحب الوكالة والمخازن: عاهد الصبري
                                            """.trimIndent()
                                            bluetoothPrintText = itemReceipt
                                            showBluetoothPrintTrigger = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "طباعة حرارية لصنف",
                                            tint = PalGoldCalligraphy,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Add / Edit Item Dialog ---
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = if (selectedItem == null) "إضافة صنف نوع قات جديد" else "تعديل بيانات الصنف",
                    color = PalWhitePure,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("اسم الصنف (أمثلة: عود، فراد، قطل، فليق، ماوية، زكري، إلخ)", color = PalWhiteMuted) },
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
                        value = itemQty,
                        onValueChange = { itemQty = it },
                        label = { Text("الكمية المتوفرة (حبة)", color = PalWhiteMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        value = itemBuyPrice,
                        onValueChange = { itemBuyPrice = it },
                        label = { Text("سعر شراء الحبة (ريال)", color = PalWhiteMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("عملة سعر الشراء للرأس/الحبة:", color = PalWhiteMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ريال يمني", "ريال سعودي").forEach { curr ->
                            Button(
                                onClick = { itemBuyPriceCurrency = curr },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (itemBuyPriceCurrency == curr) PalGreenNormal else PalBlackLight
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(curr, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = itemLowThreshold,
                        onValueChange = { itemLowThreshold = it },
                        label = { Text("الحد الأدنى للمخزون (التحذير البصري)", color = PalWhiteMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedItem != null) {
                        Button(
                            onClick = {
                                showDeleteConfirm = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal)
                        ) {
                            Text("حذف", color = PalWhitePure)
                        }
                    }
                    Button(
                        onClick = {
                            val buyVal = itemBuyPrice.toDoubleOrNull() ?: 0.0
                            val sellVal = selectedItem?.sellPrice ?: 0.0
                            val item = InventoryItem(
                                id = selectedItem?.id ?: 0,
                                name = itemName,
                                quantity = itemQty.toDoubleOrNull() ?: 0.0,
                                buyPrice = buyVal,
                                sellPrice = sellVal,
                                lowStockThreshold = itemLowThreshold.toDoubleOrNull() ?: 5.0,
                                buyPriceCurrency = itemBuyPriceCurrency
                            )
                            viewModel.saveInventoryItem(item)
                            showEditDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                    ) {
                        Text("حفظ صنف", color = PalWhitePure)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("إلغاء", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showDeleteConfirm && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "تأكيد حذف الصنف",
                    color = PalWhitePure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف الصنف (${selectedItem!!.name}) نهائياً من المخزن؟ لا يمكن التراجع عن هذا الإجراء.",
                    color = PalWhiteSoft,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInventoryItem(selectedItem!!)
                        showDeleteConfirm = false
                        showEditDialog = false
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

    if (showReportsDialog) {
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

        val filteredInvoices = when (selectedReportPeriod) {
            "يومي" -> allInvoices.filter { it.date.startsWith(todayStr) }
            "أسبوعي" -> allInvoices.filter { isWeekly(it.date) }
            "شهري" -> allInvoices.filter { it.date.startsWith(todayStr.substring(0, 7)) }
            "سنوي" -> allInvoices.filter { it.date.startsWith(todayStr.substring(0, 4)) }
            "تقويم مخصص" -> allInvoices.filter { it.date.startsWith(customReportValue) }
            else -> allInvoices
        }

        val filteredPurchases = when (selectedReportPeriod) {
            "يومي" -> allPurchases.filter { it.date.startsWith(todayStr) }
            "أسبوعي" -> allPurchases.filter { isWeekly(it.date) }
            "شهري" -> allPurchases.filter { it.date.startsWith(todayStr.substring(0, 7)) }
            "سنوي" -> allPurchases.filter { it.date.startsWith(todayStr.substring(0, 4)) }
            "تقويم مخصص" -> allPurchases.filter { it.date.startsWith(customReportValue) }
            else -> allPurchases
        }

        val invoiceIds = filteredInvoices.map { it.id }.toSet()
        val purchaseIds = filteredPurchases.map { it.id }.toSet()

        val invoiceItemsForPeriod = allInvoiceItems.filter { it.invoiceId in invoiceIds }
        val purchaseItemsForPeriod = allPurchaseItems.filter { it.purchaseId in purchaseIds }

        // Compile item stats
        val itemReportRows = allItems.map { item ->
            val soldQty = invoiceItemsForPeriod.filter { it.itemId == item.id }.sumOf { it.quantity }
            val boughtQty = purchaseItemsForPeriod.filter { it.itemId == item.id }.sumOf { it.quantity }
            Triple(item, soldQty, boughtQty)
        }

        val totalUnitsSold = itemReportRows.sumOf { it.second }
        val totalUnitsBought = itemReportRows.sumOf { it.third }

        AlertDialog(
            onDismissRequest = { showReportsDialog = false },
            title = {
                Text(
                    text = "تقرير حركة المخزون والمبيعات بالتفصيل",
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
                        text = "اختر الفترة الزمنية لعرض وتصدير تقرير الجرد التفصيلي أو حدد تقويماً مخصصاً:",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Period Selection Toggles
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

                    // Metrics Banner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PalBlackDark, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي حبات بيعت بالفترة:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text("${totalUnitsSold.toInt()} حبة", color = PalGreenLight, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي حبات وردت بالفترة:", color = PalWhiteSoft, fontSize = 11.sp)
                            Text("${totalUnitsBought.toInt()} حبة", color = PalGoldCalligraphy, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي قيمة المخزون الحالي:", color = PalWhitePure, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(Helpers.formatMoney(totalInventoryValue), color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Text(
                        text = "تفاصيل حركة وحالة الأصناف بالفترة:",
                        color = PalWhiteSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
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
                            itemReportRows.forEach { (item, sold, bought) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(PalBlackNormal, RoundedCornerShape(4.dp))
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(item.name, color = PalWhitePure, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("سعر الشراء: ${Helpers.formatMoney(item.buyPrice)}", color = PalWhiteMuted, fontSize = 9.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                        Text("مباعة: ${sold.toInt()}", color = PalGreenLight, fontSize = 9.sp)
                                        Text("موردة: ${bought.toInt()}", color = PalGoldCalligraphy, fontSize = 9.sp)
                                    }
                                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                                        Text("المخزون: ${item.quantity.toInt()} حبة", color = if (item.quantity <= item.lowStockThreshold) PalRedLight else PalWhiteSoft, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val reportMsg = buildString {
                        appendLine("وكالة عاهد الصبري للمبيعات والقات الماوية")
                        appendLine("تقرير حركة وجرد المخزون والعمليات")
                        appendLine("الفترة: $selectedReportPeriod")
                        appendLine("التاريخ: ${Helpers.getCurrentDateTime()}")
                        appendLine("--------------------------------")
                        appendLine("الصنف        مباع   مورد   المخزن")
                        appendLine("--------------------------------")
                        itemReportRows.forEach { (item, sold, bought) ->
                            val cleanName = if (item.name.length > 10) item.name.substring(0, 9) else item.name
                            appendLine("${cleanName.padEnd(12)} ${sold.toInt().toString().padEnd(6)} ${bought.toInt().toString().padEnd(6)} ${item.quantity.toInt().toString()}")
                        }
                        appendLine("--------------------------------")
                        appendLine("👈 إجمالي الوحدات المباعة بالفترة: ${totalUnitsSold.toInt()}")
                        appendLine("👈 إجمالي الوحدات الموردة بالفترة: ${totalUnitsBought.toInt()}")
                        appendLine("💰 إجمالي قيمة المخزون الحالية: ${Helpers.formatMoney(totalInventoryValue)}")
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
                                val headers = listOf("الصنف", "تاريخ الإضافة", "المخزون الحالي", "مباع بالفترة", "مورد بالفترة", "سعر الشراء")
                                val rows = itemReportRows.map { (item, sold, bought) ->
                                    listOf(item.name, item.dateAdded.ifEmpty { "غير محدد" }, "${item.quantity.toInt()} حبة", "${sold.toInt()} حبة", "${bought.toInt()} حبة", Helpers.formatMoney(item.buyPrice))
                                }
                                Helpers.generatePdfAndShare(
                                    context = context,
                                    title = "التقرير $tPeriodStr لحركة مخزن وكالة عاهد الصبري",
                                    headers = headers,
                                    rows = rows,
                                    totals = mapOf(
                                        "إجمالي الوحدات المباعة" to "${totalUnitsSold.toInt()} حبة",
                                        "إجمالي الوحدات الموردة" to "${totalUnitsBought.toInt()} حبة",
                                        "القيمة الكلية التقديرية للمخزون" to Helpers.formatMoney(totalInventoryValue)
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

    if (showMatchingDialog) {
        var mathSearchQuery by remember { mutableStateOf("") }
        val mathFilteredItems = allItems.filter {
            mathSearchQuery.trim().isEmpty() || it.name.contains(mathSearchQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showMatchingDialog = false },
            title = {
                Text(
                    text = "مطابقة الجرد الفعلي ومقارنة الفروقات ⚖️",
                    color = PalWhitePure,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "أدخل الأعداد والمجاميع المادية لتحديد نسبة الانحراف والحيود وتحديث الفروق فورياً باللون المناسب:",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mathSearchQuery,
                        onValueChange = { mathSearchQuery = it },
                        placeholder = { Text("البحث السريع في أصناف المطابقة...", color = PalWhiteMuted, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PalWhiteMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = matchingReportDate,
                        onValueChange = { matchingReportDate = it },
                        label = { Text("تخصيص تاريخ تقرير مطابقة الجرد المطبوع", color = PalWhiteMuted, fontSize = 10.sp) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PalGreenLight) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Divider(color = PalBlackLight)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(PalBlackDark, RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            mathFilteredItems.forEach { item ->
                                val physicalValue = physicalCounts[item.id] ?: ""
                                val parsedPhysical = physicalValue.toDoubleOrNull()
                                val diff = if (parsedPhysical != null) parsedPhysical - item.quantity else 0.0

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                                    border = BorderStroke(0.5.dp, PalBlackLight),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(item.name, color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("الدفترية: ${item.quantity.toInt()} حبة", color = PalWhiteMuted, fontSize = 10.sp)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = physicalValue,
                                                onValueChange = { newVal ->
                                                    physicalCounts = physicalCounts.toMutableMap().apply {
                                                        put(item.id, newVal)
                                                    }
                                                },
                                                placeholder = { Text("الكمية الفعلية", color = PalWhiteMuted, fontSize = 11.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PalGreenLight,
                                                    unfocusedBorderColor = PalBlackLight,
                                                    focusedTextColor = PalWhitePure,
                                                    unfocusedTextColor = PalWhiteSoft
                                                ),
                                                singleLine = true,
                                                modifier = Modifier.width(110.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            )

                                            Spacer(modifier = Modifier.weight(1f))

                                            if (physicalValue.isNotEmpty() && parsedPhysical != null) {
                                                val diffColor = if (diff >= 0.0) PalGreenLight else PalRedLight
                                                val diffText = if (diff >= 0.0) "+${diff.toInt()}" else "${diff.toInt()}"
                                                Text(
                                                    text = "الفروق: $diffText حبة",
                                                    color = diffColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            } else {
                                                Text(
                                                    text = "بانتظار الجرد",
                                                    color = PalWhiteMuted,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom report builders
                    val compileMatchingReport: () -> String = {
                        buildString {
                            appendLine("📊 وكالة عاهد الصبري للقات والعموميات")
                            appendLine("📐 كشف مطابقة الجرد الفعلي والانحرافات")
                            appendLine("📅 تاريخ ونشاط الجرد: $matchingReportDate")
                            appendLine("👤 صاحب الوكالة: عاهد الصبري")
                            appendLine("----------------------------------------")
                            appendLine("الصنف          الدفترية    الفعلية   الانحراف")
                            appendLine("----------------------------------------")
                            var surplusCount = 0
                            var deficitCount = 0
                            allItems.forEach { item ->
                                val physVal = physicalCounts[item.id] ?: ""
                                val parsedPhys = physVal.toDoubleOrNull()
                                if (parsedPhys != null) {
                                    val diff = parsedPhys - item.quantity
                                    val diffText = if (diff >= 0) "+${diff.toInt()}" else "${diff.toInt()}"
                                    if (diff > 0) surplusCount += diff.toInt()
                                    if (diff < 0) deficitCount += diff.toInt()
                                    val cleanName = if (item.name.length > 12) item.name.substring(0, 11) else item.name
                                    appendLine("${cleanName.padEnd(14)} ${item.quantity.toInt().toString().padEnd(10)} ${parsedPhys.toInt().toString().padEnd(10)} $diffText")
                                }
                            }
                            appendLine("----------------------------------------")
                            appendLine("🟢 إجمالي الفائض المادي: +$surplusCount حبة")
                            appendLine("🔴 إجمالي العجز المادي: $deficitCount حبة")
                            appendLine("----------------------------------------")
                            appendLine("نظام وكالة عاهد الصبري المحاسبي الذكي")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val reportString = compileMatchingReport()
                                val headers = listOf("الصنف", "الكمية الدفترية", "الكمية الفعلية", "الحيود والفروقات")
                                val rows = allItems.mapNotNull { item ->
                                    val physVal = physicalCounts[item.id] ?: ""
                                    val parsedPhys = physVal.toDoubleOrNull()
                                    if (parsedPhys != null) {
                                        val diff = parsedPhys - item.quantity
                                        val diffText = if (diff >= 0) "+${diff.toInt()}" else "${diff.toInt()}"
                                        listOf(item.name, "${item.quantity.toInt()} حبة", "${parsedPhys.toInt()} حبة", "$diffText حبة")
                                    } else null
                                }
                                Helpers.generatePdfAndShare(
                                    context = context,
                                    title = "كشف فروقات وبنود مطابقة جرد المخزن",
                                    headers = headers,
                                    rows = rows,
                                    totals = mapOf("تاريخ وموثوقية الجرد" to matchingReportDate),
                                    customDate = matchingReportDate
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("PDF تصدير", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val reportString = compileMatchingReport()
                                Helpers.shareViaWhatsApp(context, "", reportString)
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
                                val reportString = compileMatchingReport()
                                bluetoothPrintText = reportString
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
                TextButton(onClick = { showMatchingDialog = false }) {
                    Text("إغلاق وإتمام", color = PalGreenLight, fontWeight = FontWeight.Bold)
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
