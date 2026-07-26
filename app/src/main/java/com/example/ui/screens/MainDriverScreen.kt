package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.OrderWithItems
import com.example.ui.components.BarcodeScannerModal
import com.example.ui.components.PrinterDeviceDialog
import com.example.ui.components.RackAssignmentDialog
import com.example.ui.components.SettlementDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.DriverViewModel
import com.example.utils.FarsiUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDriverScreen(viewModel: DriverViewModel) {
    val context = LocalContext.current

    val orders by viewModel.ordersList.collectAsState()
    val selectedOrder by viewModel.selectedOrder.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isGpsActive by viewModel.isGpsActive.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncToastMessage.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val recentGpsLogs by viewModel.gpsLogs.collectAsState()

    val showScannerDialog by viewModel.showScannerDialog.collectAsState()
    val scanStage by viewModel.scanStage.collectAsState()

    val connectedPrinter by viewModel.connectedPrinter.collectAsState()
    val availablePrinters by viewModel.availablePrinters.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()

    var showPrinterDialog by remember { mutableStateOf(false) }
    var rackDialogOrderId by remember { mutableStateOf<String?>(null) }
    var settlementOrder by remember { mutableStateOf<OrderWithItems?>(null) }

    LaunchedEffect(syncMessage) {
        syncMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearToastMessage()
        }
    }

    if (showPrinterDialog) {
        PrinterDeviceDialog(
            connectedPrinter = connectedPrinter,
            availablePrinters = availablePrinters,
            onScan = { viewModel.scanBluetoothPrinters(context) },
            onConnect = { viewModel.connectPrinter(it) },
            onDisconnect = { /* handled in manager */ },
            onDismiss = { showPrinterDialog = false }
        )
    }

    if (rackDialogOrderId != null) {
        val currentOrder = orders.find { it.order.id == rackDialogOrderId }
        RackAssignmentDialog(
            orderId = rackDialogOrderId!!,
            currentRackCode = currentOrder?.order?.rackCode ?: "",
            onDismiss = { rackDialogOrderId = null },
            onConfirm = { rackCode ->
                viewModel.assignRackCode(rackDialogOrderId!!, rackCode)
                rackDialogOrderId = null
            }
        )
    }

    if (settlementOrder != null) {
        SettlementDialog(
            orderWithItems = settlementOrder!!,
            onDismiss = { settlementOrder = null },
            onConfirmSettlement = { paid, discount, method, print ->
                val ord = settlementOrder!!
                viewModel.settlePayment(ord.order.id, paid, discount, method)
                if (print) {
                    viewModel.printOrderReceipt("رسید تسویه حساب و تحویل فرش", ord, method)
                }
                settlementOrder = null
            }
        )
    }

    if (showScannerDialog) {
        BarcodeScannerModal(
            expectedOrder = selectedOrder,
            allOrders = orders,
            scanStage = scanStage,
            onDismiss = { viewModel.closeScanner() },
            onConfirmVerification = { success -> viewModel.handleScanSuccess(success) },
            onReportMismatchToDispatch = { alertText -> viewModel.reportScanMismatchToDispatch(alertText) }
        )
    }

    ZomorrodDriverTheme(darkTheme = isDarkMode) {
        Scaffold(
            topBar = {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(CleanBluePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ع",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "علی‌اکبر خسروی",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(CleanTealAccent)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "راننده فعال • کد ۴۰۸",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            // Active status pill badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CleanBlueContainer)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(CleanTealAccent)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "مسیریابی",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanBluePrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Barcode/QR Scanner button
                            IconButton(onClick = { viewModel.openScanner(com.example.data.model.ScanStage.DELIVERY) }) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "اسکن بارکد / QR کد",
                                    tint = CleanBluePrimary
                                )
                            }

                            // Bluetooth printer status badge
                            IconButton(onClick = {
                                viewModel.scanBluetoothPrinters(context)
                                showPrinterDialog = true
                            }) {
                                BadgedBox(
                                    badge = {
                                        if (connectedPrinter != null) {
                                            Badge(containerColor = CleanTealAccent)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = "پرینتر حرارتی")
                                }
                            }

                            // Sync button
                            IconButton(onClick = { viewModel.syncWithWebPanel() }) {
                                BadgedBox(
                                    badge = {
                                        if (unsyncedCount > 0) {
                                            Badge { Text(unsyncedCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = "همگام‌سازی",
                                        tint = if (isSyncing) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                    )
                                }
                            }

                            // Theme switch
                            IconButton(onClick = { viewModel.toggleDarkMode() }) {
                                Icon(
                                    if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "تغییر پوسته"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            bottomBar = {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent
                    ) {
                        NavigationBarItem(
                            selected = activeTab == 0,
                            onClick = { viewModel.setActiveTab(0) },
                            icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                            label = { Text("تحویل", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = activeTab == 1,
                            onClick = { viewModel.setActiveTab(1) },
                            icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                            label = { Text("جمع‌آوری", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = activeTab == 2,
                            onClick = { viewModel.setActiveTab(2) },
                            icon = { Icon(Icons.Default.Warehouse, contentDescription = null) },
                            label = { Text("تحویل انبار", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = activeTab == 3,
                            onClick = { viewModel.setActiveTab(3) },
                            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                            label = { Text("تسویه", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = activeTab == 4,
                            onClick = { viewModel.setActiveTab(4) },
                            icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                            label = { Text("پشتیبانی", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (activeTab) {
                    0 -> DeliveryRouteScreen(
                        orders = orders,
                        onSelectOrderForSettlement = { orderWithItems ->
                            viewModel.selectOrder(orderWithItems.order.id)
                            viewModel.setActiveTab(3)
                        },
                        onOpenScanner = { orderId ->
                            viewModel.openScanner(com.example.data.model.ScanStage.DELIVERY, orderId)
                        }
                    )
                    1 -> CollectionRouteScreen(
                        orders = orders,
                        onSelectOrderForInvoice = { orderWithItems ->
                            viewModel.selectOrder(orderWithItems.order.id)
                            viewModel.setActiveTab(99)
                        }
                    )
                    2 -> WarehouseHandoverScreen(
                        orders = orders,
                        onConfirmWarehouseHandover = { orderId, rackCode ->
                            viewModel.confirmWarehouseHandover(orderId, rackCode)
                        },
                        onPrintWarehouseReceipt = { orderWithItems ->
                            viewModel.printOrderReceipt("رسید تحویل و نگهداری انباردار", orderWithItems)
                        },
                        onOpenScanner = { targetId ->
                            viewModel.openScanner(com.example.data.model.ScanStage.WORKSHOP, targetId)
                        }
                    )
                    3 -> DeliverySettlementScreen(
                        orders = orders,
                        onSettlePayment = { id, paid, discount, method ->
                            viewModel.settlePayment(id, paid, discount, method)
                        },
                        onPrintReceipt = { orderWithItems, method ->
                            viewModel.printOrderReceipt("رسید تسویه حساب و تحویل فرش", orderWithItems, method)
                        },
                        onOpenScanner = { targetId ->
                            viewModel.openScanner(com.example.data.model.ScanStage.DELIVERY, targetId)
                        },
                        onSettleWithOffice = {
                            viewModel.settleWithOffice()
                        }
                    )
                    4 -> DispatchChatScreen(
                        messages = chatMessages,
                        onSendMessage = { text -> viewModel.sendChatMessage(text) }
                    )
                    5 -> GpsTrackingScreen(
                        isGpsActive = isGpsActive,
                        unsyncedCount = unsyncedCount,
                        isSyncing = isSyncing,
                        recentGpsLogs = recentGpsLogs,
                        onToggleGps = { viewModel.toggleGpsTracking() },
                        onSyncNow = { viewModel.syncWithWebPanel() }
                    )
                    99 -> CarpetRegistrationScreen(
                        orderWithItems = selectedOrder,
                        isPrinting = isPrinting,
                        onBack = { viewModel.setActiveTab(1) },
                        onAddCarpetItem = { type, len, wid, price, servs, defs, notes, tag ->
                            selectedOrder?.let {
                                viewModel.addCarpetItem(it.order.id, type, len, wid, price, servs, defs, notes, tag)
                            }
                        },
                        onDeleteCarpetItem = { itemId ->
                            selectedOrder?.let {
                                viewModel.deleteCarpetItem(itemId, it.order.id)
                            }
                        },
                        onPrintReceipt = {
                            selectedOrder?.let {
                                viewModel.printOrderReceipt("پیش‌فاکتور اولیه دریافت فرش", it)
                            }
                        },
                        onProceedToWorkshop = {
                            selectedOrder?.let {
                                viewModel.finalizeInvoiceRegistration(it.order.id)
                            }
                        }
                    )
                }
            }
        }
    }
}
