# Software Requirements Specification (SRS)
## Tofan Al-Aqsa Accounting & Supplier Management System (v3.5.0)
### Designed for وكالة عاهد الصبري لتجارة القات الماوية والورزانية

---

## 1. Document Control & Metadata
*   **System Name:** Tofan Al-Aqsa Accounting & Supplier Management System
*   **Target Client:** وكالة عاهد الصبري (Ahed Al-Sabri Agency)
*   **Version:** 3.5.0 (Enhanced HD Edition)
*   **Platform:** Native Android Application (Jetpack Compose, Room Database, MVVM Architecture)
*   **Primary Language:** Arabic (Native RTL support, typography aligning, and high-fidelity rendering)
*   **Lead Architect/Developer Signature:** المهندس محمد امين ردمان عبدالله الحاج (+967780961823)

---

## 2. System Architecture & Tech Stack

### 2.1 MVVM Clean Architecture
The application runs entirely offline-first, implementing the Model-View-ViewModel-Repository architecture pattern:
*   **UI Layer (Views):** Jetpack Compose Declarative layouts dynamically consuming and observing UI states.
*   **Logic Layer (ViewModel):** `AppViewModel.kt` utilizing Kotlin coroutines, `StateFlow`, and `MutableStateFlow` structures for safe reactive rendering.
*   **Repository Layer:** `QatRepository.kt` abstracts the database queries behind clean interfaces, caching active memory streams, and acting as the single source of truth.
*   **Database Layer:** Core SQLite managed through Android's Room ORM with full Transactions Support and structured database checkpoints.

### 2.2 Design System & Theming Tokens
The system follows a polished, high-contrast Pitch-Black and Gold calligraphy branding identity.

```kotlin
val PalBlackDark = Color(0xFF0C0C0E)     // Depths background of the entire app
val PalBlackNormal = Color(0xFF141416)   // Raised cards, panels, and dialogues
val PalBlackLight = Color(0xFF1F1F23)    // Input fields, dividers, and active selections

val PalWhitePure = Color(0xFFFFFFFF)     // Dominant text and headers
val PalWhiteSoft = Color(0xFFE2E2E8)     // Sub-titles, tables and dynamic items
val PalWhiteMuted = Color(0xFF8C8C9A)    // Secondary details, timelines and timestamps

val PalGreenNormal = Color(0xFF1B5E20)   // Primary buttons, successes, and credit indicators
val PalGreenLight = Color(0xFF2E7D32)    // Secondary successes, badges, and high-contrast gains
val PalGreenDark = Color(0xFF0D5315)     // Deep card backgrounds and banners

val PalGoldCalligraphy = Color(0xFFD4AF37) // Premium highlight color (Brand Identity)
val PalRedNormal = Color(0xFFCE2029)     // Secondary buttons, destructive alerts, and outflows
val PalRedLight = Color(0xFFFF5252)      // Debt warnings, liabilities, and losses
```

---

## 3. Data Models (Room Database Schema)

### 3.1 `Customer` (زبائن الوكالة)
Defines the client profile tracking overall accounts.
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `name`: `String` (Customer name, Unique Index)
*   `phone`: `String`
*   `notes`: `String`
*   `initialDebt`: `Double` (Beginning outstanding debit balance)
*   `totalPaid`: `Double` (Accumulated cash repayments)

### 3.2 `Supplier` (الموردين والمزارعين)
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `name`: `String` (Supplier name, Unique Index)
*   `phone`: `String`
*   `notes`: `String`
*   `initialBalance`: `Double` (Beginning credit balance owed to them)
*   `totalPurchased`: `Double` (Cumulative purchase costs)
*   `totalPaid`: `Double` (Cumulative payments rendered to supplier)

### 3.3 `TaxEntry` (سجل الرسوم والضرائب والخرجات اليومية)
Daily fixed/running state costs assigned per day.
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `date`: `String` (Format: `yyyy-MM-dd`)
*   `taxAmount`: `Double` (ضريبة القات اليومية)
*   `stallOutflow`: `Double` (مصاريف المفرش الصاردة)
*   `laborOutflow`: `Double` (خرجة العمال اليومية)
*   `notes`: `String`

### 3.4 `InventoryItem` (منتجات ومخازن الوكالة)
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `name`: `String` (Item descriptor e.g., ماوية ممتاز, ورزاني)
*   `dateAdded`: `String`
*   `quantity`: `Double` (Packs/quantities remaining in stock)
*   `purchasePrice`: `Double` (Base wholesale purchase price)

### 3.5 `SalesInvoice` (سجل فواتير مبيعات الزبائن)
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `date`: `String` (Timestamp format: `yyyy-MM-dd HH:mm`)
*   `customerName`: `String`
*   `customerId`: `Int` (Foreign Key tracking)
*   `totalAmount`: `Double` (Total invoice value prior to discounts)
*   `discount`: `Double` (Financial reduction applied)
*   `paidAmount`: `Double` (Repayments collected during checkout)
*   `debtAmount`: `Double` (Debit balance left on account: `totalAmount - discount - paidAmount`)
*   `paymentMethod`: `String` (`نقداً`, `أجل`, `تحويل`, `إيداع`, `دفع جزئي`)

### 3.6 `SalesInvoiceItem` (تفاصيل أصناف فواتير البيع)
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `invoiceId`: `Int` (Foreign Key matching `SalesInvoice`)
*   `itemName`: `String`
*   `quantity`: `Double`
*   `unitPrice`: `Double` (Sells rate)

### 3.7 `SupplierPurchase` (سجلات توريدات المزارعين المشتراة)
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `date`: `String` (Timestamp format: `yyyy-MM-dd HH:mm`)
*   `supplierId`: `Int` (Foreign Key matching `Supplier`)
*   `supplierName`: `String`
*   `totalAmount`: `Double` (Gross purchase order worth)
*   `paidAmount`: `Double` (Immediate cash/transfer payments)
*   `debtRemaining`: `Double` (Outstanding liabilities: `totalAmount - paidAmount`)
*   `paymentMethod`: `String` (`نقداً` or `على الحساب`)

### 3.8 `SupplierPurchaseItem` (أصناف بضائع أوامر التوريد)
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `purchaseId`: `Int` (Foreign Key matching `SupplierPurchase`)
*   `itemName`: `String`
*   `quantity`: `Double`
*   `unitPrice`: `Double`

### 3.9 `Expense` (المصروفات التشغيلية والعمومية)
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `date`: `String` (Timestamp format)
*   `category`: `String` (`نقل ومواصلات`, `إيجار المحل والجمارك`, `تعبئة وتغليف وعمالة`, `خدمات وتبرعات وطاقة`, `مصاريف عامة أخرى`)
*   `amount`: `Double`
*   `notes`: `String`

### 3.10 `MoneyTransfer` (حوالات مالية صادرة)
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `date`: `String`
*   `sender`: `String`
*   `receiver`: `String`
*   `status`: `String`
*   `amount`: `Double`
*   `referenceNumber`: `String`
*   `statement`: `String` (Purpose of remittance)
*   `notes`: `String`
*   `currency`: `String` (`ريال يمني`, `ريال سعودي`, `دولار أمريكي`)

### 3.11 `CustomerPayment` (تحصيل أقساط ديون العملاء)
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `customerId`: `Int`
*   `date`: `String`
*   `amount`: `Double` (Paid amount value)
*   `paymentMethod`: `String`
*   `referenceNumber`: `String`
*   `notes`: `String`

---

## 4. Architectural Data Access (DAOs Core Queries)

Data consistency is enforced through specific Data Access Objects APIs:
*   **Transactions Block Check:** Every multi-row balance reconciliation (e.g., creating checkouts, settling payments, purging states) runs wrapped inside Room's `@Transaction` annotation ensuring failure rollback.
*   **Observer Flowability:** All listing queries return `Flow<List<Entity>>` providing instant UI re-renders on record changes.

### Key Queries Excerpt
*   **Customer Statement:**
    ```sql
    @Query("SELECT * FROM Customer ORDER BY id DESC")
    fun getAllCustomersFlow(): Flow<List<Customer>>
    ```
*   **Invoice Detail Construction:**
    ```sql
    @Query("SELECT * FROM SalesInvoiceItem WHERE invoiceId = :invId")
    suspend fun getItemsForInvoice(invId: Int): List<SalesInvoiceItem>
    ```
*   **Custom Period Inventory movement Tracking:**
    ```sql
    @Query("SELECT * FROM SalesInvoiceItem")
    suspend fun getAllInvoiceItems(): List<SalesInvoiceItem>
    ```

---

## 5. Core Mathematical Formulas & Calculations

### 5.1 Financial Safe Balance (صافي النقدية الفعلية بالخزينة)
Tracks the true liquidity matching physical coins, bills, bank positions, and exchange balances of the agency:

$$\text{Net Safes Cash} = (\text{Total Sales Cash Received} + \text{Total Repayments Collected}) - (\text{Total Supplier Cash Paid} + \text{Total Operating Expenses} + \text{Daily Taxes} + \text{Daily Stall Outflow} + \text{Daily Labor Outflow} + \text{Capped Outflow Remittances})$$

*Where:*
*   `Sales Cash Received` comprises all checkouts carrying `PaymentMethod` as `نقداً`, `تحويل`, `إيداع`, plus the `paidAmount` of `دفع جزئي`.
*   `Supplier Cash Paid` tracks actual disbursements matched on order checkout.

### 5.2 Net Realized Agency Profit (صافي الربح التجاري الفعلي للوكالة)
Tracks trading profitability matching cash inflows against costs without penalizing safe balances for bank transfers or cash balance exports:

$$\text{Net Realized Profit} = (\text{Wholesale Sales Cash Inflows} + \text{Debt Collections}) - (\text{Purchase Costs Rendered} + \text{Operating Expenses} + \text{Daily Tax} + \text{Daily Stall} + \text{Daily Labor Outflow})$$

### 5.3 Customer & Supplier Debt Reconciliation
*   `Customer Outstanding Balance` = `initialDebt + Cumulative Sales Invoices debtAmount - Cumulative Customer Repayment amount`
*   `Supplier Outstanding Liabilities` = `initialBalance + Cumulative Supplier Purchases debtRemaining - Cumulative payments rendered`

### 5.4 Performance Scaling (Stress Testing Load Simulator)
The system contains an automated stress evaluation tool generating **1,500 highly connected operations** distributed across consecutive retro-dates:
*   Creates **150** realistic customer records.
*   Simulates **800** detailed transactions (with varying invoice items, discounts, partial pay allocations, cash logs, and credit registers).
*   Simulates corresponding tax items, wages, transport expenditures, and money transfers.
*   Validates zero performance latency, instant querying indices, and real-time canvas chart sorting speeds.

---

## 6. Functional & Interface View Specifications

### 6.1 Screen 1: Splash Screen
*   **UI Layout:** Fullscreen minimalist black background featuring a centered corporate emblem of `Ahed Al-Sabri Agency` encased within rotating gold margins with glowing pulsing animations.
*   **Interactive Logic:** Plays a fade-in sequence, checks `isAppLockEnabled` state, and automatically navigates after 2500ms.
    *   *Branch:* If lock is active, navigates to **Login Screen**.
    *   *Branch:* If lock is disabled, routes directly to **Dashboard Screen**.

### 6.2 Screen 2: Login Authenticator (شاشة المرور وحماية النظام)
*   **UI Layout:** Centered passcode utility carrying 4 PIN circles.
*   **Interactive Logic:** Verifies PIN entering. Hardcoded default system admin passcode remains `1234`.
*   **Actions:** Wrong attempt displays a high-contrast red error shaking badge. Successful entry unlocks the state.

### 6.3 Screen 3: Main Dashboard (شاشة التحكم والمؤشرات الوطنية)
*   **UI Layout:** Comprehensive dashboard panel with KPI highlights.
    *   Header highlighting agency details.
    *   Top stats cards carrying: **Total Revenue**, **Net Realized Profit (Gold)**, **Outstanding customer Debts (Warning Red)**, and **Internal Stock items**.
    *   Dynamic canvas-rendered graph chart drawing smooth financial lines and trends for sales versus outgoing expenditures.
    *   Scrollable grid layout featuring quick navigation paths.
*   **Dialogs:**
    *   *Daily Tax Input Dialog:* Prompts the manager every morning or at checkout to define the daily expenses (ضريبة القات اليومية, خرج المفرش, خرجة العمال).

### 6.4 Screen 4: Stock & Warehouse Inventory (قسم مخازن وأصناف البضاعة)
*   **UI Layout:** Grid containing stock cards. Standard items include `ماوية ممتاز`, `ورزاني فاخر`, `طسوس`, `قطاف`.
*   **Interactive Controls:** Search box, "Add Stock Item" CTA.
*   **Actions & Operations:** Clicking any item launches the "Product Details & Modifiers Dialogue" enabling edits, price updates, or complete removal.

### 6.5 Screen 5: Sales Checkout Terminal (شاشة كاشير فواتير البيع المحسوبة)
*   **UI Layout:** Split layout tracking cashiers. Left panels include customer select, payment term toggles, discount entries, and checkout items cart list.
*   **Workflow Logic:**
    *   Select customer from dynamic dropdown or write name.
    *   Select item, quantity, unit price click "Add to Billing list".
    *   Adjust discount values or split cash/partial payments.
*   **Dialogs:** Settle order dialog, print and whatsapp sharing shortcuts.

### 6.6 Screen 6: General Accounts Ledger (التحاسب التجاري والتحليل المحاسبي)
*   **UI Layout:** Scrollable accounting matrix showing Period Tabs (يومية, أسبوعية, شهرية, سنوية, تقويم مخصص).
*   **Calculations:** Computes cumulative sales, purchase liabilities, margins, taxes, expenses, and renders the absolute net profits.
*   **Export:** Prints dynamic accounting statements in A4 PDF format.

### 6.7 Screen 7: Customer Relationship Ledger (حسابات الديون والزبائن والتسديدات)
*   **UI Layout:** Client list with high-contrast indicator flags matching overdue balances.
*   **Actions:** Add customer, track aging debt, register customer repayments, and download the account statements.

### 6.8 Screen 8: Supplier Accounts Panel (مشتريات وحسابات الموردين والمزارعين)
*   **UI Layout:** Accounts ledger mapping wholesale purchase orders, paid installments, and liabilities left to clear.
*   **Export:** Prints wholesale invoice tracking details or complete statements.

### 6.9 Screen 9: Expense Ledger (قسم تقييد ومشتريات المصروفات)
*   **UI Layout:** Category-segmented cost entries tracking running costs.
*   **Workflow:** Renders expense lists matching transportation, customs, packing supplies, and general overhead.

### 6.10 Screen 10: Currency Money Transfers (سجل حركات الحوالات المالية)
*   **UI Layout:** Remittance ledger detailing money transfers out of the safes, custom reference validation fields, and multi-currency tracking (Saudi Riyal, US Dollar, Yemeni Riyal).

### 6.11 Screen 11: Business Intelligence Reports (قسم التحاليل والبيانات الإحصائية)
*   **UI Layout:** Graphical charts presenting spending-to-income ratios, with detailed timeline exports.

### 6.12 Screen 12: Archives Vault (أرشيف النظام التفصيلي)
*   **UI Layout:** Universal searchable and filterable ledger containing every transaction type of the agency. Renders chronological invoice, purchase, expenditure, repayment, and remittance flows.

### 6.13 Screen 13: System Preferences & Tools (إعدادات النظام المحاسبي للوكالة)
*   **UI Layout:** Config preference panel containing:
    *   Branding Header: Pulsing interactive agency logo.
    *   Administrative configurations (Toggle Admin Mode PIN, change passcodes).
    *   Bluetooth Thermal Printers connector (Supports 58mm/80mm layout adjustments).
    *   Database Local Backup Checkpoint Manager: Force writes WAL back to SQLite master DB, exports backups, restores database from external backups, and triggers the stress loading panel.

---

## 7. Printing & Document Share Specifications

The system implements strict high-fidelity document workflows matching regulatory requirements:

### 7.1 Modern Arabic PDF Generator & Native OS Print Spooling
Renders beautiful tables, headings, and margins directly to A4 format using Android's native graphics Canvas:
*   **RTL Text Layout Wrapping:** Since Arabic is written from right to left, the system utilizes custom text splitting algorithm and paint measures to wrap long descriptions dynamically, resolving clipping issues in standard layout engines.
*   **Clean Structural Elements:** Generates corporate brand headers, logo graphic bitmap bindings, customizable page indicators, itemized table rows, alternating grid highlights, and a signature double-border box enclosing total sums at the bottom of the last page.
*   **Native AirPrint Spooling:** Sends the calculated PDF stream to standard desktop printer networks directly. Renders a sharing intent as a fall back if print systems are missing.

### 7.2 Core Bluetooth Thermal Printer Integration
Integrated directly within the receipts workflow:
*   **RTL Alignment Formatting:** Converts invoice contents, details, totals, and promotional footer slogans into neatly formatted text strings.
*   **Adjustable Width Layout:** Detects and applies correct line characters (32 chars for 58mm systems, 48 chars for 80mm cashier terminals), applying symmetrical spacers and dynamic hyphen borders.
*   **Virtual Print Emulation:** Renders an on-screen preview dialog detailing the final print output prior to physical sheet generation.

---

## 8. Database Reliability & Fault-Tolerance Policy

*   **WAL Mode Checkpoint Control:** Normal SQLite operations write logs to `-wal` and `-shm` temporary files. During administrative local backup generation, the system initiates a checkpoint execution (`PRAGMA wal_checkpoint(FULL)`) pushing all cached inputs back into the main SQLite database before sharing or duplicating base files.
*   **Safety Backup Retention:** Saves backups inside protected device directories (`tofan_backups`) and provides external restoration paths to prevent complete data loss.

---

### Software Specification Document Complete (v3.5.0)
*Prepared and certified dynamically to retain full architectural compliance.*
