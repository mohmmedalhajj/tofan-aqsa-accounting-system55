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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.components.UnifiedPeriodSelector
import com.example.ui.theme.*
import com.example.util.Helpers
import androidx.compose.ui.platform.testTag

data class TimelineOperation(
    val type: String,
    val docId: String,
    val date: String,
    val title: String,
    val details: String,
    val amount: Double,
    val isInflow: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val invoices by viewModel.allInvoices.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val taxHistory by viewModel.allTaxHistory.collectAsState()
    val purchases by viewModel.allPurchases.collectAsState()
    val transfers by viewModel.allTransfers.collectAsState()
    val allPurchaseItems by viewModel.allPurchaseItems.collectAsState()
    val allInvoiceItems by viewModel.allInvoiceItems.collectAsState()

    var showBluetoothPrintTrigger by remember { mutableStateOf(false) }
    var bluetoothPrintText by remember { mutableStateOf("") }

    var selectedPeriod by remember { mutableStateOf(0) } // 0: Daily, 1: Weekly, 2: Monthly, 3: Yearly, 4: Archive (All Time), 5: Custom
    var customReportsType by remember { mutableStateOf("يوم") }
    var customReportsValue by remember { mutableStateOf(Helpers.getCurrentDate()) }
    var reportsReportDate by remember { mutableStateOf(Helpers.getCurrentDateTime()) }

    val todayStr = Helpers.getCurrentDate()
    val curMonthStr = todayStr.substring(0, 7)
    val curYearStr = todayStr.substring(0, 4)

    val dateFilter = when (selectedPeriod) {
        0 -> todayStr
        1 -> "7 أيام الماضية"
        2 -> curMonthStr
        3 -> curYearStr
        5 -> "$customReportsType : $customReportsValue"
        else -> "جميع الأوقات"
    }

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

    val matchesCustomFilter: (String) -> Boolean = { dateStr ->
        val dateOnly = dateStr.take(10)
        when (customReportsType) {
            "يوم" -> dateOnly == customReportsValue
            "شهر" -> dateOnly.startsWith(customReportsValue)
            "سنة" -> dateOnly.startsWith(customReportsValue)
            else -> true
        }
    }

    val filteredInvoices = when (selectedPeriod) {
        0 -> invoices.filter { it.date.startsWith(todayStr) }
        1 -> invoices.filter { isWeekly(it.date) }
        2 -> invoices.filter { it.date.startsWith(curMonthStr) }
        3 -> invoices.filter { it.date.startsWith(curYearStr) }
        5 -> invoices.filter { matchesCustomFilter(it.date) }
        else -> invoices
    }
    val filteredExpenses = when (selectedPeriod) {
        0 -> expenses.filter { it.date.startsWith(todayStr) }
        1 -> expenses.filter { isWeekly(it.date) }
        2 -> expenses.filter { it.date.startsWith(curMonthStr) }
        3 -> expenses.filter { it.date.startsWith(curYearStr) }
        5 -> expenses.filter { matchesCustomFilter(it.date) }
        else -> expenses
    }
    val filteredPurchases = when (selectedPeriod) {
        0 -> purchases.filter { it.date.startsWith(todayStr) }
        1 -> purchases.filter { isWeekly(it.date) }
        2 -> purchases.filter { it.date.startsWith(curMonthStr) }
        3 -> purchases.filter { it.date.startsWith(curYearStr) }
        5 -> purchases.filter { matchesCustomFilter(it.date) }
        else -> purchases
    }
    val filteredTaxHistory = when (selectedPeriod) {
        0 -> taxHistory.filter { it.date.startsWith(todayStr) }
        1 -> taxHistory.filter { isWeekly(it.date) }
        2 -> taxHistory.filter { it.date.startsWith(curMonthStr) }
        3 -> taxHistory.filter { it.date.startsWith(curYearStr) }
        5 -> taxHistory.filter { matchesCustomFilter(it.date) }
        else -> taxHistory
    }
    val filteredTransfers = when (selectedPeriod) {
        0 -> transfers.filter { it.date.startsWith(todayStr) }
        1 -> transfers.filter { isWeekly(it.date) }
        2 -> transfers.filter { it.date.startsWith(curMonthStr) }
        3 -> transfers.filter { it.date.startsWith(curYearStr) }
        5 -> transfers.filter { matchesCustomFilter(it.date) }
        else -> transfers
    }

    val totalSales = filteredInvoices.sumOf { it.totalAmount }
    val totalDisc = filteredInvoices.sumOf { it.discount }
    val totalExpenses = filteredExpenses.sumOf { it.amount }
    val totalPurchases = filteredPurchases.sumOf { it.totalAmount }
    val totalPurchasesPaid = filteredPurchases.sumOf { it.paidAmount }
    val totalTaxAmount = filteredTaxHistory.sumOf { it.taxAmount }
    val totalStallOutflow = filteredTaxHistory.sumOf { it.stallOutflow }
    val totalLaborOutflow = filteredTaxHistory.sumOf { it.laborOutflow }
    val totalTransferred = filteredTransfers.sumOf { it.amount }

    val customerPayments by viewModel.allPayments.collectAsState()
    val filteredCustomerPayments = when (selectedPeriod) {
        0 -> customerPayments.filter { it.date.startsWith(todayStr) }
        1 -> customerPayments.filter { isWeekly(it.date) }
        2 -> customerPayments.filter { it.date.startsWith(curMonthStr) }
        3 -> customerPayments.filter { it.date.startsWith(curYearStr) }
        5 -> customerPayments.filter { matchesCustomFilter(it.date) }
        else -> customerPayments
    }
    val totalCollections = filteredCustomerPayments.sumOf { it.amount }

    val totalCashReceived = filteredInvoices.filter { it.paymentMethod == "نقداً" || it.paymentMethod == "دفع جزئي" }.sumOf { it.paidAmount }
    val totalTransferReceived = filteredInvoices.filter { it.paymentMethod == "تحويل" }.sumOf { it.paidAmount }
    val totalDepositReceived = filteredInvoices.filter { it.paymentMethod == "إيداع" }.sumOf { it.paidAmount }
    val totalCreditSales = filteredInvoices.filter { it.paymentMethod == "آجل" }.sumOf { it.totalAmount }
    val totalNewDebts = filteredInvoices.sumOf { it.debtAmount }

    val salesInflowCash = totalCashReceived + totalTransferReceived + totalDepositReceived
    
    // Net Actual Cash = Inflow Cash - Outflows
    val netActualCash = (salesInflowCash + totalCollections) - (totalPurchasesPaid + totalExpenses + totalTaxAmount + totalStallOutflow + totalLaborOutflow + totalTransferred)

    // Net Actual Profit = Revenue Inflows - Realized Costs
    val netActualProfit = (salesInflowCash + totalCollections) - (totalPurchasesPaid + totalExpenses + totalTaxAmount + totalStallOutflow + totalLaborOutflow)
    val netProfit = netActualProfit

    val timelineOps = remember(filteredInvoices, filteredExpenses, filteredPurchases, filteredTransfers, filteredTaxHistory, allPurchaseItems, allInvoiceItems) {
        val list = mutableListOf<TimelineOperation>()
        
        filteredInvoices.forEach { inv ->
            val items = allInvoiceItems.filter { it.invoiceId == inv.id }
            val itemsDetail = if (items.isNotEmpty()) {
                "الأصناف: " + items.joinToString(", ") { "${it.itemName} (${it.quantity.toInt()} حبة x ${Helpers.formatMoney(it.unitPrice)})" }
            } else ""
            val fullDetails = "طريقة الدفع: ${inv.paymentMethod} • المدفوع: ${Helpers.formatMoney(inv.paidAmount)} • المتبقي: ${Helpers.formatMoney(inv.debtAmount)}" + if (itemsDetail.isNotEmpty()) " • $itemsDetail" else ""

            list.add(
                TimelineOperation(
                    type = "مبيعات",
                    docId = "فاتورة مبيعات #${inv.id}",
                    date = inv.date,
                    title = "فاتورة بيع للزبون: ${inv.customerName}",
                    details = fullDetails,
                    amount = inv.totalAmount,
                    isInflow = true
                )
            )
        }
        
        filteredPurchases.forEach { pur ->
            val items = allPurchaseItems.filter { it.purchaseId == pur.id }
            val itemsDetail = if (items.isNotEmpty()) {
                "الأصناف: " + items.joinToString(", ") { "${it.itemName} (${it.quantity.toInt()} حبة x ${Helpers.formatMoney(it.unitPrice)})" }
            } else ""
            val fullDetails = "طريقة الدفع: ${pur.paymentMethod} • المدفوع: ${Helpers.formatMoney(pur.paidAmount)} • المتبقي: ${Helpers.formatMoney(pur.debtRemaining)}" + if (itemsDetail.isNotEmpty()) " • $itemsDetail" else ""

            list.add(
                TimelineOperation(
                    type = "مشتريات",
                    docId = "أمر توريد #${pur.id}",
                    date = pur.date,
                    title = "توريد بضاعة من المورد",
                    details = fullDetails,
                    amount = pur.totalAmount,
                    isInflow = false
                )
            )
        }
        
        filteredExpenses.forEach { exp ->
            list.add(
                TimelineOperation(
                    type = "مصروفات",
                    docId = "سند صرف #${exp.id}",
                    date = exp.date,
                    title = "مصاريف تشغيلية: ${exp.category}",
                    details = exp.notes.ifEmpty { "مصاريف عامة" },
                    amount = exp.amount,
                    isInflow = false
                )
            )
        }
        
        filteredTransfers.forEach { tr ->
            list.add(
                TimelineOperation(
                    type = "حوالة",
                    docId = "حوالة مالية #${tr.id}",
                    date = tr.date,
                    title = "تحويل مالي من [${tr.sender}] ➔ إلى [${tr.receiver}]",
                    details = "مرجع: ${tr.referenceNumber.ifEmpty { "لا يوجد" }} • البيان: ${tr.statement}",
                    amount = tr.amount,
                    isInflow = false
                )
            )
        }
        
        filteredTaxHistory.forEach { tax ->
            list.add(
                TimelineOperation(
                    type = "رسوم",
                    docId = "رسوم يومية بتاريخ ${tax.date}",
                    date = tax.date,
                    title = "سجل التكلفة والرسوم والضرائب اليومية",
                    details = "ضريبة القات: ${Helpers.formatMoney(tax.taxAmount)} • خرج المفرش: ${Helpers.formatMoney(tax.stallOutflow)} • خرج العمال: ${Helpers.formatMoney(tax.laborOutflow)}",
                    amount = tax.taxAmount + tax.stallOutflow + tax.laborOutflow,
                    isInflow = false
                )
            )
        }
        
        list.sortByDescending { it.date }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "التقارير المالية والتحليل الإحصائي",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Institutional Brand Title Card
            Card(
                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                border = BorderStroke(1.dp, PalGreenDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "وكالة عاهد الصبري",
                        color = PalGoldCalligraphy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "صاحب الوكالة / عاهد الصبري",
                        color = PalWhitePure,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Tabs to select filter
            ScrollableTabRow(
                selectedTabIndex = selectedPeriod,
                containerColor = PalBlackNormal,
                contentColor = PalGreenLight,
                edgePadding = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Tab(selected = selectedPeriod == 0, onClick = { selectedPeriod = 0 }, text = { Text("اليومية", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedPeriod == 1, onClick = { selectedPeriod = 1 }, text = { Text("أسبوعية", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedPeriod == 2, onClick = { selectedPeriod = 2 }, text = { Text("الشهرية", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedPeriod == 3, onClick = { selectedPeriod = 3 }, text = { Text("السنوية", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedPeriod == 4, onClick = { selectedPeriod = 4 }, text = { Text("الأرشيف", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedPeriod == 5, onClick = { selectedPeriod = 5 }, text = { Text("تقويم مخصص 📆", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PalGoldCalligraphy) })
            }

            if (selectedPeriod == 5) {
                UnifiedPeriodSelector(
                    initialType = customReportsType,
                    initialValue = customReportsValue,
                    onPeriodChanged = { type, finalVal ->
                        customReportsType = type
                        customReportsValue = finalVal
                        reportsReportDate = finalVal
                    }
                )
            }

            val periodLabel = when (selectedPeriod) {
                0 -> "تقرير أداء اليوم ($dateFilter)"
                1 -> "تقرير أداء الأسبوع الحالي ($dateFilter)"
                2 -> "تقرير أداء الشهر ($dateFilter)"
                3 -> "تقرير أداء السنة ($dateFilter)"
                5 -> "تقرير أداء الفترة المخصصة ($dateFilter)"
                else -> "تقرير الأداء المالي والأرشيف العام (شامل ماليًا)"
            }

            Text(
                text = periodLabel,
                color = PalWhitePure,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            // Performance Card Visuals
            Card(
                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, Color(0xFF222222))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Headline Net Cash and Realized Profit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "صافي النقدية الفعلية (الخزينة):",
                            color = PalWhitePure,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = Helpers.formatMoney(netActualCash),
                            color = if (netActualCash >= 0) PalGreenLight else PalRedLight,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "صافي الربح الفعلي المالي 💰:",
                            color = PalWhitePure,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = Helpers.formatMoney(netActualProfit),
                            color = PalGoldCalligraphy,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }

                    Divider(color = Color(0xFF222222), thickness = 1.dp)

                    Text(
                        text = "تفاصيل الإيرادات والمقبوضات (الداخلة):",
                        color = PalGreenLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    ReportBreakdownRow(label = "إجمالي المبيعات الكلية بالفترة (+)", amount = totalSales, isNegative = false)
                    ReportBreakdownRow(label = "👈 المقبوض نقداً (مبيعات كاش)", amount = totalCashReceived, isNegative = false, tintColor = PalWhiteMuted)
                    ReportBreakdownRow(label = "👈 المقبوض بنظام تحويل بنكي", amount = totalTransferReceived, isNegative = false, tintColor = PalWhiteMuted)
                    ReportBreakdownRow(label = "👈 المقبوض بنظام إيداع صرافة", amount = totalDepositReceived, isNegative = false, tintColor = PalWhiteMuted)
                    ReportBreakdownRow(label = "👈 مبيعات كليّة آجلة (على الحساب)", amount = totalCreditSales, isNegative = false, tintColor = Color.Gray)
                    ReportBreakdownRow(label = "إجمالي الديون الجديدة المسجلة (-)", amount = totalNewDebts, isNegative = true, tintColor = PalRedLight)
                    ReportBreakdownRow(label = "إجمالي تحصيلات ديون الزبائن (+)", amount = totalCollections, isNegative = false, tintColor = PalGoldCalligraphy)

                    Divider(color = Color(0xFF222222), thickness = 0.5.dp)

                    Text(
                        text = "تفاصيل المدفوعات والخرجات (الخارجة):",
                        color = PalRedLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    ReportBreakdownRow(label = "إجمالي مدفوعات المشتريات والموردين", amount = totalPurchasesPaid, isNegative = true)
                    ReportBreakdownRow(label = "ضرائب ورسوم القات المسددة", amount = totalTaxAmount, isNegative = true)
                    ReportBreakdownRow(label = "خرج المفرش وعمال اليومية", amount = totalStallOutflow + totalLaborOutflow, isNegative = true)
                    ReportBreakdownRow(label = "مصاريف تشغيلية وعامة أخرى", amount = totalExpenses, isNegative = true)
                    ReportBreakdownRow(label = "حركات الحوالات المالية الصادرة", amount = totalTransferred, isNegative = true, tintColor = PalWhiteSoft)
                }
            }

            // Real-time Canvas Chart Visualization
            Text(
                text = "مقارنة نسب الأداء المالي والتحليلي",
                color = PalWhiteSoft,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                border = BorderStroke(0.5.dp, Color(0xFF222222))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val maxVal = maxOf(totalSales, totalPurchases, totalTaxAmount + totalStallOutflow + totalLaborOutflow + totalExpenses, 1.0)
                    
                    ChartBarItem(label = "إيراد المبيعات", amount = totalSales, maxAmount = maxVal, color = PalGreenLight)
                    ChartBarItem(label = "قيمة المشتريات", amount = totalPurchases, maxAmount = maxVal, color = PalRedLight)
                    ChartBarItem(label = "مجموع الرسوم اليومية", amount = totalTaxAmount + totalStallOutflow + totalLaborOutflow, maxAmount = maxVal, color = PalGoldCalligraphy)
                    ChartBarItem(label = "مصروفات تشغيلية", amount = totalExpenses, maxAmount = maxVal, color = Color(0xFF42A5F5))
                }
            }

            // Shared Document / Message Formulation
            val shareTitle = when (selectedPeriod) {
                0 -> "تقرير اليوم المالي التفصيلي"
                1 -> "تقرير الأسبوع المالي التفصيلي"
                2 -> "تقرير الشهر المالي التفصيلي"
                3 -> "تقرير السنة المالي التفصيلي"
                else -> "تقرير الأداء المالي والأرشيف العام الشامل"
            }
            
            val archiveMsg = buildString {
                appendLine("📝 وكالة عاهد الصبري للقات الماوية والورزاني بجميع أنواعها")
                appendLine("👤 صاحب الوكالة: عاهد الصبري")
                appendLine("📊 الأرشيف والسجل المالي التفصيلي ($shareTitle)")
                appendLine("📅 تاريخ التصدير: $reportsReportDate")
                appendLine("========================================")
                appendLine("📊 الحسابات والملخص المالي للفترة:")
                appendLine("• إجمالي المبيعات الكلية بالفترة (+): ${Helpers.formatMoney(totalSales)}")
                appendLine("• إجمالي المقبوض نقداً (كاش): ${Helpers.formatMoney(totalCashReceived)}")
                appendLine("• إجمالي المقبوض عبر تحويل: ${Helpers.formatMoney(totalTransferReceived)}")
                appendLine("• إجمالي المقبوض عبر إيداع: ${Helpers.formatMoney(totalDepositReceived)}")
                appendLine("• إجمالي المبيعات الآجلة (على الحساب): ${Helpers.formatMoney(totalCreditSales)}")
                appendLine("• إجمالي الديون الجديدة المسجلة (-): ${Helpers.formatMoney(totalNewDebts)}")
                appendLine("• إجمالي تحصيلات ديون الزبائن (+): ${Helpers.formatMoney(totalCollections)}")
                appendLine("• إجمالي مدفوعات المشتريات للموردين (-): ${Helpers.formatMoney(totalPurchasesPaid)}")
                appendLine("• ضرائب ورسوم القات المسددة (-): ${Helpers.formatMoney(totalTaxAmount)}")
                appendLine("• خرج المفرش وعمال اليومية (-): ${Helpers.formatMoney(totalStallOutflow + totalLaborOutflow)}")
                appendLine("• إجمالي المصروفات العامة (-): ${Helpers.formatMoney(totalExpenses)}")
                appendLine("• حركات الحوالات المالية الصادرات (-): ${Helpers.formatMoney(totalTransferred)}")
                appendLine("----------------------------------------")
                appendLine("• صافي النقدية الفعلية بالخزينة: ${Helpers.formatMoney(netActualCash)}")
                appendLine("• صافي الربح الفعلي المالي 💰: ${Helpers.formatMoney(netActualProfit)}")
                appendLine("============ السجل التفصيلي لجميع العمليات ============")
                if (timelineOps.isEmpty()) {
                    appendLine("لا توجد أي عمليات مالية مقيدة لهذه الفترة الزمنية.")
                } else {
                    timelineOps.forEachIndexed { idx, op ->
                        appendLine("${idx + 1}. [${op.type}] - ${op.docId}")
                        appendLine("   • التاريخ والوقت: ${op.date}")
                        appendLine("   • التفاصيل: ${op.title}")
                        appendLine("   • القيود: ${op.details}")
                        val sign = if (op.isInflow) "+" else "-"
                        appendLine("   • المبلغ الحركي: $sign ${Helpers.formatMoney(op.amount)}")
                        appendLine("-------------------------")
                    }
                }
                appendLine("نظام وكالة عاهد الصبري المحاسبي المتكامل")
            }

            // Report Export Section Header
            Text(
                text = "تصدير ومشاركة تقارير الأرشيف المالي 📤",
                color = PalWhiteSoft,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Custom Report Print Date Input Field
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = reportsReportDate,
                onValueChange = { reportsReportDate = it },
                label = { Text("تعيين وتخصيص تاريخ التقرير المطبوع", color = PalWhiteMuted, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth().testTag("reports_custom_date_input"),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Export Actions buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PDF Button
                Button(
                    onClick = {
                        val headers = listOf("التاريخ", "رقم السند", "البيان والتفاصيل", "القيمة المالية")
                        val rows = timelineOps.map { op ->
                            listOf(
                                op.date.split(" ")[0],
                                "#${op.docId}",
                                "${op.title} - ${op.details}",
                                "${if (op.isInflow) "تحصيل +" else "صرف -"} ${Helpers.formatMoney(op.amount)}"
                            )
                        }
                        Helpers.generatePdfAndShare(
                            context = context,
                            title = shareTitle,
                            headers = headers,
                            rows = rows,
                            totals = mapOf(
                                "إجمالي المبيعات الكلية" to Helpers.formatMoney(totalSales),
                                "صـافي النقـدية الفعليـة" to Helpers.formatMoney(netActualCash),
                                "صـافي الـربح الفعـلي المحقق" to Helpers.formatMoney(netActualProfit)
                            ),
                            customDate = reportsReportDate,
                            reportPeriod = dateFilter
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("سجل PDF 📄", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // WhatsApp Button
                Button(
                    onClick = {
                        Helpers.shareViaWhatsApp(context, "", archiveMsg)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("واتساب 💬", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Bluetooth Print Button
                Button(
                    onClick = {
                        bluetoothPrintText = archiveMsg
                        showBluetoothPrintTrigger = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PalGoldCalligraphy),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("طباعة حرارية 🖨️", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Divider(color = Color(0xFF222222), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            // Timeline Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل العمليات والقيود ($shareTitle)",
                    color = PalWhiteSoft,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "العدد: (${timelineOps.size})",
                    color = PalWhiteMuted,
                    fontSize = 12.sp
                )
            }

            // Timeline List of elements
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (timelineOps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد عمليات مسجلة تحت هذا النطاق الزمني",
                            color = PalWhiteMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    timelineOps.forEach { op ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                            border = BorderStroke(0.5.dp, Color(0xFF222222)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Colored badge for the operation type
                                        val badgeBg = when (op.type) {
                                            "مبيعات" -> PalGreenDark
                                            "مشتريات" -> PalRedNormal
                                            "رسوم" -> PalGoldCalligraphy
                                            "مصروفات" -> Color(0xFFE57373)
                                            "حوالة" -> Color(0xFF1E88E5)
                                            else -> Color.Gray
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(badgeBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(op.type, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(op.docId, color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    
                                    val amountColor = if (op.isInflow) PalGreenLight else PalRedLight
                                    val sign = if (op.isInflow) "+" else "-"
                                    Text(
                                        text = "$sign ${Helpers.formatMoney(op.amount)}",
                                        color = amountColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Text(text = op.title, color = PalWhiteSoft, fontSize = 12.sp)
                                Text(text = op.details, color = PalWhiteMuted, fontSize = 11.sp)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "📅 ${op.date}",
                                        color = PalWhiteMuted,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Left
                                    )
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ReportBreakdownRow(
    label: String,
    amount: Double,
    isNegative: Boolean,
    tintColor: Color = PalWhiteSoft
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = PalWhiteMuted, fontSize = 12.sp)
        val showText = if (isNegative) "- ${Helpers.formatMoney(amount)}" else Helpers.formatMoney(amount)
        val color = if (isNegative && amount > 0) PalRedLight else if (!isNegative && amount > 0) PalGreenLight else tintColor
        Text(text = showText, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun ChartBarItem(
    label: String,
    amount: Double,
    maxAmount: Double,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = PalWhiteSoft, fontSize = 11.sp)
            Text(text = Helpers.formatMoney(amount), color = PalWhiteMuted, fontSize = 11.sp)
        }
        
        val fraction = if (maxAmount <= 0.0 || amount.isNaN() || maxAmount.isNaN()) {
            0.0f
        } else {
            (amount / maxAmount).coerceIn(0.0, 1.0).toFloat()
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF222222))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(color)
            )
        }
    }
}
