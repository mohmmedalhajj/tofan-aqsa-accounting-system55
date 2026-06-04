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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryItem
import com.example.data.Supplier
import com.example.data.SupplierPurchase
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import com.example.util.Helpers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.example.ui.components.BluetoothPrintDialog
import com.example.ui.components.UnifiedPeriodSelector

@Composable
fun AssistButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color
) {
    Surface(
        onClick = onClick,
        color = PalBlackDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, PalBlackLight),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = text, tint = tint, modifier = Modifier.size(16.dp))
            Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val suppliersList by viewModel.allSuppliers.collectAsState()
    val allPurchasesList by viewModel.allPurchases.collectAsState()
    val inventoryItems by viewModel.inventoryItems.collectAsState()
    val allPurchaseItems by viewModel.allPurchaseItems.collectAsState()

    var isAddingPurchase by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Forms for adding/editing suppliers
    var selectedSupplierForEdit by remember { mutableStateOf<Supplier?>(null) }
    var supplierFormName by remember { mutableStateOf("") }
    var supplierFormPhone by remember { mutableStateOf("") }

    // Forms for creating new purchase
    var selectedSupplierForPurchase by remember { mutableStateOf<Supplier?>(null) }
    val newPurchaseItems = remember { mutableStateListOf<Triple<InventoryItem, Double, Double>>() } // Item, Quantity, Unit Buy Price
    var paidAmountInput by remember { mutableStateOf("") }
    var purchasePaymentMethod by remember { mutableStateOf("نقداً") }
    
    var showProductSelectionDialog by remember { mutableStateOf(false) }

    // View Purchase Details State
    var viewingPurchase by remember { mutableStateOf<SupplierPurchase?>(null) }
    var viewingPurchaseItems by remember { mutableStateOf<List<com.example.data.SupplierPurchaseItem>>(emptyList()) }
    var showPurchaseDetailsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewingPurchase) {
        viewingPurchase?.let { purchase ->
            viewModel.getPurchaseItemsFlow(purchase.id).collect { items ->
                viewingPurchaseItems = items
            }
        }
    }

    // Void Purchase Confirm State
    var purchaseToVoid by remember { mutableStateOf<SupplierPurchase?>(null) }
    var showVoidConfirm by remember { mutableStateOf(false) }

    var showSupplierReportsDialog by remember { mutableStateOf(false) }
    var selectedSupplierReportPeriod by remember { mutableStateOf("يومي") } // يومي, شهري, سنوي

    var showSupplierStatementDialog by remember { mutableStateOf(false) }
    var selectedSupplierForStatement by remember { mutableStateOf<Supplier?>(null) }
    var selectedSupplierStatementPeriod by remember { mutableStateOf("الكل") }
    var supplierSearchQuery by remember { mutableStateOf("") }
    var editingPurchaseId by remember { mutableStateOf<Int?>(null) }
    var deedDateInput by remember { mutableStateOf("") }
    var purchaseSearchQuery by remember { mutableStateOf("") }
    var supplierReportDate by remember { mutableStateOf(Helpers.getCurrentDateTime()) }
    var supplierGeneralReportDate by remember { mutableStateOf(Helpers.getCurrentDateTime()) }

    var customSupplierReportType by remember { mutableStateOf("date") }
    var customSupplierReportValue by remember { mutableStateOf("") }
    var customSupplierStatementType by remember { mutableStateOf("date") }
    var customSupplierStatementValue by remember { mutableStateOf("") }

    var showBluetoothPrintTrigger by remember { mutableStateOf(false) }
    var bluetoothPrintText by remember { mutableStateOf("") }

    var showEditPurchaseDialog by remember { mutableStateOf(false) }
    var editPurchasePaidAmount by remember { mutableStateOf("") }
    var editPurchasePaymentMethod by remember { mutableStateOf("نقداً") }

    // Return Goods form states
    var showAddReturnDialog by remember { mutableStateOf(false) }
    var selectedSupplierForReturn by remember { mutableStateOf<Supplier?>(null) }
    var selectedItemForReturn by remember { mutableStateOf<InventoryItem?>(null) }
    var returnedQtyInput by remember { mutableStateOf("") }
    var returnedPriceInput by remember { mutableStateOf("") }
    var returnDateInput by remember { mutableStateOf(Helpers.getCurrentDateTime()) }
    var returnNotesInput by remember { mutableStateOf("") }
    var refundedAmountInput by remember { mutableStateOf("") }

    // Calculations
    val purchaseTotal = newPurchaseItems.sumOf { it.second * it.third }
    val paidBuyAmount = paidAmountInput.toDoubleOrNull() ?: 0.0
    val remainingDebtOwed = if (purchaseTotal > paidBuyAmount) purchaseTotal - paidBuyAmount else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAddingPurchase) "شراء وتوريد بضوع قات" else "إدارة الموردين والمشتريات", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isAddingPurchase) {
                            isAddingPurchase = false
                        } else {
                            viewModel.navigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PalWhitePure)
                    }
                },
                actions = {
                    if (!isAddingPurchase) {
                        IconButton(onClick = {
                            android.widget.Toast.makeText(context, "تم مزامنة وتحديث سجلات الموردين والمشتريات بنجاح 🔄", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث السجلات", tint = PalWhitePure)
                        }
                        IconButton(onClick = {
                            selectedSupplierForEdit = null
                            supplierFormName = ""
                            supplierFormPhone = ""
                            showAddSupplierDialog = true
                        }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "مورد جديد", tint = PalGreenLight)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PalBlackNormal)
            )
        },
        containerColor = PalBlackDark
    ) { innerPadding ->
        if (isAddingPurchase) {
            // Screen layout to ADD multi-item Purchases under a supplier
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Supplier select
                Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("مورد البضاعة المسجلة", color = PalWhiteMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        var expanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                                .border(1.dp, PalBlackLight, RoundedCornerShape(4.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = selectedSupplierForPurchase?.name ?: "انقر هنا لتحديد المورد المعتمد للعملية",
                                color = if (selectedSupplierForPurchase != null) PalWhitePure else PalWhiteMuted
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(PalBlackNormal)
                        ) {
                            suppliersList.forEach { supplier ->
                                DropdownMenuItem(
                                    text = { Text(supplier.name, color = PalWhitePure) },
                                    onClick = {
                                        selectedSupplierForPurchase = supplier
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Add several items inside the purchase block
                Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("أصناف فاتورة التوريد والشراء", color = PalWhiteMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Button(
                                onClick = { showProductSelectionDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("توريد صنف", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (newPurchaseItems.isEmpty()) {
                            Text(
                                "لم يتم إدراج أصناف توريد في هذه الفاتورة",
                                color = PalWhiteMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        } else {
                            newPurchaseItems.forEachIndexed { index, triple ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(triple.first.name, color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${triple.second.toInt()} حبة x ${Helpers.formatMoney(triple.third)}", color = PalWhiteMuted, fontSize = 11.sp)
                                    }

                                    Text(
                                        Helpers.formatMoney(triple.second * triple.third),
                                        color = PalWhitePure,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )

                                    IconButton(
                                        onClick = { newPurchaseItems.removeAt(index) },
                                        modifier = Modifier.weight(0.5f)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = PalRedLight, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Divider(color = PalBlackLight, thickness = 1.dp)
                            }
                        }
                    }
                }

                // Financial Box for Purchases
                Card(colors = CardDefaults.cardColors(containerColor = PalBlackNormal)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("التحاسب المالي للتوريد", color = PalWhiteMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي قيمة المشتريات التراكمية:", color = PalWhiteSoft)
                            Text(Helpers.formatMoney(purchaseTotal), color = PalWhitePure, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = deedDateInput,
                            onValueChange = { deedDateInput = it },
                            label = { Text("تاريخ وقت السند (yyyy-MM-dd HH:mm)", color = PalWhiteMuted) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight, focusedTextColor = Color.White, unfocusedTextColor = PalWhiteSoft),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = paidAmountInput,
                            onValueChange = { paidAmountInput = it },
                            label = { Text("المبلغ المدفوع للمورد (ريال)", color = PalWhiteMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight, focusedTextColor = Color.White, unfocusedTextColor = PalWhiteSoft),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("طريقة تسديد المشتريات:", color = PalWhiteSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val buyPaymentMethods = listOf("نقداً", "آجل", "إيداع", "تحويل", "دفعة جزئية")
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            buyPaymentMethods.forEach { method ->
                                val isSel = purchasePaymentMethod == method
                                val bg = if (isSel) PalGreenNormal else PalBlackLight
                                val fg = if (isSel) Color.White else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bg)
                                        .clickable { 
                                            purchasePaymentMethod = method 
                                            if (method == "آجل") {
                                                paidAmountInput = "0"
                                            } else if (method == "نقداً" || method == "إيداع" || method == "تحويل") {
                                                paidAmountInput = purchaseTotal.toString()
                                            } else if (method == "دفعة جزئية") {
                                                paidAmountInput = ""
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(text = method, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Divider(color = PalBlackLight, thickness = 1.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الرصيد المتبقي آجل للمورد (علينا ديون):", color = PalWhiteSoft)
                            Text(Helpers.formatMoney(remainingDebtOwed), color = if (remainingDebtOwed > 0) PalRedLight else PalWhitePure, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Save Purchases
                Button(
                    onClick = {
                        if (selectedSupplierForPurchase == null) {
                            android.widget.Toast.makeText(context, "الرجاء تحديد المورد المعتمد لهذه العملية!", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPurchaseItems.isEmpty()) {
                            android.widget.Toast.makeText(context, "الرجاء إدراج صنف وارد واحد على الأقل!", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val finalDate = deedDateInput.ifEmpty { Helpers.getCurrentDateTime() }

                        if (editingPurchaseId == null) {
                            viewModel.executeSupplierPurchase(
                                supplierId = selectedSupplierForPurchase!!.id,
                                itemsPriceAndQty = newPurchaseItems.toList(),
                                paidAmount = paidBuyAmount,
                                paymentMethod = purchasePaymentMethod
                            )
                            android.widget.Toast.makeText(context, "تم حفظ وتوريد الأصناف بنجاح للمخازن", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.editSupplierPurchase(
                                purchaseId = editingPurchaseId!!,
                                newSupplierId = selectedSupplierForPurchase!!.id,
                                newDate = finalDate,
                                newItemsPriceAndQty = newPurchaseItems.toList(),
                                newPaidAmount = paidBuyAmount,
                                newPaymentMethod = purchasePaymentMethod
                            )
                            android.widget.Toast.makeText(context, "تم تعديل سند التوريد وتحديث المخزون والقيود بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        isAddingPurchase = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("حفظ وتسجيل عملية التوريد والمشتريات", color = PalWhitePure, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // General suppliers directory and purchase statements list
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Segment Choice: Suppliers Directory or Past Purchases List
                var selectedTab by remember { mutableStateOf(0) }
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PalBlackNormal,
                    contentColor = PalGreenLight
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("سجل الموردين المسجلين", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("فواتير المشتريات والتوريد الواردة", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                }

                // Reports and Refresh action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showSupplierReportsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("التقارير والمشتريات 🗓️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            android.widget.Toast.makeText(context, "تم قراءة وتعمير وتحميل أحدث السجلات والمشتريات والموردين بنجاح 🔄", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalBlackLight),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحديث البيانات 🔄", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (selectedTab == 0) {
                    // Search box for suppliers
                    OutlinedTextField(
                        value = supplierSearchQuery,
                        onValueChange = { supplierSearchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("supplier_search_bar"),
                        placeholder = { Text("ابحث عن مورد باسمه أو رقم هاتفه...", color = PalWhiteMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = PalGreenLight) },
                        trailingIcon = {
                            if (supplierSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { supplierSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = PalWhiteMuted)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft,
                            focusedContainerColor = PalBlackDark,
                            unfocusedContainerColor = PalBlackDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    val filteredSuppliers = remember(suppliersList, supplierSearchQuery) {
                        suppliersList.filter {
                            it.name.contains(supplierSearchQuery, ignoreCase = true) ||
                            it.phone.contains(supplierSearchQuery)
                        }
                    }

                    // Suppliers List
                    if (filteredSuppliers.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (supplierSearchQuery.isEmpty()) "لا يوجد موردين مسجلين" else "لا توجد نتائج مطابقة للبحث",
                                color = PalWhiteMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredSuppliers) { supplier ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        // Deck 1: Info and Balance
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(supplier.name, color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("📞 هاتف: ${supplier.phone.ifEmpty { "غير محدد" }}", color = PalWhiteMuted, fontSize = 12.sp)
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("حساب المورد (علينا له)", color = PalWhiteMuted, fontSize = 9.sp)
                                                Text(Helpers.formatMoney(supplier.totalBalance), color = if (supplier.totalBalance > 0) PalRedLight else PalGreenLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = PalBlackLight.copy(0.5f), modifier = Modifier.fillMaxWidth())
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Deck 2: Actions Bar which makes Edit, Delete, Return, Statement, and Supply visible & spaced
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Action 1: Statement (كشف الحساب)
                                            AssistButton(
                                                onClick = {
                                                    selectedSupplierForStatement = supplier
                                                    selectedSupplierStatementPeriod = "الكل"
                                                    showSupplierStatementDialog = true
                                                },
                                                icon = Icons.Default.ReceiptLong,
                                                text = "كشف حساب",
                                                tint = PalGoldCalligraphy
                                            )

                                            // Action 2: Supply (توريد بضاعة)
                                            AssistButton(
                                                onClick = {
                                                    selectedSupplierForPurchase = supplier
                                                    newPurchaseItems.clear()
                                                    paidAmountInput = ""
                                                    editingPurchaseId = null
                                                    deedDateInput = Helpers.getCurrentDateTime()
                                                    purchasePaymentMethod = "نقداً"
                                                    isAddingPurchase = true
                                                },
                                                icon = Icons.Default.CloudUpload,
                                                text = "توريد بضاعة",
                                                tint = PalGreenLight
                                            )

                                            // Action 3: Return (إضافة مرتجع بضاعة)
                                            AssistButton(
                                                onClick = {
                                                    selectedSupplierForReturn = supplier
                                                    selectedItemForReturn = null
                                                    returnedQtyInput = ""
                                                    returnedPriceInput = ""
                                                    refundedAmountInput = ""
                                                    returnNotesInput = ""
                                                    returnDateInput = Helpers.getCurrentDateTime()
                                                    showAddReturnDialog = true
                                                },
                                                icon = Icons.Default.AssignmentReturn,
                                                text = "إضافة مرتجع",
                                                tint = PalRedLight
                                            )

                                            // Action 4: Edit (تعديل)
                                            AssistButton(
                                                onClick = {
                                                    selectedSupplierForEdit = supplier
                                                    supplierFormName = supplier.name
                                                    supplierFormPhone = supplier.phone
                                                    showAddSupplierDialog = true
                                                },
                                                icon = Icons.Default.Edit,
                                                text = "تعديل",
                                                tint = PalWhiteMuted
                                            )

                                            // Action 5: Delete (حذف)
                                            AssistButton(
                                                onClick = {
                                                    selectedSupplierForEdit = supplier
                                                    showDeleteConfirm = true
                                                },
                                                icon = Icons.Default.Delete,
                                                text = "حذف",
                                                tint = PalRedNormal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Purchases List with advanced search box supporting: Supplier name, phone, operation ID, date, amount, payment method
                    val filteredPurchases = remember(allPurchasesList, suppliersList, purchaseSearchQuery) {
                        allPurchasesList.filter { p ->
                            val sup = suppliersList.find { it.id == p.supplierId }
                            val nameMatch = sup?.name?.contains(purchaseSearchQuery, ignoreCase = true) == true
                            val phoneMatch = sup?.phone?.contains(purchaseSearchQuery) == true
                            val idMatch = p.id.toString() == purchaseSearchQuery.trim()
                            val dateMatch = p.date.contains(purchaseSearchQuery)
                            val methodMatch = p.paymentMethod.contains(purchaseSearchQuery)
                            val amountMatch = p.totalAmount.toString().contains(purchaseSearchQuery) || p.paidAmount.toString().contains(purchaseSearchQuery) || p.debtRemaining.toString().contains(purchaseSearchQuery)
                            
                            purchaseSearchQuery.isEmpty() || nameMatch || phoneMatch || idMatch || dateMatch || methodMatch || amountMatch
                        }
                    }

                    OutlinedTextField(
                        value = purchaseSearchQuery,
                        onValueChange = { purchaseSearchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("purchase_search_bar"),
                        placeholder = { Text("بحث متقدم (مورد، هاتف، رقم، تاريخ، مبلغ، سداد)...", color = PalWhiteMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = PalGreenLight) },
                        trailingIcon = {
                            if (purchaseSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { purchaseSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = PalWhiteMuted)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = PalWhiteSoft,
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedContainerColor = PalBlackDark,
                            unfocusedContainerColor = PalBlackDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (filteredPurchases.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(if (purchaseSearchQuery.isEmpty()) "لا توجد فواتير مشتريات وتوريد مسجلة حاليا" else "لا توجد نتائج مطابقة للبحث السريع", color = PalWhiteMuted)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredPurchases) { purchase ->
                                val sup = suppliersList.find { it.id == purchase.supplierId }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewingPurchase = purchase
                                            showPurchaseDetailsDialog = true
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("سند شراء #${purchase.id}", color = PalWhiteMuted, fontSize = 11.sp)
                                            Text(purchase.date, color = PalWhiteMuted, fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("المورد: ${sup?.name ?: "مورد مجهول"}", color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("إجمالي الفاتورة", color = PalWhiteMuted, fontSize = 11.sp)
                                                Text(Helpers.formatMoney(purchase.totalAmount), color = PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Column {
                                                Text("المدفوع للمورد", color = PalWhiteMuted, fontSize = 11.sp)
                                                Text(Helpers.formatMoney(purchase.paidAmount), color = PalGreenLight, fontSize = 13.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("الآجل علينا متبقي", color = PalWhiteMuted, fontSize = 11.sp)
                                                Text(Helpers.formatMoney(purchase.debtRemaining), color = if (purchase.debtRemaining > 0) PalRedLight else PalWhitePure, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("انقر للاطلاع والتصدير 📋", color = PalWhiteMuted, fontSize = 11.sp)
                                            Row {
                                                IconButton(onClick = {
                                                    editingPurchaseId = purchase.id
                                                    selectedSupplierForPurchase = suppliersList.find { it.id == purchase.supplierId }
                                                    val itemsForThisPurchase = allPurchaseItems.filter { it.purchaseId == purchase.id }
                                                    val loadedItems = itemsForThisPurchase.map { pi ->
                                                        val invItem = inventoryItems.find { it.id == pi.itemId } ?: com.example.data.InventoryItem(id = pi.itemId, name = pi.itemName, quantity = pi.quantity, buyPrice = pi.unitPrice, sellPrice = 0.0, lowStockThreshold = 5.0)
                                                        Triple(invItem, pi.quantity, pi.unitPrice)
                                                    }
                                                    newPurchaseItems.clear()
                                                    newPurchaseItems.addAll(loadedItems)
                                                    paidAmountInput = purchase.paidAmount.toString()
                                                    purchasePaymentMethod = purchase.paymentMethod
                                                    deedDateInput = purchase.date
                                                    isAddingPurchase = true
                                                }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "تعديل الفاتورة", tint = PalGreenLight)
                                                }
                                                IconButton(onClick = {
                                                    purchaseToVoid = purchase
                                                    showVoidConfirm = true
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "إلغاء المشتريات", tint = PalRedLight)
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
        }
    }

    // --- Add / Edit Supplier Dialog ---
    if (showAddSupplierDialog) {
        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = { Text(if (selectedSupplierForEdit == null) "إضافة مورد جديد" else "تعديل بيانات المورد", color = PalWhitePure, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = supplierFormName,
                        onValueChange = { supplierFormName = it },
                        label = { Text("اسم المورد المسجل", color = PalWhiteMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = supplierFormPhone,
                        onValueChange = { supplierFormPhone = it },
                        label = { Text("رقم هاتفه المعتمد", color = PalWhiteMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedSupplierForEdit != null) {
                        Button(
                            onClick = {
                                showDeleteConfirm = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal)
                        ) {
                            Text("حذف المورد", color = PalWhitePure)
                        }
                    }
                    Button(
                        onClick = {
                            if (supplierFormName.trim().isEmpty()) {
                                android.widget.Toast.makeText(context, "الرجاء إدخال اسم المورد!", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedSupplierForEdit == null) {
                                viewModel.createSupplier(supplierFormName, supplierFormPhone)
                            } else {
                                viewModel.updateSupplier(selectedSupplierForEdit!!.copy(name = supplierFormName, phone = supplierFormPhone))
                            }
                            showAddSupplierDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                    ) {
                        Text("حفظ", color = PalWhitePure)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) {
                    Text("إلغاء", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    // --- Supplier Product Selection Picker ---
    if (showProductSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showProductSelectionDialog = false },
            title = { Text("إدراج صنف توريد قات", color = PalWhitePure, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var isCustomItem by remember { mutableStateOf(false) }
                    var customItemNameInput by remember { mutableStateOf("") }
                    var customItemSellPriceInput by remember { mutableStateOf("") }
                    var pickedProd by remember { mutableStateOf<InventoryItem?>(null) }
                    var supplyQty by remember { mutableStateOf("10") }
                    var supplyBuyPrice by remember { mutableStateOf("") }
                    var customItemCurrency by remember { mutableStateOf("ريال يمني") }

                    // Toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isCustomItem = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isCustomItem) PalGreenNormal else PalBlackLight
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("اختيار صنف موجود", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = { isCustomItem = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCustomItem) PalGreenNormal else PalBlackLight
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إدخال صنف جديد يدوياً", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    if (!isCustomItem) {
                        Text("اختر الصنف لتوريده للمخزن:", color = PalWhiteMuted, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .border(1.dp, PalBlackLight)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column {
                                inventoryItems.forEach { prod ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (pickedProd == prod) PalGreenDark else Color.Transparent)
                                            .clickable {
                                                pickedProd = prod
                                                supplyBuyPrice = prod.buyPrice.toString()
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Text(prod.name, color = PalWhitePure, fontSize = 13.sp)
                                    }
                                    Divider(color = PalBlackLight)
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = customItemNameInput,
                            onValueChange = { customItemNameInput = it },
                            label = { Text("اسم الصنف الجديد (أمثلة: عود، فراد، قطل، فليق، ماوية، زكري)", color = PalWhiteMuted) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight, focusedTextColor = PalWhitePure, unfocusedTextColor = PalWhiteSoft),
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
                                    onClick = { customItemCurrency = curr },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (customItemCurrency == curr) PalGreenNormal else PalBlackLight
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(curr, fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = supplyQty,
                        onValueChange = { supplyQty = it },
                        label = { Text("الكمية المستلمة الموردة (حبة)", color = PalWhiteMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight, focusedTextColor = PalWhitePure, unfocusedTextColor = PalWhiteSoft),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = supplyBuyPrice,
                        onValueChange = { supplyBuyPrice = it },
                        label = { Text(if (isCustomItem) "سعر شراء الحبة (ريال)" else "سعر شراء الحبة (ريال)", color = PalWhiteMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PalGreenLight, unfocusedBorderColor = PalBlackLight, focusedTextColor = PalWhitePure, unfocusedTextColor = PalWhiteSoft),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val qty = supplyQty.toDoubleOrNull() ?: 0.0
                            val bPrice = supplyBuyPrice.toDoubleOrNull() ?: 0.0
                            if (qty <= 0.0 || bPrice <= 0.0) {
                                android.widget.Toast.makeText(context, "الرجاء كتابة كمية وسعر شراء أكبر من الصفر!", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (!isCustomItem) {
                                if (pickedProd != null) {
                                    newPurchaseItems.add(Triple(pickedProd!!, qty, bPrice))
                                    showProductSelectionDialog = false
                                } else {
                                    android.widget.Toast.makeText(context, "يرجى تحديد الصنف أولاً!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                if (customItemNameInput.trim().isEmpty()) {
                                    android.widget.Toast.makeText(context, "يرجى كتابة اسم الصنف الجديد!", android.widget.Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val sPrice = 0.0
                                scope.launch {
                                    val newItem = InventoryItem(
                                        id = 0,
                                        name = customItemNameInput.trim(),
                                        quantity = 0.0,
                                        buyPrice = bPrice,
                                        sellPrice = sPrice,
                                        lowStockThreshold = 5.0,
                                        buyPriceCurrency = customItemCurrency
                                    )
                                    val generatedId = viewModel.saveInventoryItemDirectly(newItem)
                                    val completeItem = newItem.copy(id = generatedId)
                                    newPurchaseItems.add(Triple(completeItem, qty, bPrice))
                                    showProductSelectionDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إدراج بفاتورة التوريد", color = PalWhitePure)
                    }
                }
            },
            confirmButton = {},
            containerColor = PalBlackNormal
        )
    }

    // --- Details Viewing Dialog ---
    if (showPurchaseDetailsDialog && viewingPurchase != null) {
        val sup = suppliersList.find { it.id == viewingPurchase!!.supplierId }
        AlertDialog(
            onDismissRequest = { showPurchaseDetailsDialog = false },
            title = { Text("تفاصيل فاتورة المشتريات #${viewingPurchase!!.id}", color = PalWhitePure, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المورد: ${sup?.name ?: "مورد مجهول"}", color = PalWhitePure, fontWeight = FontWeight.Bold)
                    Text("التاريخ والوقت: ${viewingPurchase!!.date}", color = PalWhiteMuted, fontSize = 12.sp)
                    Divider(color = PalBlackLight)

                    Text("الأصناف والمواد المسجلة للتوريد:", color = PalWhiteMuted, fontSize = 12.sp)
                    viewingPurchaseItems.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.itemName, color = PalWhitePure, fontSize = 13.sp)
                            Text("${item.quantity.toInt()} حبة x ${Helpers.formatMoney(item.unitPrice)}", color = PalWhiteSoft, fontSize = 13.sp)
                        }
                    }

                    Divider(color = PalBlackLight)
                    Text("المجموع الإجمالي: ${Helpers.formatMoney(viewingPurchase!!.totalAmount)}", color = PalGreenLight, fontWeight = FontWeight.Bold)
                    Text("المدفوع نقداً للمورد: ${Helpers.formatMoney(viewingPurchase!!.paidAmount)}", color = PalWhiteSoft)
                    Text("الرصيد الآجل المتبقي ديون للمورد: ${Helpers.formatMoney(viewingPurchase!!.debtRemaining)}", color = if (viewingPurchase!!.debtRemaining > 0) PalRedLight else PalWhitePure)
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    val msg = buildString {
                        appendLine("وكالة عاهد الصبري")
                        appendLine("أمر توريد بضاعة الماوية والورزاني")
                        appendLine("المورد: ${sup?.name ?: "مورد مجهول"}")
                        appendLine("سند توريد رقم: #${viewingPurchase!!.id}")
                        appendLine("التاريخ والوقت: ${viewingPurchase!!.date}")
                        appendLine("-------------------------")
                        for (item in viewingPurchaseItems) {
                            appendLine("${item.itemName} x ${item.quantity.toInt()} حبة = ${Helpers.formatMoney(item.quantity * item.unitPrice)}")
                        }
                        appendLine("-------------------------")
                        appendLine("المجموع الإجمالي: ${Helpers.formatMoney(viewingPurchase!!.totalAmount)}")
                        appendLine("المدفوع نقداً للمورد: ${Helpers.formatMoney(viewingPurchase!!.paidAmount)}")
                        appendLine("الرصيد الآجل المتبقي: ${Helpers.formatMoney(viewingPurchase!!.debtRemaining)}")
                        appendLine("صاحب الوكالة: عاهد الصبري")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val headers = listOf("الصنف المورد", "الكمية المشتراة", "سعر البيع")
                                val rows = viewingPurchaseItems.map {
                                    listOf(it.itemName, "${it.quantity} حبة", Helpers.formatMoney(it.unitPrice))
                                }
                                Helpers.generatePdfAndShare(
                                    context = context,
                                    title = "أمر توريد بضاعة الماوية والورزاني",
                                    headers = headers,
                                    rows = rows,
                                    totals = mapOf("قيمة البضائع والتوريد الكلية" to Helpers.formatMoney(viewingPurchase!!.totalAmount))
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
                                Helpers.shareViaWhatsApp(context, sup?.phone ?: "", msg)
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
                            editPurchasePaidAmount = viewingPurchase!!.paidAmount.toString()
                            editPurchasePaymentMethod = viewingPurchase!!.paymentMethod
                            showEditPurchaseDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                    ) {
                        Text("تعديل فاتورة الشراء ✏️", color = PalWhitePure, fontSize = 12.sp)
                    }
                    TextButton(onClick = { showPurchaseDetailsDialog = false }) {
                        Text("إغلاق", color = PalWhiteMuted)
                    }
                }
            },
            containerColor = PalBlackNormal
        )
    }

    // --- Edit Purchase Invoice Dialog ---
    if (showEditPurchaseDialog && viewingPurchase != null) {
        val sup = suppliersList.find { it.id == viewingPurchase!!.supplierId }
        AlertDialog(
            onDismissRequest = { showEditPurchaseDialog = false },
            title = {
                Text(
                    text = "تعديل فاتورة الشراء رقم #${viewingPurchase!!.id}",
                    color = PalWhitePure,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "المورد: ${sup?.name ?: "مورد مجهول"}",
                        color = PalWhiteMuted,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "إجمالي قيمة الفاتورة: ${Helpers.formatMoney(viewingPurchase!!.totalAmount)}",
                        color = PalWhitePure,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = editPurchasePaidAmount,
                        onValueChange = { editPurchasePaidAmount = it },
                        label = { Text("المبلغ المدفوع للمورد (ريال)", color = PalWhiteMuted) },
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

                    Text("طريقة التحصيل / الدفع:", color = PalWhiteSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val purchasePaymentMethods = listOf("نقداً", "آجل", "إيداع", "تحويل")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        purchasePaymentMethods.forEach { method ->
                            val isSel = editPurchasePaymentMethod == method
                            val bg = if (isSel) PalGreenNormal else PalBlackLight
                            val fg = if (isSel) Color.White else Color.Gray
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bg)
                                    .clickable { editPurchasePaymentMethod = method }
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
                        val newPaid = editPurchasePaidAmount.toDoubleOrNull() ?: 0.0
                        viewModel.updateSupplierPurchase(
                            purchaseId = viewingPurchase!!.id,
                            newPaidAmount = newPaid,
                            newPaymentMethod = editPurchasePaymentMethod
                        )
                        showEditPurchaseDialog = false
                        showPurchaseDetailsDialog = false
                        android.widget.Toast.makeText(context, "تم تعديل فاتورة الشراء وتعديل حسابات المورد بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                ) {
                    Text("حفظ التعديلات", color = PalWhitePure)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPurchaseDialog = false }) {
                    Text("إلغاء", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    // --- Void Purchase Dialog ---
    if (showVoidConfirm && purchaseToVoid != null) {
        AlertDialog(
            onDismissRequest = { showVoidConfirm = false },
            title = { Text("إلغاء وفسخ عملية التوريد البضاعة؟", color = PalWhitePure) },
            text = { Text("هل أنت متأكد من إلغاء أمر توريد البضاعة رقم #${purchaseToVoid!!.id}؟ سيتم إسقاط الكميات المضافة للمخازن تلقائيا وتسوية القيود مع المورد.", color = PalWhiteMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.voidPurchase(purchaseToVoid!!.id)
                        showVoidConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal)
                ) {
                    Text("نعم، إلغاء التوريد", color = PalWhitePure)
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

    if (showDeleteConfirm && selectedSupplierForEdit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "تأكيد حذف المورد",
                    color = PalWhitePure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف المورد (${selectedSupplierForEdit!!.name}) نهائياً؟ سيتم مسح حساب المورد السحابي وكل السجلات المالية المرتبطة به.",
                    color = PalWhiteSoft,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSupplier(selectedSupplierForEdit!!)
                        showDeleteConfirm = false
                        showAddSupplierDialog = false
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

    if (showSupplierReportsDialog) {
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

        val periodPurchases = when (selectedSupplierReportPeriod) {
            "يومي" -> allPurchasesList.filter { it.date.startsWith(todayStr) }
            "أسبوعي" -> allPurchasesList.filter { isWeekly(it.date) }
            "شهري" -> allPurchasesList.filter { it.date.startsWith(todayStr.substring(0, 7)) }
            "سنوي" -> allPurchasesList.filter { it.date.startsWith(todayStr.substring(0, 4)) }
            "تقويم مخصص" -> {
                allPurchasesList.filter { p ->
                    val pDate = p.date.take(10)
                    when (customSupplierReportType) {
                        "date" -> pDate == customSupplierReportValue
                        "month" -> pDate.startsWith(customSupplierReportValue) // yyyy-MM
                        "year" -> pDate.startsWith(customSupplierReportValue) // yyyy
                        else -> true
                    }
                }
            }
            else -> allPurchasesList
        }
        val periodTotalCost = periodPurchases.sumOf { it.totalAmount }
        val periodCashPaid = periodPurchases.sumOf { it.paidAmount }
        val periodNewDebtRemaining = periodPurchases.sumOf { it.debtRemaining }
        val totalSuppliersDebtOutstanding = suppliersList.sumOf { it.totalBalance }

        AlertDialog(
            onDismissRequest = { showSupplierReportsDialog = false },
            title = {
                Text(
                    text = "تقارير المشتريات والتوريد للمفرش",
                    color = PalWhitePure,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "اختر دورة أو فترة التقرير لتجهيز إحصائيات المشتريات وحركات سداد الموردين:",
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
                            val isSel = selectedSupplierReportPeriod == period
                            val bg = if (isSel) PalGreenNormal else PalBlackLight
                            val fg = if (isSel) Color.White else Color.Gray
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable { selectedSupplierReportPeriod = period }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (period) {
                                        "يومي" -> "يومية (اليوم)"
                                        "أسبوعي" -> "أسبوعي"
                                        "شهري" -> "شهرية (الشهر)"
                                        "سنوي" -> "سنوية (العام)"
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

                    if (selectedSupplierReportPeriod == "تقويم مخصص") {
                        UnifiedPeriodSelector(
                            initialType = if (customSupplierReportType == "date") "يوم" else if (customSupplierReportType == "month") "شهر" else "سنة",
                            initialValue = customSupplierReportValue.ifEmpty { Helpers.getCurrentDate() },
                            onPeriodChanged = { type, formattedValue ->
                                customSupplierReportType = when (type) {
                                    "يوم" -> "date"
                                    "شهر" -> "month"
                                    "سنة" -> "year"
                                    else -> "date"
                                }
                                customSupplierReportValue = formattedValue
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = supplierGeneralReportDate,
                        onValueChange = { supplierGeneralReportDate = it },
                        label = { Text("تعيين وتخصيص تاريخ التقرير المطبوع", color = PalWhiteMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("supplier_general_report_date_input"),
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
                            Text("إجمالي قيمة المشتريات:", color = PalWhiteSoft, fontSize = 12.sp)
                            Text(Helpers.formatMoney(periodTotalCost), color = PalWhitePure, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المسدد نقداً للموردين:", color = PalWhiteSoft, fontSize = 12.sp)
                            Text(Helpers.formatMoney(periodCashPaid), color = PalGreenLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الديون المتبقية للموردين:", color = PalWhiteSoft, fontSize = 12.sp)
                            Text(Helpers.formatMoney(periodNewDebtRemaining), color = PalRedLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "كشف مفصل بالتوريد والتواريخ ($selectedSupplierReportPeriod):",
                        color = PalWhiteSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(PalBlackDark, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (periodPurchases.isEmpty()) {
                                Text(
                                    text = "لا توجد عمليات مبيعات أو توريد مشتريات مسجلة لهذه الفترة.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                periodPurchases.forEach { purchase ->
                                    val supName = suppliersList.find { it.id == purchase.supplierId }?.name ?: "مورد"
                                    val isReturn = purchase.totalAmount < 0 || purchase.paymentMethod.startsWith("مرتجع")
                                    val valPrefix = if (isReturn) "مرتجع: " else "توريد: "
                                    val absAmount = java.lang.Math.abs(purchase.totalAmount)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("$supName: $valPrefix${Helpers.formatMoney(absAmount)}", color = if (isReturn) PalGoldCalligraphy else PalWhitePure, fontSize = 10.sp)
                                        Text(purchase.date.split(" ")[0], color = PalWhiteMuted, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val reportMsg = buildString {
                        appendLine("وكالة عاهد الصبري")
                        appendLine("تقرير المشتريات والتوريد وحسابات الموردين التفصيلي")
                        appendLine("فترة التقرير: $selectedSupplierReportPeriod")
                        appendLine("التاريخ والوقت: $supplierGeneralReportDate")
                        appendLine("---------------------------------")
                        if (periodPurchases.isNotEmpty()) {
                            appendLine("📋 كشف تفاصيل عمليات الشراء والتوريد بالتفصيل والتواريخ:")
                            periodPurchases.forEachIndexed { index, p ->
                                val supName = suppliersList.find { it.id == p.supplierId }?.name ?: "مورد غير معروف"
                                val isReturn = p.totalAmount < 0 || p.paymentMethod.startsWith("مرتجع")
                                val docTitle = if (isReturn) "🔄 سند مرتجع بضاعة" else "📥 سند توريد"
                                val absTotal = java.lang.Math.abs(p.totalAmount)
                                val absPaid = java.lang.Math.abs(p.paidAmount)
                                val absDebt = java.lang.Math.abs(p.debtRemaining)

                                appendLine("${index + 1}) $docTitle #${p.id} - المورد: $supName")
                                appendLine("   التاريخ والوقت: ${p.date}")
                                appendLine("   طريقة الدفع/البيان: ${p.paymentMethod}")
                                appendLine("   قيمة الفاتورة الكلية: ${Helpers.formatMoney(absTotal)}")
                                appendLine("   المسترد/المدفوع نقداً: ${Helpers.formatMoney(absPaid)}")
                                appendLine("   المتبقي آجل ديون: ${Helpers.formatMoney(absDebt)}")
                                appendLine("   -------------------")
                            }
                            appendLine("---------------------------------")
                        } else {
                            appendLine("لا توجد فواتير مشتريات وتوريد مسجلة حاليا لهذه الفترة.")
                        }
                        appendLine("👈 إجمالي قيمة المشتريات: ${Helpers.formatMoney(periodTotalCost)}")
                        appendLine("👈 إجمالي المسدد نقدا للموردين: ${Helpers.formatMoney(periodCashPaid)}")
                        appendLine("👈 إجمالي الديون الآجلة الجديدة: ${Helpers.formatMoney(periodNewDebtRemaining)}")
                        appendLine("---------------------------------")
                        appendLine("إجمالي الذمم والديون المتبقية للموردين حالياً:")
                        appendLine("💰 ${Helpers.formatMoney(totalSuppliersDebtOutstanding)}")
                        appendLine("صاحب المفرش والوكالة: عاهد الصبري")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val tPrefix = when (selectedSupplierReportPeriod) {
                                    "يومي" -> "تقرير يومي"
                                    "أسبوعي" -> "تقرير أسبوعي"
                                    "شهري" -> "تقرير شهري"
                                    "سنوي" -> "تقرير سنوي"
                                    "تقويم مخصص" -> "التقرير المخصص ($customSupplierReportValue)"
                                    else -> "تقرير دوري"
                                }
                                val headers = listOf("التاريخ والسند", "تفاصيل العملية والأصناف المشمولة", "المبلغ الكلي", "المدفوع/المسترد", "المتبقي الآجل", "طريقة الدفع/البيان")
                                val rows = periodPurchases.map { p ->
                                    val supName = suppliersList.find { it.id == p.supplierId }?.name ?: "مورد غير معروف"
                                    val isReturn = p.totalAmount < 0 || p.paymentMethod.startsWith("مرتجع")
                                    val typeTitle = if (isReturn) "🔄 مرتجع" else "📥 توريد"
                                    val items = allPurchaseItems.filter { it.purchaseId == p.id }
                                    val itemsText = if (items.isNotEmpty()) {
                                        "\n الأصناف: " + items.joinToString(" | ") { "${it.itemName} (${java.lang.Math.abs(it.quantity).toInt()} حبة • بسعر: ${Helpers.formatMoney(it.unitPrice)})" }
                                    } else ""
                                    listOf(
                                        p.date,
                                        "$typeTitle #${p.id} - المورد: $supName$itemsText",
                                        Helpers.formatMoney(java.lang.Math.abs(p.totalAmount)),
                                        Helpers.formatMoney(java.lang.Math.abs(p.paidAmount)),
                                        Helpers.formatMoney(java.lang.Math.abs(p.debtRemaining)),
                                        p.paymentMethod
                                    )
                                }
                                Helpers.generatePdfAndShare(
                                    context = context,
                                    title = "$tPrefix مشتريات وتوريد وكالة عاهد الصبري التفصيلي",
                                    headers = headers,
                                    rows = rows,
                                    totals = mapOf(
                                        "إجمالي قيمة المشتريات بالفترة" to Helpers.formatMoney(periodTotalCost),
                                        "إجمالي المسدد نقداً للموردين بالفترة" to Helpers.formatMoney(periodCashPaid),
                                        "إجمالي الديون الآجلة الجديدة بالفترة" to Helpers.formatMoney(periodNewDebtRemaining),
                                        "إجمالي الدين الحالي المعلق للموردين كلياً" to Helpers.formatMoney(totalSuppliersDebtOutstanding)
                                    ),
                                    customDate = supplierGeneralReportDate
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("PDF", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                Helpers.shareViaWhatsApp(context, "", reportMsg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("واتساب", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                bluetoothPrintText = reportMsg
                                showBluetoothPrintTrigger = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGoldCalligraphy),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("طباعة 🖨️", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupplierReportsDialog = false }) {
                    Text("إغلاق", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }

    if (showSupplierStatementDialog && selectedSupplierForStatement != null) {
        val todayStrStr = Helpers.getCurrentDate()
        val curMonthStrStr = todayStrStr.substring(0, 7)
        val curYearStrStr = todayStrStr.substring(0, 4)

        val filteredStatementPurchases = allPurchasesList.filter { it.supplierId == selectedSupplierForStatement!!.id }.filter { p ->
            when (selectedSupplierStatementPeriod) {
                "يومي" -> p.date.startsWith(todayStrStr)
                "اسبوعي" -> Helpers.isWithinLast7Days(p.date)
                "شهري" -> p.date.startsWith(curMonthStrStr)
                "سنوي" -> p.date.startsWith(curYearStrStr)
                "تقويم مخصص" -> {
                    val pDate = p.date.take(10)
                    when (customSupplierStatementType) {
                        "date" -> pDate == customSupplierStatementValue
                        "month" -> pDate.startsWith(customSupplierStatementValue)
                        "year" -> pDate.startsWith(customSupplierStatementValue)
                        else -> true
                    }
                }
                else -> true
            }
        }

        val totalPurchasedVal = filteredStatementPurchases.sumOf { it.totalAmount }
        val totalPaidVal = filteredStatementPurchases.sumOf { it.paidAmount }
        val totalOutstandingVal = selectedSupplierForStatement!!.totalBalance

        val statementMsg = buildString {
            appendLine("وكالة عاهد الصبري")
            appendLine("كشف حساب تفصيلي للمورد: ${selectedSupplierForStatement!!.name}")
            appendLine("رقم الهاتف: ${selectedSupplierForStatement!!.phone.ifEmpty { "غير محدد" }}")
            appendLine("الفترة المحددة: $selectedSupplierStatementPeriod")
            appendLine("التاريخ والوقت: $supplierReportDate")
            appendLine("-------------------------")
            appendLine("💵 رصيد المديونية الكلي المستحق له حالياً:")
            appendLine("💰 ${Helpers.formatMoney(totalOutstandingVal)}")
            appendLine("-------------------------")
            if (filteredStatementPurchases.isNotEmpty()) {
                appendLine("📥 حركات وفواتير التوريد والمرتجعات للفترة المختارة:")
                filteredStatementPurchases.forEach { purchase ->
                    val isReturn = purchase.totalAmount < 0 || purchase.paymentMethod.startsWith("مرتجع")
                    val typeStr = if (isReturn) "🔄 سند مرتجع بضاعة" else "📥 سند توريد"
                    val absTotal = java.lang.Math.abs(purchase.totalAmount)
                    val absPaid = java.lang.Math.abs(purchase.paidAmount)
                    val absDebt = java.lang.Math.abs(purchase.debtRemaining)
                    appendLine("• $typeStr #${purchase.id} بتاريخ ${purchase.date} بقيمة ${Helpers.formatMoney(absTotal)} (${purchase.paymentMethod} • المسدد/المسترد: ${Helpers.formatMoney(absPaid)} • المتبقي: ${Helpers.formatMoney(absDebt)})")
                    val items = allPurchaseItems.filter { it.purchaseId == purchase.id }
                    if (items.isNotEmpty()) {
                        appendLine("   الأصناف بالعملية:")
                        items.forEach { item ->
                            val absQty = java.lang.Math.abs(item.quantity)
                            appendLine("   👈 ${item.itemName} (${absQty.toInt()} حبة • بسعر: ${Helpers.formatMoney(item.unitPrice)})")
                        }
                    }
                    appendLine("   -------------------")
                }
                appendLine("-------------------------")
            }
            appendLine("إجمالي قيمة المشتريات بالفترة: ${Helpers.formatMoney(totalPurchasedVal)}")
            appendLine("إجمالي المبالغ المسددة بالفترة: ${Helpers.formatMoney(totalPaidVal)}")
            appendLine("-------------------------")
            appendLine("صاحب الوكالة: عاهد الصبري")
        }

        AlertDialog(
            onDismissRequest = { showSupplierStatementDialog = false },
            title = {
                Text(
                    text = "كشف حساب المورد التفصيلي",
                    color = PalWhitePure,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تفاصيل كافة حركات التوريد والتسديد وقيمتها بالعملية والتاريخ وبالتأصيل للمورد: ${selectedSupplierForStatement!!.name}",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right
                    )

                    // Period Toggles
                    val periodsToggle = listOf("يومي", "اسبوعي", "شهري", "سنوي", "تقويم مخصص", "الكل")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        periodsToggle.forEach { period ->
                            val isSel = selectedSupplierStatementPeriod == period
                            val bg = if (isSel) PalGreenNormal else PalBlackLight
                            val fg = if (isSel) Color.White else Color.Gray
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable { selectedSupplierStatementPeriod = period }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (period) {
                                        "يومي" -> "يومي"
                                        "اسبوعي" -> "أسبوعي"
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

                    if (selectedSupplierStatementPeriod == "تقويم مخصص") {
                        UnifiedPeriodSelector(
                            initialType = if (customSupplierStatementType == "date") "يوم" else if (customSupplierStatementType == "month") "شهر" else "سنة",
                            initialValue = customSupplierStatementValue.ifEmpty { Helpers.getCurrentDate() },
                            onPeriodChanged = { type, formattedValue ->
                                customSupplierStatementType = when (type) {
                                    "يوم" -> "date"
                                    "شهر" -> "month"
                                    "سنة" -> "year"
                                    else -> "date"
                                }
                                customSupplierStatementValue = formattedValue
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = supplierReportDate,
                        onValueChange = { supplierReportDate = it },
                        label = { Text("تعيين وتخصيص تاريخ السند المطبوع", color = PalWhiteMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("supplier_statement_date_input"),
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

                    // Detailed Transactions View Block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(PalBlackDark, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (filteredStatementPurchases.isEmpty()) {
                                Text(
                                    text = "لا توجد معاملات شراء أو توريد بضاعة مسجلة خلال الفترة المحددة.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                filteredStatementPurchases.forEach { purchase ->
                                    val isReturn = purchase.totalAmount < 0 || purchase.paymentMethod.startsWith("مرتجع")
                                    val typeStr = if (isReturn) "🔄 سند مرتجع بضاعة" else "📥 سند توريد"
                                    val absTotal = java.lang.Math.abs(purchase.totalAmount)
                                    val absPaid = java.lang.Math.abs(purchase.paidAmount)
                                    val absDebt = java.lang.Math.abs(purchase.debtRemaining)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(PalBlackLight, RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("$typeStr #${purchase.id}", color = if (isReturn) PalGoldCalligraphy else PalWhitePure, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(purchase.date, color = PalWhiteMuted, fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("البيان: ${purchase.paymentMethod}", color = PalWhiteSoft, fontSize = 10.sp)
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        editingPurchaseId = purchase.id
                                                        selectedSupplierForPurchase = suppliersList.find { it.id == purchase.supplierId }
                                                        val itemsForThisPurchase = allPurchaseItems.filter { it.purchaseId == purchase.id }
                                                        val loadedItems = itemsForThisPurchase.map { pi ->
                                                            val invItem = inventoryItems.find { it.id == pi.itemId } ?: com.example.data.InventoryItem(id = pi.itemId, name = pi.itemName, quantity = pi.quantity, buyPrice = pi.unitPrice, sellPrice = 0.0, lowStockThreshold = 5.0)
                                                            Triple(invItem, pi.quantity, pi.unitPrice)
                                                        }
                                                        newPurchaseItems.clear()
                                                        newPurchaseItems.addAll(loadedItems)
                                                        paidAmountInput = purchase.paidAmount.toString()
                                                        purchasePaymentMethod = purchase.paymentMethod
                                                        deedDateInput = purchase.date
                                                        isAddingPurchase = true
                                                        showSupplierStatementDialog = false
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = PalGreenLight, modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = {
                                                        purchaseToVoid = purchase
                                                        showVoidConfirm = true
                                                        showSupplierStatementDialog = false
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = PalRedLight, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("القيمة الكلية: ${Helpers.formatMoney(absTotal)}", color = PalWhiteSoft, fontSize = 10.sp)
                                            Text("المسدد/المسترد: ${Helpers.formatMoney(absPaid)}", color = PalGreenLight, fontSize = 10.sp)
                                        }
                                        val items = allPurchaseItems.filter { it.purchaseId == purchase.id }
                                        if (items.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            items.forEach { item ->
                                                val absQty = java.lang.Math.abs(item.quantity)
                                                Text("   👈 ${item.itemName} (${absQty.toInt()} حبة • بسعر: ${Helpers.formatMoney(item.unitPrice)})", color = PalWhitePure, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val tPrefix = when (selectedSupplierStatementPeriod) {
                                    "يومي" -> "تقرير يومي"
                                    "شهري" -> "تقرير شهري"
                                    "سنوي" -> "تقرير سنوي"
                                    "تقويم مخصص" -> "القرير المخصص لفترة ($customSupplierStatementValue)"
                                    else -> "تقرير دوري"
                                }
                                val headers = listOf("التاريخ والسند", "نوع العملية وتفاصيل الأصناف والبيان", "القيمة الكلية", "المسدد/المسترد", "الآجل المتبقي")
                                val rows = filteredStatementPurchases.map { purchase ->
                                    val isReturn = purchase.totalAmount < 0 || purchase.paymentMethod.startsWith("مرتجع")
                                    val typeStr = if (isReturn) "🔄 مرتجع" else "📥 توريد"
                                    val absTotal = java.lang.Math.abs(purchase.totalAmount)
                                    val absPaid = java.lang.Math.abs(purchase.paidAmount)
                                    val absDebt = java.lang.Math.abs(purchase.debtRemaining)
                                    
                                    val items = allPurchaseItems.filter { it.purchaseId == purchase.id }
                                    val itemsText = if (items.isNotEmpty()) {
                                        "\n الأصناف: " + items.joinToString(" | ") { "${it.itemName} (${java.lang.Math.abs(it.quantity).toInt()} حبة • بسعر: ${Helpers.formatMoney(it.unitPrice)})" }
                                    } else ""
                                    
                                    listOf(
                                        purchase.date,
                                        "$typeStr #${purchase.id} - البيان: ${purchase.paymentMethod}$itemsText",
                                        Helpers.formatMoney(absTotal),
                                        Helpers.formatMoney(absPaid),
                                        Helpers.formatMoney(absDebt)
                                    )
                                }
                                Helpers.generatePdfAndShare(
                                    context = context,
                                    title = "$tPrefix كشف حساب المورد التفصيلي: ${selectedSupplierForStatement!!.name}",
                                    headers = headers,
                                    rows = rows,
                                    totals = mapOf(
                                        "إجمالي قيمة المشتريات بالفترة" to Helpers.formatMoney(totalPurchasedVal),
                                        "إجمالي المدفوعات والمسدد نقداً بالفترة" to Helpers.formatMoney(totalPaidVal),
                                        "رصيد المديونية الكلي المستحق له حالياً" to Helpers.formatMoney(totalOutstandingVal)
                                    ),
                                    customDate = supplierReportDate
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalRedNormal),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("PDF", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                Helpers.shareViaWhatsApp(context, selectedSupplierForStatement!!.phone, statementMsg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("واتساب", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                Helpers.shareViaSMS(context, selectedSupplierForStatement!!.phone, statementMsg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("SMS", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                bluetoothPrintText = statementMsg
                                showBluetoothPrintTrigger = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalGoldCalligraphy),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("طباعة 🖨️", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupplierStatementDialog = false }) {
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

    if (showAddReturnDialog && selectedSupplierForReturn != null) {
        AlertDialog(
            onDismissRequest = { showAddReturnDialog = false },
            title = {
                Text(
                    text = "تسجيل مرتجع بضاعة للمورد: ${selectedSupplierForReturn!!.name}",
                    color = PalWhitePure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
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
                    Text(
                        text = "قم بتحديد الصنف والكمية المرتجعة والمبلغ المسترد نقداً إن وجد لتحديث حساب المورد والمخزون بدقة:",
                        color = PalWhiteSoft,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right
                    )

                    // 1. Product selection dropdown
                    var showProductDropdown by remember { mutableStateOf(false) }
                    Text("الصنف المراد إرجاعه:", color = PalWhiteMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showProductDropdown = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PalWhitePure),
                            border = BorderStroke(1.dp, PalBlackLight),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedItemForReturn?.name ?: "اضغط لاختيار الصنف",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedItemForReturn != null) PalGreenLight else Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showProductDropdown,
                            onDismissRequest = { showProductDropdown = false },
                            modifier = Modifier.background(PalBlackDark).fillMaxWidth(0.9f)
                        ) {
                            inventoryItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.name} (المتوفر: ${item.quantity.toInt()} حبة)", color = Color.White, fontSize = 12.sp) },
                                    onClick = {
                                        selectedItemForReturn = item
                                        returnedPriceInput = item.buyPrice.toString()
                                        showProductDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Quantity Input
                    OutlinedTextField(
                        value = returnedQtyInput,
                        onValueChange = { returnedQtyInput = it },
                        label = { Text("الكمية المرتجعة", color = PalWhiteMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            focusedContainerColor = PalBlackDark,
                            unfocusedContainerColor = PalBlackDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // 3. Price per Unit Input
                    OutlinedTextField(
                        value = returnedPriceInput,
                        onValueChange = { returnedPriceInput = it },
                        label = { Text("سعر الإرجاع للوحدة (ريال)", color = PalWhiteMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            focusedContainerColor = PalBlackDark,
                            unfocusedContainerColor = PalBlackDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // 4. Refunded Cash (if any)
                    OutlinedTextField(
                        value = refundedAmountInput,
                        onValueChange = { refundedAmountInput = it },
                        label = { Text("المبلغ المسترد نقداً (اختياري - افتراضي: 0)", color = PalWhiteMuted, fontSize = 11.sp) },
                        placeholder = { Text("0", color = Color.Gray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            focusedContainerColor = PalBlackDark,
                            unfocusedContainerColor = PalBlackDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // 5. Date Input
                    OutlinedTextField(
                        value = returnDateInput,
                        onValueChange = { returnDateInput = it },
                        label = { Text("تاريخ ووقت الإرجاع", color = PalWhiteMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PalGreenLight) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            focusedContainerColor = PalBlackDark,
                            unfocusedContainerColor = PalBlackDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // 6. Notes Input
                    OutlinedTextField(
                        value = returnNotesInput,
                        onValueChange = { returnNotesInput = it },
                        label = { Text("بيان أو ملاحظات المرتجع", color = PalWhiteMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            focusedContainerColor = PalBlackDark,
                            unfocusedContainerColor = PalBlackDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val product = selectedItemForReturn
                        val qty = returnedQtyInput.toDoubleOrNull() ?: 0.0
                        val price = returnedPriceInput.toDoubleOrNull() ?: 0.0
                        val refund = refundedAmountInput.toDoubleOrNull() ?: 0.0

                        if (product == null) {
                            android.widget.Toast.makeText(context, "الرجاء اختيار صنف مرتجع أولاً!", android.widget.Toast.LENGTH_SHORT).show()
                        } else if (qty <= 0.0) {
                            android.widget.Toast.makeText(context, "يرجى تحديد كمية إرجاع صحيحة تزيد عن الصفر!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.executeSupplierReturn(
                                supplierId = selectedSupplierForReturn!!.id,
                                itemId = product.id,
                                itemName = product.name,
                                returnedQty = qty,
                                returnedPrice = price,
                                returnDate = returnDateInput.ifEmpty { Helpers.getCurrentDateTime() },
                                refundedAmount = refund,
                                notes = returnNotesInput
                            )
                            android.widget.Toast.makeText(context, "تم تسجيل المرتجع بنجاح وتحديث الحساب والمخزن!", android.widget.Toast.LENGTH_SHORT).show()
                            showAddReturnDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
                ) {
                    Text("حفظ المرتجع", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddReturnDialog = false }) {
                    Text("إلغاء", color = PalWhiteMuted)
                }
            },
            containerColor = PalBlackNormal
        )
    }
}
