package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.OrderWithItems
import com.example.ui.components.SettlementDialog
import com.example.ui.theme.CleanBlueContainer
import com.example.ui.theme.CleanBluePrimary
import com.example.ui.theme.CleanPurpleAccent
import com.example.ui.theme.CleanPurpleContainer
import com.example.utils.FarsiUtils
import com.example.utils.NavigationUtils

@Composable
fun DeliverySettlementScreen(
    orders: List<OrderWithItems>,
    onSettlePayment: (orderId: String, paidAmount: Long, discountAmount: Long, paymentMethod: String) -> Unit,
    onPrintReceipt: (OrderWithItems, String) -> Unit,
    onOpenScanner: (orderId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val deliveryOrders = orders.filter {
        it.order.orderType == "DELIVERY" || it.order.status == "READY_FOR_DELIVERY" || it.order.status == "DELIVERED_SETTLED"
    }

    var selectedOrderForSettlement by remember { mutableStateOf<OrderWithItems?>(null) }

    if (selectedOrderForSettlement != null) {
        SettlementDialog(
            orderWithItems = selectedOrderForSettlement!!,
            onDismiss = { selectedOrderForSettlement = null },
            onConfirmSettlement = { paid, discount, method, print ->
                val order = selectedOrderForSettlement!!
                onSettlePayment(order.order.id, paid, discount, method)
                if (print) {
                    onPrintReceipt(order, method)
                }
                selectedOrderForSettlement = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Top Info Box
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "لیست فرش‌های آماده تحویل به مشتری",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ترتیب بر اساس مسیریابی بهینه نقشه هوشمند پنل وب",
                        fontSize = 12.sp
                    )
                }
                Icon(
                    Icons.Default.Route,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (deliveryOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("هیچ فرشی در صف تحویل قرار ندارد.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(deliveryOrders, key = { it.order.id }) { item ->
                    DeliveryOrderCard(
                        orderWithItems = item,
                        onCall = { NavigationUtils.makePhoneCall(context, item.order.customerPhone) },
                        onNavigateNeshan = { NavigationUtils.launchNeshan(context, item.order.latitude, item.order.longitude, item.order.address) },
                        onNavigateBalad = { NavigationUtils.launchBalad(context, item.order.latitude, item.order.longitude, item.order.address) },
                        onSettleClick = { selectedOrderForSettlement = item },
                        onOpenScanVerification = { onOpenScanner(item.order.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeliveryOrderCard(
    orderWithItems: OrderWithItems,
    onCall: () -> Unit,
    onNavigateNeshan: () -> Unit,
    onNavigateBalad: () -> Unit,
    onSettleClick: () -> Unit,
    onOpenScanVerification: () -> Unit = {}
) {
    val order = orderWithItems.order
    val isSettled = order.status == "DELIVERED_SETTLED"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSettled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ایستگاه شماره ${order.routeOrder}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "کد: ${order.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "نام تحویل‌گیرنده: ${order.customerName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = "تلفن: ${order.customerPhone}", fontSize = 13.sp)
            Text(text = "آدرس: ${order.address}", fontSize = 12.sp)

            if (order.rackCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("محل برداشت از انبار: قفسه ${order.rackCode}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Carpet items summary
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("اقلام این فاکتور (${orderWithItems.items.size} مورد):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    orderWithItems.items.forEach { item ->
                        val tag = if (item.barcodeTag.isNotBlank()) item.barcodeTag else "ST-${item.orderId.takeLast(4)}-${item.id}"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "• ${item.carpetType} (${item.lengthMeter}×${item.widthMeter} م) - ${item.requestedServicesJson}",
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CleanPurpleContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CleanPurpleAccent.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "📌 $tag",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanPurpleAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("مبلغ قابلاخذ:", fontSize = 13.sp)
                Text(
                    text = FarsiUtils.formatPrice(order.totalAmount - order.discountAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onCall, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = "تماس", tint = MaterialTheme.colorScheme.primary)
                    }
                    FilledTonalButton(
                        onClick = onNavigateNeshan,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("نشان", fontSize = 11.sp)
                    }

                    // Barcode scan verification button before delivery
                    IconButton(
                        onClick = onOpenScanVerification,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CleanBlueContainer)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "اسکن تطبیق تحویل",
                            tint = CleanBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!isSettled) {
                    Button(
                        onClick = onSettleClick,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تسویه و تحویل", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onSettleClick,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("مشاهده رسید تسویه", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
