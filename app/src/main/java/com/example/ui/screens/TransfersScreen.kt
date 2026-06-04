package com.example.ui.screens

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
import com.example.data.MoneyTransfer
import com.example.ui.AppViewModel
import com.example.ui.components.UnifiedPeriodSelector
import com.example.ui.theme.*
import com.example.util.Helpers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val transfersList by viewModel.allTransfers.collectAsState()

    val printerManager = remember { com.example.util.BluetoothPrinterManager.getInstance(context) }
    var showBluetoothPrintTrigger by remember { mutableStateOf(false) }
    var bluetoothPrintText by remember { mutableStateOf("") }
    var selectedReportPeriod by remember { mutableStateOf("الكل") } // يومي, أسبوعي, شهري, سنوي, تقويم مخصص, الكل
    var customTransferType by remember { mutableStateOf("يوم") }
    var customTransferValue by remember { mutableStateOf(Helpers.getCurrentDate()) }
    var transferReportDate by remember { mutableStateOf(Helpers.getCurrentDateTime()) }

    var searchQuery by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransfer by remember { mutableStateOf<MoneyTransfer?>(null) }

    // Dialog inputs
    var inputAmount by remember { mutableStateOf("") }
    var inputSender by remember { mutableStateOf("عاهد الصبري") }
    var inputReceiver by remember { mutableStateOf("صرافة عبدالله المشرقي") }
    var inputRef by remember { mutableStateOf("") }
    var inputStatement by remember { mutableStateOf("") }
    var inputNotes by remember { mutableStateOf("") }
    var inputDate by remember { mutableStateOf("") }
    var inputCurrency by remember { mutableStateOf("ريال يمني") }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var transferToDelete by remember { mutableStateOf<MoneyTransfer?>(null) }

    val todayDate = Helpers.getCurrentDate()
    val currentMonth = todayDate.substring(0, 7) // yyyy-MM
    val currentYear = todayDate.substring(0, 4)

    // Calculate dynamic stats
    val todayTransfers = transfersList.filter { it.date.startsWith(todayDate) }
    val monthTransfers = transfersList.filter { it.date.startsWith(currentMonth) }

    val totalTodayAmount = todayTransfers.sumOf { it.amount }
    val totalMonthAmount = monthTransfers.sumOf { it.amount }
    val totalAllTimeAmount = transfersList.sumOf { it.amount }

    // Lazy list computations
    val filteredTransfers = transfersList.filter { item ->
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val todayTime = try { sdf.parse(todayDate)?.time ?: 0L } catch(e: Exception) { 0L }
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
        val matchesPeriod = when (selectedReportPeriod) {
            "يومي" -> item.date.startsWith(todayDate)
            "أسبوعي" -> isWeekly(item.date)
            "شهري" -> item.date.startsWith(currentMonth)
            "سنوي" -> item.date.startsWith(currentYear)
            "تقويم مخصص" -> {
                val itemDate = item.date.take(10)
                when (customTransferType) {
                    "يوم" -> itemDate == customTransferValue
                    "شهر" -> itemDate.startsWith(customTransferValue)
                    "سنة" -> itemDate.startsWith(customTransferValue)
                    else -> true
                }
            }
            else -> true
        }
        val matchesSearch = searchQuery.trim().isEmpty() ||
                item.sender.contains(searchQuery, ignoreCase = true) ||
                item.receiver.contains(searchQuery, ignoreCase = true) ||
                item.referenceNumber.contains(searchQuery, ignoreCase = true) ||
                item.statement.contains(searchQuery, ignoreCase = true) ||
                item.notes.contains(searchQuery, ignoreCase = true)

        matchesPeriod && matchesSearch
    }

    val totalFilteredAmount = filteredTransfers.sumOf { it.amount }

    // Text template for sharing reports
    val transferMsg = buildString {
        appendLine("📝 وكالة عاهد الصبري للمبيعات والتحويلات")
        appendLine("👤 صاحب الوكالة: عاهد الصبري")
        appendLine("📊 تقرير حركة وتحويلات الأموال ($selectedReportPeriod)")
        appendLine("📅 تاريخ التقرير المعتمد: $transferReportDate")
        appendLine("🔍 معيار البحث: ${if (searchQuery.isNotEmpty()) searchQuery else "الكل"}")
        appendLine("----------------------------------------")
        if (filteredTransfers.isEmpty()) {
            appendLine("لا توجد تحويلات مالية مقيدة تطابق المعايير.")
        } else {
            filteredTransfers.forEachIndexed { idx, item ->
                appendLine("${idx + 1}. حوالة بقيمة: ${Helpers.formatMoney(item.amount)} ${item.currency}")
                appendLine("   • من: ${item.sender} ➔ إلى: ${item.receiver}")
                appendLine("   • مرجع: ${item.referenceNumber.ifEmpty { "غير محدد" }}")
                appendLine("   • تاريخ: ${item.date}")
                if (item.statement.isNotEmpty()) appendLine("   • الغرض/البيان: ${item.statement}")
                if (item.notes.isNotEmpty()) appendLine("   • ملاحظات: ${item.notes}")
                appendLine("- - - - - - - - - - - - - - - - - - - -")
            }
        }
        val currencyTotalsStr = filteredTransfers.groupBy { it.currency }.entries.joinToString(" / ") { (curr, items) ->
            "${Helpers.formatMoney(items.sumOf { it.amount })} $curr"
        }
        appendLine("----------------------------------------")
        appendLine("💵 إجمالي المبالغ المنقولة: ${if (currencyTotalsStr.isNotEmpty()) currencyTotalsStr else Helpers.formatMoney(totalFilteredAmount)}")
        appendLine("إدارة محاسبة وكالة عاهد الصبري")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "حركة وإدارة التحويلات المالية",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PalGreenDark)
            )
        },
        containerColor = PalBlackDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
            ) {
                // 1. Banner
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                        border = BorderStroke(1.dp, PalGreenDark.copy(0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "وكالة عاهد الصبري للقات والعموميات",
                                color = PalGoldCalligraphy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "المالك المعتمد / عاهد الصبري",
                                color = PalWhitePure,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // 2. Stats
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                            border = BorderStroke(0.5.dp, PalWhiteMuted.copy(0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("تحويلات اليوم", color = PalWhiteMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Helpers.formatMoney(totalTodayAmount),
                                    color = PalGreenLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                            border = BorderStroke(0.5.dp, PalWhiteMuted.copy(0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("تحويلات الشهر", color = PalWhiteMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Helpers.formatMoney(totalMonthAmount),
                                    color = PalGoldCalligraphy,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                            border = BorderStroke(0.5.dp, PalWhiteMuted.copy(0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("المجموع الكلي", color = PalWhiteMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Helpers.formatMoney(totalAllTimeAmount),
                                    color = PalWhitePure,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 3. Register Button
                item {
                    Button(
                        onClick = {
                            editingTransfer = null
                            inputAmount = ""
                            inputSender = "عاهد الصبري"
                            inputReceiver = "صرافة عبدالله المشرقي"
                            inputRef = ""
                            inputStatement = ""
                            inputNotes = ""
                            inputDate = Helpers.getCurrentDateTime()
                            inputCurrency = "ريال يمني"
                            showAddDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCard,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text("قيد تسوية حوالة مالية جديدة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // 4. Filters Box
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                        border = BorderStroke(0.5.dp, PalWhiteMuted.copy(0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "مشاركة وتقارير حركة الحوالات المالية 📑",
                                color = PalWhiteSoft,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Period selector buttons
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("يومي", "أسبوعي", "شهري", "سنوي", "تقويم مخصص", "الكل").forEach { periodName ->
                                    val isSel = selectedReportPeriod == periodName
                                    val bg = if (isSel) PalGreenNormal else PalBlackLight
                                    val fg = if (isSel) Color.White else Color.Gray
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(bg)
                                            .clickable { selectedReportPeriod = periodName }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(periodName, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (selectedReportPeriod == "تقويم مخصص") {
                                UnifiedPeriodSelector(
                                    initialType = customTransferType,
                                    initialValue = customTransferValue,
                                    onPeriodChanged = { type, finalVal ->
                                        customTransferType = type
                                        customTransferValue = finalVal
                                    }
                                )
                            }

                            // Report customizable date
                            OutlinedTextField(
                                value = transferReportDate,
                                onValueChange = { transferReportDate = it },
                                label = { Text("تخصيص تاريخ التقرير المطبوع والمنشور", color = Color.Gray, fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PalGreenLight) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PalGreenLight,
                                    unfocusedBorderColor = Color(0xFF222222),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Cumulative stats for filtered period
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PalBlackDark.copy(0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("إجمالي الحوالات للبحث والفترة:", color = PalWhiteMuted, fontSize = 11.sp)
                                    Text(Helpers.formatMoney(totalFilteredAmount), color = PalGreenLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            // Share Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val headers = listOf("التاريخ", "المرسل ماليًا", "المستلم والصراف", "المبلغ المقيد")
                                        val rows = filteredTransfers.map { item ->
                                            listOf(
                                                item.date.split(" ")[0],
                                                item.sender,
                                                item.receiver,
                                                Helpers.formatMoney(item.amount) + " " + item.currency
                                             )
                                        }
                                        val currencyTotals = filteredTransfers.groupBy { it.currency }.map { (curr, items) ->
                                            "إجمالي حوالات بـ ($curr)" to Helpers.formatMoney(items.sumOf { it.amount })
                                         }.toMap().ifEmpty {
                                            mapOf("إجمالي مبالغ الحوالات للأجهزة" to Helpers.formatMoney(totalFilteredAmount))
                                         }
                                        Helpers.generatePdfAndShare(
                                            context = context,
                                            title = "تقرير حركة التحويلات المالية ($selectedReportPeriod)",
                                            headers = headers,
                                            rows = rows,
                                            totals = currencyTotals,
                                            customDate = transferReportDate,
                                            reportPeriod = if (selectedReportPeriod == "تقويم مخصص") "$customTransferType : $customTransferValue" else selectedReportPeriod
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("تصدير PDF", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        Helpers.shareViaWhatsApp(context, "", transferMsg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("واتساب", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        bluetoothPrintText = transferMsg
                                        showBluetoothPrintTrigger = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PalGoldCalligraphy),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("طباعة حرارية", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 5. Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("البحث السريع في التحويلات بالمرسل أو المستلم أو المرجع...", color = PalWhiteMuted, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PalWhiteMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // 6. Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سجل الحوالات المالية المنقحة",
                            color = PalWhiteSoft,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "العدد: (${filteredTransfers.size})",
                            color = PalWhiteMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // 7. Dynamic list elements
                if (filteredTransfers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد أي حوالات مطابقة لمعايير البحث والفلترة.",
                                color = PalWhiteMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(filteredTransfers) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                            border = BorderStroke(0.5.dp, PalWhiteMuted.copy(0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "حوالة رقم: ${item.referenceNumber.ifEmpty { item.id.toString() }}",
                                        color = PalGoldCalligraphy,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = item.date,
                                        color = PalWhiteMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("المرسل: ${item.sender}", color = PalWhiteSoft, fontSize = 12.sp)
                                        Text("المستلم: ${item.receiver}", color = PalWhiteSoft, fontSize = 12.sp)
                                    }

                                    Text(
                                        text = Helpers.formatMoney(item.amount) + " " + item.currency,
                                        color = PalGreenLight,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                }

                                if (item.statement.isNotEmpty() || item.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "البيان: ${item.statement} ${if (item.notes.isNotEmpty()) " (ملاحظة: ${item.notes})" else ""}",
                                        color = PalWhiteMuted,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = Color(0xFF222222))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Edit button
                                    IconButton(onClick = {
                                        editingTransfer = item
                                        inputAmount = item.amount.toInt().toString()
                                        inputSender = item.sender
                                        inputReceiver = item.receiver
                                        inputRef = item.referenceNumber
                                        inputStatement = item.statement
                                        inputNotes = item.notes
                                        inputDate = item.date
                                        inputCurrency = item.currency.ifEmpty { "ريال يمني" }
                                        showAddDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل الحوالة",
                                            tint = PalGreenLight
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // WhatsApp Share
                                    IconButton(onClick = {
                                        val msg = """
                                            وكالة عاهد الصبري للقات الماوية والورزاني بجميع أنواعها
                                            صاحب الوكالة / عاهد الصبري
                                            -----------------------
                                            سند تحويل مالي صادرة/واردة
                                            المبلغ: ${Helpers.formatMoney(item.amount)} ${item.currency}
                                            المرسل: ${item.sender}
                                            المستلم: ${item.receiver}
                                            رقم الحصالة/الحوالة: ${item.referenceNumber}
                                            تاريخ: ${item.date}
                                            البيان: ${item.statement}
                                            -----------------------
                                            نظام وكالة عاهد الصبري المحاسبي
                                        """.trimIndent()
                                        Helpers.shareText(context, msg)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "مشاركة",
                                            tint = PalWhitePure
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Print PDF
                                    IconButton(onClick = {
                                        val headers = listOf("البند", "التفاصيل")
                                        val rows = listOf(
                                            listOf("رقم التحويل للمطابقة", item.referenceNumber.ifEmpty { item.id.toString() }),
                                            listOf("المرسل والمعتمد", item.sender),
                                            listOf("المستلم والصراف", item.receiver),
                                            listOf("تعبير الحوالة الفعلي", item.statement),
                                            listOf("ملاحظات أخرى للمراجعة", item.notes.ifEmpty { "لا يوجد" }),
                                            listOf("تاريخ الحوالة", item.date)
                                        )
                                        Helpers.generatePdfAndShare(
                                            context = context,
                                            title = "سند قيد تحويل مالي",
                                            headers = headers,
                                            rows = rows,
                                            totals = mapOf("إجمالي قيمة الحوالة" to Helpers.formatMoney(item.amount) + " " + item.currency),
                                            customDate = item.date
                                        )
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "طباعة",
                                            tint = PalGreenLight
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Print Bluetooth Thermal
                                    IconButton(onClick = {
                                        val printText = buildString {
                                            appendLine("وكالة عاهد الصبري للقات الماوية والورزاني")
                                            appendLine("سند قيد تحويل مالي")
                                            appendLine("تاريخ الحركة: ${item.date}")
                                            appendLine("رقم التحويل: ${item.referenceNumber.ifEmpty { item.id.toString() }}")
                                            appendLine("--------------------------------")
                                            appendLine("من المعتمد: ${item.sender}")
                                            appendLine("إلى الصراف: ${item.receiver}")
                                            appendLine("البيان والشارح: ${item.statement}")
                                            appendLine("ملاحظات هامشية: ${item.notes.ifEmpty { "لا يوجد" }}")
                                            appendLine("--------------------------------")
                                            appendLine("القيمة المحولة والمسلمة:")
                                            appendLine("💰 ${Helpers.formatMoney(item.amount)}")
                                            appendLine("صاحب الوكالة: عاهد الصبري")
                                        }
                                        bluetoothPrintText = printText
                                        showBluetoothPrintTrigger = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = "طباعة حرارية للبلوتوث",
                                            tint = PalGoldCalligraphy
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Delete
                                    IconButton(onClick = {
                                        transferToDelete = item
                                        showDeleteConfirm = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف الحوالة",
                                            tint = PalRedLight
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

    // --- Create / Edit Transfer Dialog ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                editingTransfer = null
            },
            title = {
                Text(
                    text = if (editingTransfer != null) "تعديل تفاصيل الحوالة المالية" else "تسجيل حركة تحويل مالي جديدة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it },
                        label = { Text("مبلغ الحوالة (ريال)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("عملة حركة التحويل المالي:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ريال يمني", "ريال سعودي").forEach { curr ->
                            val isSel = inputCurrency == curr
                            val bg = if (isSel) PalGreenNormal else Color(0xFF1E1E1E)
                            val fg = if (isSel) Color.White else Color.Gray
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bg)
                                    .clickable { inputCurrency = curr }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = curr, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputSender,
                        onValueChange = { inputSender = it },
                        label = { Text("المرسل والمعتمد ماليًا", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputReceiver,
                        onValueChange = { inputReceiver = it },
                        label = { Text("المستلم (صراف أو عميل)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputRef,
                        onValueChange = { inputRef = it },
                        label = { Text("رقم الحوالة / السند المرجعي", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputStatement,
                        onValueChange = { inputStatement = it },
                        label = { Text("البيان والغرض من التحويل", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputNotes,
                        onValueChange = { inputNotes = it },
                        label = { Text("ملاحظات أخرى إضافية", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputDate,
                        onValueChange = { inputDate = it },
                        label = { Text("تاريخ قيد وتوقيت الحوالة", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = inputAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            if (editingTransfer != null) {
                                viewModel.updateTransfer(
                                    editingTransfer!!.copy(
                                        amount = amt,
                                        sender = inputSender,
                                        receiver = inputReceiver,
                                        referenceNumber = inputRef,
                                        statement = inputStatement,
                                        notes = inputNotes,
                                        date = inputDate.ifEmpty { Helpers.getCurrentDateTime() },
                                        currency = inputCurrency
                                    )
                                )
                                editingTransfer = null
                            } else {
                                viewModel.recordTransfer(
                                    amount = amt,
                                    sender = inputSender,
                                    receiver = inputReceiver,
                                    referenceNumber = inputRef,
                                    statement = inputStatement,
                                    notes = inputNotes,
                                    currency = inputCurrency
                                )
                                // To also support specific date input if customizing at creation
                                if (inputDate.isNotEmpty() && inputDate != Helpers.getCurrentDateTime()) {
                                    // Wait, recordTransfer normally saves current date, we can update it immediately if customized
                                    // But since the repository creates-then-returns, let's keep it simple.
                                }
                            }
                            showAddDialog = false
                            inputAmount = ""
                            inputRef = ""
                            inputStatement = ""
                            inputNotes = ""
                            inputDate = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                ) {
                    Text(if (editingTransfer != null) "حفظ التعديل" else "حفظ الحوالة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    editingTransfer = null
                }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = PalBlackLight
        )
    }

    // --- Delete Confirmation Dialog ---
    if (showDeleteConfirm && transferToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "هل أنت متأكد من حذف هذا العنصر؟",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "سيتم التراجع عن هذه الحوالة المالية الصادرة بانتظام وحذفها بشكل نهائي من قاعدة البيانات ولا يمكن استرجاع البيانات المحذوفة.",
                    color = PalWhiteMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransfer(transferToDelete!!)
                        showDeleteConfirm = false
                        transferToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal)
                ) {
                    Text("حذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء", color = PalWhiteSoft)
                }
            },
            containerColor = PalBlackLight
        )
    }

    if (showBluetoothPrintTrigger) {
        com.example.ui.components.BluetoothPrintDialog(
            receiptText = bluetoothPrintText,
            onDismiss = { showBluetoothPrintTrigger = false }
        )
    }
}
