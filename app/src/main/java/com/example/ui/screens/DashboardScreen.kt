package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.Screen
import com.example.data.TaxEntry
import com.example.data.InventoryItem
import com.example.ui.theme.*
import com.example.util.Helpers
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.*

// Representation of a transaction line for bento logs
data class UnifiedOperation(
    val title: String,
    val subtitle: String,
    val amount: Double,
    val isExpense: Boolean
)

@Composable
fun AgencyLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.2.dp, Color(0xFF8B7340), RoundedCornerShape(8.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = com.example.R.drawable.img_agency_logo_1780160497858),
            contentDescription = "شعار وكالة عاهد الصبري",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    var showTaxDialog by remember { mutableStateOf(false) }
    var selectedDashboardCurrency by remember { mutableStateOf("الكل") }
    
    val todayTax by viewModel.todayTaxAmount.collectAsState()
    val todayTaxEntry by viewModel.todayTaxEntry.collectAsState()
    val allItems by viewModel.inventoryItems.collectAsState()
    val invoices by viewModel.allInvoices.collectAsState()
    val purchases by viewModel.allPurchases.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val supplies by viewModel.allSuppliers.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchTodayTax()
    }

    // Calculations & Filtered values by selected currency
    val filteredInvoices = remember(invoices, selectedDashboardCurrency) {
        if (selectedDashboardCurrency == "الكل") invoices else invoices.filter {
            if (selectedDashboardCurrency == "الريال اليمني") it.currency.contains("يمني") else it.currency.contains("سعودي")
        }
    }

    val filteredPurchases = remember(purchases, selectedDashboardCurrency) {
        if (selectedDashboardCurrency == "الريال السعودي") emptyList() else purchases
    }

    val filteredExpenses = remember(expenses, selectedDashboardCurrency) {
        if (selectedDashboardCurrency == "الريال السعودي") emptyList() else expenses
    }

    val totalStockQty = allItems.sumOf { it.quantity }
    val totalStockVal = allItems.sumOf { it.quantity * it.buyPrice }
    
    val dateToday = Helpers.getCurrentDate()
    val todaySales = filteredInvoices.filter { it.date.startsWith(dateToday) }
    val todaySalesTotal = todaySales.sumOf { it.totalAmount }
    
    val todayPurchases = filteredPurchases.filter { it.date.startsWith(dateToday) }
    val todayPurchasesTotal = todayPurchases.sumOf { it.totalAmount }
    
    val totalDebtsOutstanding = remember(invoices, customers, selectedDashboardCurrency) {
        if (selectedDashboardCurrency == "الكل") {
            customers.sumOf { it.totalDebt }
        } else {
            val filterString = if (selectedDashboardCurrency == "الريال اليمني") "يمني" else "سعودي"
            invoices.filter { it.currency.contains(filterString) }.sumOf { it.debtAmount }
        }
    }

    val todayExpensesTotal = filteredExpenses.filter { it.date.startsWith(dateToday) }.sumOf { it.amount }

    // Today outflows & tax apply to Yemeni Rial only
    val todayTaxVal = if (selectedDashboardCurrency == "الريال السعودي") 0.0 else (todayTaxEntry?.taxAmount ?: 0.0)
    val todayStallOutflow = if (selectedDashboardCurrency == "الريال السعودي") 0.0 else (todayTaxEntry?.stallOutflow ?: 0.0)
    val todayLaborOutflow = if (selectedDashboardCurrency == "الريال السعودي") 0.0 else (todayTaxEntry?.laborOutflow ?: 0.0)

    val todayProfitEst = todaySalesTotal - todayPurchasesTotal - todayTaxVal - todayStallOutflow - todayLaborOutflow - todayExpensesTotal

    val currencySuffix = when (selectedDashboardCurrency) {
        "الريال السعودي" -> "ر.س"
        "الريال اليمني" -> "ر.ي"
        else -> "ريال"
    }

    // Fetch live recent operations directly from tables
    val recentOperations = remember(invoices, expenses, selectedDashboardCurrency) {
        val ops = mutableListOf<UnifiedOperation>()
        for (inv in invoices) {
            val matchesFilter = when (selectedDashboardCurrency) {
                "الريال اليمني" -> inv.currency.contains("يمني")
                "الريال السعودي" -> inv.currency.contains("سعودي")
                else -> true
            }
            if (matchesFilter) {
                ops.add(
                    UnifiedOperation(
                        title = "مبيع: " + (inv.customerName.ifEmpty { "عميل نقدي" }),
                        subtitle = "فاتورة #${inv.id} • ${inv.date.split(" ").firstOrNull() ?: ""} • ${inv.currency}",
                        amount = inv.totalAmount,
                        isExpense = false
                    )
                )
            }
        }
        if (selectedDashboardCurrency != "الريال السعودي") {
            for (exp in expenses) {
                ops.add(
                    UnifiedOperation(
                        title = "مصروف: " + exp.category,
                        subtitle = "${exp.notes.ifEmpty { "منصرف عام" }} • ${exp.date.split(" ").firstOrNull() ?: ""}",
                        amount = exp.amount,
                        isExpense = true
                    )
                )
            }
        }
        ops.take(4)
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PalBlackNormal)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AgencyLogoBadge()
                        Column {
                            Text(
                                text = "وكالة عاهد الصبري",
                                color = PalWhitePure,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "عاهد الصبري • نظام محاسبي متكامل",
                                color = PalWhiteMuted,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                    
                    // Top Bar utility connectivity indicator
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PalBlackLight, RoundedCornerShape(10.dp))
                            .border(1.dp, PalWhiteMuted.copy(0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "متصل بالشبكة المحلية",
                            tint = Color(0xFF10b981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = PalWhiteMuted.copy(0.2f), thickness = 1.dp)
            }
        },
        bottomBar = {
            BottomNavigationBar(viewModel)
        },
        containerColor = PalBlackDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            
            // Currency Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("الكل", "الريال اليمني", "الريال السعودي").forEach { currencyOption ->
                    val isSelected = selectedDashboardCurrency == currencyOption
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PalGreenNormal else PalBlackNormal)
                            .border(1.dp, if (isSelected) PalGreenLight else Color(0xFF222222), RoundedCornerShape(12.dp))
                            .clickable { selectedDashboardCurrency = currencyOption }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currencyOption,
                            color = if (isSelected) PalWhitePure else PalWhiteMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Bento Tax & Outflows Status Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Tax
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (todayTaxVal > 0) Color(0xFF052e16).copy(0.35f) else Color(0xFF2d0606).copy(0.35f)
                    ),
                    border = BorderStroke(1.dp, if (todayTaxVal > 0) Color(0xFF10b981).copy(0.40f) else Color(0xFFef4444).copy(0.40f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTaxDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (todayTaxVal > 0) Color(0xFF10b981).copy(0.12f) else Color(0xFFef4444).copy(0.12f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (todayTaxVal > 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (todayTaxVal > 0) Color(0xFF10b981) else Color(0xFFef4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ضريبة القات اليومية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(
                                text = if (todayTaxVal > 0) Helpers.formatMoney(todayTaxVal) else "لم تسجل بعد",
                                color = if (todayTaxVal > 0) Color(0xFF10b981) else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Card 2: Stall & Labor Outflows
                val isOutflowEntered = todayStallOutflow > 0 || todayLaborOutflow > 0
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOutflowEntered) Color(0xFF052e16).copy(0.35f) else Color(0xFF2d0606).copy(0.35f)
                    ),
                    border = BorderStroke(1.dp, if (isOutflowEntered) Color(0xFF10b981).copy(0.40f) else Color(0xFFef4444).copy(0.40f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTaxDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (isOutflowEntered) Color(0xFF10b981).copy(0.12f) else Color(0xFFef4444).copy(0.12f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isOutflowEntered) Icons.Default.Payments else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isOutflowEntered) Color(0xFF10b981) else Color(0xFFef4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("مفرش وعمالة اليوم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(
                                text = if (isOutflowEntered) Helpers.formatMoney(todayStallOutflow + todayLaborOutflow) else "لم تسجل بعد",
                                color = if (isOutflowEntered) Color(0xFF10b981) else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bento Stats Grid System
            // Today Sales Wide Bento Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                border = BorderStroke(1.dp, Color(0xFF10b981).copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { viewModel.navigateTo(Screen.Sales) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF063e23).copy(alpha = 0.35f),
                                    PalBlackNormal
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(
                            text = "إجمالي مبيعات اليوم",
                            color = Color(0xFF10b981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = Helpers.formatMoney(todaySalesTotal) + " " + currencySuffix,
                                color = PalWhitePure,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF10b981).copy(0.12f), CircleShape)
                            .align(Alignment.CenterEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF10b981),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Two Bento Cards side-by-side: Net Profit & Outstandings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Net Profit Card
                Card(
                     shape = RoundedCornerShape(20.dp),
                     colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                     border = BorderStroke(1.dp, Color(0xFF222222)),
                     modifier = Modifier
                         .weight(1f)
                         .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("صافي الربح", color = PalWhiteMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = Helpers.formatMoney(todayProfitEst) + " " + currencySuffix,
                            color = PalWhitePure,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Debts Card
                Card(
                     shape = RoundedCornerShape(20.dp),
                     colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                     border = BorderStroke(1.dp, Color(0xFF222222)),
                     modifier = Modifier
                         .weight(1f)
                         .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إجمالي الديون", color = PalWhiteMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = Helpers.formatMoney(totalDebtsOutstanding) + " " + currencySuffix,
                            color = Color(0xFFef4444),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Bento Stock Status Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                border = BorderStroke(1.dp, Color(0xFF222222)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { viewModel.navigateTo(Screen.Inventory) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val lowStockItemsCount = allItems.count { it.quantity < 10 }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("حالة المخزون (صعدي ممتاز)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        
                        if (lowStockItemsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color(0xFFef4444).copy(0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "كمية منخفضة لـ $lowStockItemsCount أصناف",
                                    color = Color(0xFFef4444),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color(0xFF10b981).copy(0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "المخزون ممتاز ومستقر",
                                    color = Color(0xFF10b981),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // Progress Bar
                    val totalInventoryItems = allItems.size
                    val percentage = if (totalInventoryItems > 0) {
                        (totalInventoryItems - lowStockItemsCount).toFloat() / totalInventoryItems.toFloat()
                    } else {
                        1.0f
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFF222222))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percentage)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (lowStockItemsCount > 0) Color(0xFFef4444) else Color(0xFF10b981))
                        )
                    }
                }
            }

            // Quick Access Grid Section
            Text(
                text = "الوصول السريع",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    title = "بيع جديد",
                    icon = Icons.Default.Add,
                    iconBgColor = Color(0xFF10b981).copy(0.12f),
                    iconColor = Color(0xFF10b981),
                    onClick = { viewModel.navigateTo(Screen.Sales) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionItem(
                    title = "المخزون",
                    icon = Icons.Default.Layers,
                    iconBgColor = Color(0xFF3b82f6).copy(0.12f),
                    iconColor = Color(0xFF3b82f6),
                    onClick = { viewModel.navigateTo(Screen.Inventory) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionItem(
                    title = "العملاء",
                    icon = Icons.Default.People,
                    iconBgColor = Color(0xFFf97316).copy(0.12f),
                    iconColor = Color(0xFFf97316),
                    onClick = { viewModel.navigateTo(Screen.Customers) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Remaining Management Grid Tiles
            Text(
                text = "أقسام الإدارة والتحاسب",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            )

            Column(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuTile(
                        title = "الموردين والمشتريات",
                        icon = Icons.Default.LocalShipping,
                        color = Color(0xFF8b5cf6), // Purple
                        onClick = { viewModel.navigateTo(Screen.Suppliers) },
                        modifier = Modifier.weight(1f)
                    )
                    MenuTile(
                        title = "المصروفات اليومية",
                        icon = Icons.Default.Payments,
                        color = Color(0xFFf43f5e), // Pink
                        onClick = { viewModel.navigateTo(Screen.Expenses) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuTile(
                        title = "التقارير المتقدمة",
                        icon = Icons.Default.Assessment,
                        color = PalGoldCalligraphy, // Gold
                        onClick = { viewModel.navigateTo(Screen.Reports) },
                        modifier = Modifier.weight(1f)
                    )
                    MenuTile(
                        title = "الحسابات والأرباح",
                        icon = Icons.Default.BarChart,
                        color = Color(0xFF10b981), // Green
                        onClick = { viewModel.navigateTo(Screen.Accounts) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuTile(
                        title = "حركة الحوالات المالية",
                        icon = Icons.Default.CompareArrows,
                        color = Color(0xFF3b82f6), // Blue
                        onClick = { viewModel.navigateTo(Screen.Transfers) },
                        modifier = Modifier.weight(1f)
                    )
                    MenuTile(
                        title = "أرشيف السجلات الكلي",
                        icon = Icons.Default.FolderOpen,
                        color = Color(0xFF06b6d4), // Cyan
                        onClick = { viewModel.navigateTo(Screen.Archive) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuTile(
                        title = "إعدادات النظام والتقني",
                        icon = Icons.Default.Settings,
                        color = Color(0xFF94a3b8), // Slate
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Canvas analytical graphics Styled Card
            Text(
                text = "الأداء المالي البياني",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                border = BorderStroke(1.dp, Color(0xFF222222)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مقارنة مبيعات ومصروفات الأسبوع", color = PalWhitePure, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF10b981)).clip(RoundedCornerShape(2.dp)))
                            Text(" مبيعات", color = PalWhiteMuted, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp, end = 12.dp))
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFef4444)).clip(RoundedCornerShape(2.dp)))
                            Text(" مصروفات", color = PalWhiteMuted, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Simulated simple graph with Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Draw Grid lines
                        val numPoints = 7
                        val barGap = canvasWidth / (numPoints + 1)
                        val maxSales = 120000.0
                        val rawSales = listOf(30000f, 65000f, 40000f, 85000f, 95000f, 110000f, todaySalesTotal.toFloat())
                        val rawExp = listOf(12000f, 19000f, 8000f, 32000f, 15000f, 22000f, todayExpensesTotal.toFloat())

                        for (i in 0 until numPoints) {
                            val xPos = barGap * (i + 1)
                            
                            // Scale values
                            val salesHeight = (rawSales[i] / maxSales) * canvasHeight
                            val expHeight = (rawExp[i] / maxSales) * canvasHeight

                            // Draw Sales Bar (Green)
                            drawRect(
                                color = Color(0xFF10b981),
                                topLeft = androidx.compose.ui.geometry.Offset(xPos - 8.dp.toPx(), (canvasHeight - salesHeight).toFloat()),
                                size = androidx.compose.ui.geometry.Size(6.dp.toPx(), salesHeight.toFloat())
                            )

                            // Draw Expenses Bar (Red)
                            drawRect(
                                color = Color(0xFFef4444),
                                topLeft = androidx.compose.ui.geometry.Offset(xPos + 1.dp.toPx(), (canvasHeight - expHeight).toFloat()),
                                size = androidx.compose.ui.geometry.Size(6.dp.toPx(), expHeight.toFloat())
                            )
                        }
                    }
                }
            }

            // Real Live Recent Operations List Card
            if (recentOperations.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                    border = BorderStroke(1.dp, Color(0xFF222222)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("آخر العمليات", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "عرض الكل",
                                color = Color(0xFF10b981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.navigateTo(Screen.Archive) }
                                    .padding(4.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            recentOperations.forEachIndexed { index, op ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (op.isExpense) Color(0xFFef4444).copy(0.12f) else Color(0xFF10b981).copy(0.12f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (op.isExpense) Icons.Default.Remove else Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (op.isExpense) Color(0xFFef4444) else Color(0xFF10b981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(op.title, color = PalWhitePure, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(op.subtitle, color = PalWhiteMuted, fontSize = 10.sp)
                                    }
                                    Text(
                                        text = (if (op.isExpense) "-" else "+") + Helpers.formatMoney(op.amount),
                                        color = if (op.isExpense) Color(0xFFef4444) else PalWhitePure,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (index < recentOperations.size - 1) {
                                    Divider(color = Color(0xFF222222), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // --- Daily Tax Input Dialog ---
    if (showTaxDialog) {
        val currentEntry = todayTaxEntry ?: TaxEntry(date = Helpers.getCurrentDate(), taxAmount = 0.0)
        var taxInput by remember { mutableStateOf(if (currentEntry.taxAmount > 0) currentEntry.taxAmount.toInt().toString() else "") }
        var stallInput by remember { mutableStateOf(if (currentEntry.stallOutflow > 0) currentEntry.stallOutflow.toInt().toString() else "") }
        var laborInput by remember { mutableStateOf(if (currentEntry.laborOutflow > 0) currentEntry.laborOutflow.toInt().toString() else "") }
        var notesInput by remember { mutableStateOf(currentEntry.notes) }

        AlertDialog(
            onDismissRequest = { showTaxDialog = false },
            title = {
                Text(
                    text = "الرسوم والمصروفات اليومية الثابتة",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
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
                        text = "يتم تسجيل الضريبة والمصروفات الثابتة مرة واحدة يومياً وتنعكس تلقائياً في التقارير الحسابية والمالية والمطبوعات.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = taxInput,
                        onValueChange = { taxInput = it },
                        label = { Text("ضريبة القات اليومية (ريال)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10b981),
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = stallInput,
                        onValueChange = { stallInput = it },
                        label = { Text("خرج المفرش اليومي (ريال)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10b981),
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = laborInput,
                        onValueChange = { laborInput = it },
                        label = { Text("خرج العمال اليومي (ريال)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10b981),
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("شرح / ملاحظات المصروفات", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10b981),
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
                        val tax = taxInput.toDoubleOrNull() ?: 0.0
                        val stall = stallInput.toDoubleOrNull() ?: 0.0
                        val labor = laborInput.toDoubleOrNull() ?: 0.0
                        viewModel.saveTodayTaxAndOutflows(tax, stall, labor, notesInput)
                        showTaxDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10b981))
                ) {
                    Text("حفظ البيانات اليومية", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTaxDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF111111)
        )
    }
}

@Composable
fun QuickActionItem(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = BorderStroke(1.dp, Color(0xFF222222)),
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun MenuTile(title: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = BorderStroke(1.dp, Color(0xFF222222)),
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun BottomNavigationBar(viewModel: AppViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Divider(color = Color(0xFF222222), thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BottomNavItem(
                title = "الرئيسية",
                icon = Icons.Default.Home,
                selected = currentScreen is Screen.Dashboard,
                onClick = { viewModel.navigateTo(Screen.Dashboard) }
            )
            BottomNavItem(
                title = "الحسابات",
                icon = Icons.Default.Leaderboard,
                selected = currentScreen is Screen.Accounts,
                onClick = { viewModel.navigateTo(Screen.Accounts) }
            )
            BottomNavItem(
                title = "الأرشيف",
                icon = Icons.Default.History,
                selected = currentScreen is Screen.Archive,
                onClick = { viewModel.navigateTo(Screen.Archive) }
            )
            BottomNavItem(
                title = "الإعدادات",
                icon = Icons.Default.Settings,
                selected = currentScreen is Screen.Settings,
                onClick = { viewModel.navigateTo(Screen.Settings) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (selected) Color(0xFF10b981) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = if (selected) Color(0xFF10b981) else Color.Gray,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
