package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.OrderWithItems
import com.example.data.model.ScanStage
import com.example.ui.components.BarcodeScannerModal
import com.example.ui.components.BarcodeView
import com.example.ui.theme.*
import com.example.utils.FarsiUtils
import com.example.utils.NavigationUtils

import com.example.ui.components.ReturnToCleanWarehouseDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeliveryRouteScreen(
    orders: List<OrderWithItems>,
    onSelectOrderForSettlement: (OrderWithItems) -> Unit,
    onOpenScanner: (orderId: String) -> Unit,
    onReturnToCleanWarehouse: (orderId: String, cleanRackCode: String, reason: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current

    // Filter orders ready for delivery or assigned/collected that are scheduled on map (excluding settled or returned orders)
    val deliveryOrders = orders.filter {
        (it.order.status == "READY_FOR_DELIVERY" ||
                it.order.status == "DELIVERED_TO_WORKSHOP" ||
                it.order.orderType == "DELIVERY") &&
                it.order.status != "DELIVERED_SETTLED" &&
                it.order.status != "OFFICE_SETTLED" &&
                it.order.status != "RETURNED_TO_CLEAN_WAREHOUSE"
    }

    // Selected order on map or from cards
    var selectedOrderForMap by remember(deliveryOrders) {
        mutableStateOf<OrderWithItems?>(deliveryOrders.firstOrNull())
    }

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, READY, WORKSHOP
    var searchQuery by remember { mutableStateOf("") }
    var showDeliverySearchScanner by remember { mutableStateOf(false) }
    var scanNoticeMessage by remember { mutableStateOf<String?>(null) }
    var orderForCleanWarehouseReturn by remember { mutableStateOf<OrderWithItems?>(null) }

    if (showDeliverySearchScanner) {
        BarcodeScannerModal(
            expectedOrder = null,
            allOrders = deliveryOrders,
            scanStage = ScanStage.DELIVERY,
            onDismiss = { showDeliverySearchScanner = false },
            onConfirmVerification = { result ->
                showDeliverySearchScanner = false
                val matchedOrder = result.orderWithItems
                searchQuery = matchedOrder.order.id
                onSelectOrderForSettlement(matchedOrder)
            },
            onReportMismatchToDispatch = { showDeliverySearchScanner = false }
        )
    }

    if (scanNoticeMessage != null) {
        AlertDialog(
            onDismissRequest = { scanNoticeMessage = null },
            confirmButton = {
                TextButton(onClick = { scanNoticeMessage = null }) {
                    Text("تأیید و بستن", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CleanBluePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نتیجه اسکن بارکد فاکتور", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = { Text(scanNoticeMessage!!, fontSize = 13.sp) },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (orderForCleanWarehouseReturn != null) {
        val target = orderForCleanWarehouseReturn!!
        ReturnToCleanWarehouseDialog(
            orderId = target.order.id,
            customerName = target.order.customerName,
            currentRackCode = target.order.rackCode,
            onDismiss = { orderForCleanWarehouseReturn = null },
            onConfirm = { cleanRackCode, reason ->
                onReturnToCleanWarehouse(target.order.id, cleanRackCode, reason)
                orderForCleanWarehouseReturn = null
            }
        )
    }

    val filteredOrders = deliveryOrders.filter { item ->
        val matchesSearch = item.order.customerName.contains(searchQuery, true) ||
                item.order.id.contains(searchQuery, true) ||
                item.order.rackCode.contains(searchQuery, true) ||
                item.items.any { it.barcodeTag.contains(searchQuery, true) }

        val matchesFilter = when (selectedFilter) {
            "READY" -> item.order.status == "READY_FOR_DELIVERY"
            "WORKSHOP" -> item.order.status == "DELIVERED_TO_WORKSHOP"
            else -> true
        }

        matchesSearch && matchesFilter
    }

    val readyCount = deliveryOrders.count { it.order.status == "READY_FOR_DELIVERY" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Header Banner: Neshan Delivery Map & Warehouse Loading
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CleanBlueContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = CleanBluePrimary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PinDrop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "نقشه نشان و مسیرهای تحویل پنل وب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CleanBluePrimary
                        )
                        Text(
                            text = "پین خودکار آدرس‌ها جهت بارگیری از قفسه و تحویل",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CleanBluePrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${FarsiUtils.toFarsiDigits(readyCount.toString())} آماده تحویل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanBluePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Section 1: Neshan Map View with Pins
        NeshanDeliveryMapView(
            orders = deliveryOrders,
            selectedOrder = selectedOrderForMap,
            onSelectOrder = { order -> selectedOrderForMap = order },
            onLaunchNeshanRoute = { order ->
                NavigationUtils.launchNeshan(context, order.order.latitude, order.order.longitude, order.order.address)
            },
            onLaunchAllRoute = {
                if (deliveryOrders.isNotEmpty()) {
                    val first = deliveryOrders.first()
                    NavigationUtils.launchNeshan(context, first.order.latitude, first.order.longitude, "مسیر کلی تحویل زمرد")
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search & Filter Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "فاکتورهای آماده تحویل & بارگیری انبار:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Quick Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("همه (${deliveryOrders.size})", fontSize = 10.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = selectedFilter == "READY",
                    onClick = { selectedFilter = "READY" },
                    label = { Text("آماده (${readyCount})", fontSize = 10.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Search Field & Barcode Scanner Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("جستجو در نام، کد قفسه یا بارکد فاکتور...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { showDeliverySearchScanner = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CleanBluePrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "اسکن بارکد فاکتور", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("اسکن فاکتور", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Cards List
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "هیچ فاکتوری برای تحویل یا بارگیری یافت نشد",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredOrders, key = { it.order.id }) { item ->
                    val isSelected = selectedOrderForMap?.order?.id == item.order.id
                    DeliveryReadyCard(
                        orderWithItems = item,
                        isSelected = isSelected,
                        onCardClick = { selectedOrderForMap = item },
                        onOpenScanner = { onOpenScanner(item.order.id) },
                        onNavigate = {
                            NavigationUtils.launchNeshan(context, item.order.latitude, item.order.longitude, item.order.address)
                        },
                        onProceedToSettlement = {
                            onSelectOrderForSettlement(item)
                        },
                        onReturnToCleanWarehouseClick = {
                            orderForCleanWarehouseReturn = item
                        }
                    )
                }
            }
        }
    }
}

/**
 * Interactive Neshan Styled Map Component with Pinned Route Stops & Warehouse Point.
 */
@Composable
private fun NeshanDeliveryMapView(
    orders: List<OrderWithItems>,
    selectedOrder: OrderWithItems?,
    onSelectOrder: (OrderWithItems) -> Unit,
    onLaunchNeshanRoute: (OrderWithItems) -> Unit,
    onLaunchAllRoute: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Map Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF212121))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        tint = Color(0xFF22C55E), // Neshan Green Accent
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "نقشه همراه نشان (مسیر پین شده تحویل)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF22C55E),
                    modifier = Modifier.clickable { onLaunchAllRoute() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "مسیریابی کل در نشان",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Canvas Map Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFFE5E5E0))
            ) {
                val neshanRoadColor = Color(0xFFFFFFFF)
                val mainRoadColor = Color(0xFFFDE047)
                val buildingColor = Color(0xFFD6D6CE)
                val routePathColor = Color(0xFF0288D1)

                // Render Map Grid & Route on Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw city background blocks
                    drawRoundRect(
                        color = buildingColor,
                        topLeft = Offset(w * 0.1f, h * 0.1f),
                        size = Size(w * 0.25f, h * 0.35f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = buildingColor,
                        topLeft = Offset(w * 0.45f, h * 0.15f),
                        size = Size(w * 0.45f, h * 0.25f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = buildingColor,
                        topLeft = Offset(w * 0.12f, h * 0.55f),
                        size = Size(w * 0.35f, h * 0.35f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = buildingColor,
                        topLeft = Offset(w * 0.55f, h * 0.52f),
                        size = Size(w * 0.35f, h * 0.38f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // 2. Draw Secondary Roads
                    val secondaryRoadStroke = Stroke(width = 12f)
                    drawLine(neshanRoadColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 14f)
                    drawLine(neshanRoadColor, Offset(0f, h * 0.7f), Offset(w, h * 0.7f), strokeWidth = 14f)
                    drawLine(neshanRoadColor, Offset(w * 0.4f, 0f), Offset(w * 0.4f, h), strokeWidth = 14f)

                    // 3. Draw Main Highway / Avenue (Yellow)
                    drawLine(mainRoadColor, Offset(0f, h * 0.48f), Offset(w, h * 0.48f), strokeWidth = 22f)
                    drawLine(mainRoadColor, Offset(w * 0.72f, 0f), Offset(w * 0.72f, h), strokeWidth = 22f)

                    // 4. Draw Route Line Connecting Warehouse -> Delivery Stops
                    val path = Path().apply {
                        moveTo(w * 0.15f, h * 0.8f) // Central Warehouse Point
                        lineTo(w * 0.4f, h * 0.48f)
                        lineTo(w * 0.65f, h * 0.3f)
                        lineTo(w * 0.82f, h * 0.65f)
                    }

                    drawPath(
                        path = path,
                        color = routePathColor,
                        style = Stroke(
                            width = 8f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                        )
                    )
                }

                // Map Pins Overlay (Warehouse + Customers)
                // Central Warehouse Pin
                Box(
                    modifier = Modifier
                        .offset(x = 24.dp, y = 110.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CleanPurpleAccent,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.Warehouse, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("انبار زمرد", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Customer Pins mapped dynamically
                val pinOffsets = listOf(
                    Pair(120.dp, 60.dp),
                    Pair(210.dp, 35.dp),
                    Pair(260.dp, 95.dp)
                )

                orders.take(3).forEachIndexed { idx, item ->
                    val offset = pinOffsets.getOrElse(idx) { Pair(150.dp, 80.dp) }
                    val isSelected = selectedOrder?.order?.id == item.order.id

                    Box(
                        modifier = Modifier
                            .offset(x = offset.first, y = offset.second)
                            .clickable { onSelectOrder(item) }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFFD97706) else CleanBluePrimary, // Gold if selected
                            shadowElevation = if (isSelected) 8.dp else 4.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 3.dp else 1.5.dp,
                                color = Color.White
                            ),
                            modifier = Modifier.size(if (isSelected) 36.dp else 30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = FarsiUtils.toFarsiDigits((idx + 1).toString()),
                                    color = Color.White,
                                    fontSize = if (isSelected) 13.sp else 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Bar for Map - Selected Order Info Callout
            selectedOrder?.let { sel ->
                val rack = if (sel.order.rackCode.isNotBlank()) sel.order.rackCode else "تعیین نشده"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CleanBlueContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sel.order.customerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CleanPurpleAccent
                            ) {
                                Text(
                                    text = "قفسه: $rack",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = sel.order.address,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onLaunchNeshanRoute(sel) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسیریابی نشان", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Card component for individual Invoice ready for delivery & warehouse pick-up.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeliveryReadyCard(
    orderWithItems: OrderWithItems,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onOpenScanner: () -> Unit,
    onNavigate: () -> Unit,
    onProceedToSettlement: () -> Unit,
    onReturnToCleanWarehouseClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val order = orderWithItems.order
    val items = orderWithItems.items
    val rackCode = if (order.rackCode.isNotBlank()) order.rackCode else "قفسه A-01"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CleanBlueContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CleanBluePrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Invoice ID, Customer Name, Rack Code Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CleanBluePrimary
                    ) {
                        Text(
                            text = "فاکتور ${order.id}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Prominent Warehouse Rack Code Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CleanPurpleContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanPurpleAccent.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Warehouse,
                            contentDescription = null,
                            tint = CleanPurpleAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "قفسه: $rackCode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanPurpleAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Phone & Address
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = FarsiUtils.toFarsiDigits(order.customerPhone),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = order.address,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Carpets & Stapled Barcodes Section
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "اقلام آماده بارگیری (${items.size} تخته فرش):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    items.forEachIndexed { idx, carpet ->
                        val stapleTag = if (carpet.barcodeTag.isNotBlank()) carpet.barcodeTag else "ST-${carpet.orderId.takeLast(4)}-${carpet.id}"
                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${FarsiUtils.toFarsiDigits((idx + 1).toString())}. ${carpet.carpetType} (${FarsiUtils.toFarsiDigits(carpet.lengthMeter.toString())}×${FarsiUtils.toFarsiDigits(carpet.widthMeter.toString())} م)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CleanPurpleAccent
                                ) {
                                    Text(
                                        text = "کد فرش: $stapleTag",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Mini Barcode preview for each stapled carpet
                            BarcodeView(
                                code = stapleTag,
                                height = 28.dp,
                                showText = false,
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Icon-Only Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Direct Phone Call
                IconButton(
                    onClick = { NavigationUtils.makePhoneCall(context, order.customerPhone) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CleanBlueContainer)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "تماس با مشتری",
                        tint = CleanBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. Neshan Navigation
                IconButton(
                    onClick = onNavigate,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDCFCE7))
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = "مسیریابی نشان",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 3. Scan Barcode
                IconButton(
                    onClick = onOpenScanner,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CleanPurpleContainer)
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "اسکن بارکد",
                        tint = CleanPurpleAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 4. Return to Clean Warehouse (Customer Absent)
                IconButton(
                    onClick = onReturnToCleanWarehouseClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CleanPurpleContainer)
                ) {
                    Icon(
                        Icons.Default.Warehouse,
                        contentDescription = "عدم حضور مشتری / برگشت به انبار",
                        tint = CleanPurpleAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 5. Proceed to Settlement
                IconButton(
                    onClick = onProceedToSettlement,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CleanBluePrimary)
                ) {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = "تحویل و تسویه فاکتور",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
