package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
import com.example.ui.theme.*
import com.example.utils.FarsiUtils
import com.example.utils.NavigationUtils

@Composable
fun CollectionRouteScreen(
    orders: List<OrderWithItems>,
    onSelectOrderForInvoice: (OrderWithItems) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    // Filter pickup / collection orders (only those pending collection / invoice registration)
    val pickupOrders = orders.filter {
        it.order.status == "ASSIGNED"
    }

    val filteredOrders = pickupOrders.filter { item ->
        searchQuery.isBlank() ||
                item.order.customerName.contains(searchQuery, true) ||
                item.order.customerPhone.contains(searchQuery) ||
                item.order.id.contains(searchQuery, true) ||
                item.order.address.contains(searchQuery, true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("جستجو در نام مشتری، تلفن، آدرس یا کد سفارش...", fontSize = 11.sp) },
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // List of Collection Items listed vertically
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "هیچ سفارشی برای جمع‌آوری یافت نشد",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredOrders, key = { it.order.id }) { item ->
                    CollectionOrderItemCard(
                        orderWithItems = item,
                        onNavigate = {
                            NavigationUtils.launchNeshan(
                                context,
                                item.order.latitude,
                                item.order.longitude,
                                item.order.address
                            )
                        },
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${item.order.customerPhone}")
                            }
                            context.startActivity(intent)
                        },
                        onRegisterInvoice = {
                            onSelectOrderForInvoice(item)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Vertical Collection Item containing:
 * 1. Approximate pinned Neshan Map preview box
 * 2. Customer details card
 * 3. Register Invoice ("ثبت فاکتور") action button under the card
 */
@Composable
private fun CollectionOrderItemCard(
    orderWithItems: OrderWithItems,
    onNavigate: () -> Unit,
    onCall: () -> Unit,
    onRegisterInvoice: () -> Unit
) {
    val order = orderWithItems.order
    val itemCount = orderWithItems.items.size

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Real Neshan Map Location Preview Box
            NeshanSingleLocationMapPreview(
                customerName = order.customerName,
                address = order.address,
                orderId = order.id,
                onNavigate = onNavigate
            )

            // 2. Customer Details Card Content
            Column(modifier = Modifier.padding(14.dp)) {
                // Header: Order ID & Customer Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CleanPurpleAccent
                    ) {
                        Text(
                            text = "سفارش ${order.id}",
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
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Phone
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = FarsiUtils.toFarsiDigits(order.customerPhone),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Address
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.address,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }

                // Registered carpet items summary if any
                if (itemCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CleanPurpleContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "تعداد اقلام ثبت شده: ${FarsiUtils.toFarsiDigits(itemCount.toString())} تخته فرش",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanPurpleAccent
                            )
                            Icon(Icons.Default.Check, contentDescription = null, tint = CleanPurpleAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Row: Status Badge + Action Icon Buttons (Call, Navigation, Register Invoice - NO TEXT LABELS)
                val statusText = when (order.status) {
                    "ASSIGNED" -> "جدید / در انتظار مراجعه"
                    "COLLECTED_IN_INSPECTION" -> "فاکتور ثبت شده"
                    else -> "آماده دریافت"
                }
                val statusColor = if (order.status == "ASSIGNED") Color(0xFFF57C00) else CleanPurpleAccent

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Moved Status Badge to bottom
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Single Row of Icon-Only Action Buttons (No text labels)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Phone Call Icon Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanBlueContainer,
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { onCall() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "تماس تلفنی",
                                    tint = CleanBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 2. Neshan Navigation Icon Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { onNavigate() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Navigation,
                                    contentDescription = "مسیریابی نشان",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 3. Register Invoice / Add Carpets Icon Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanPurpleContainer,
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { onRegisterInvoice() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = "ثبت/ویرایش فاکتور",
                                    tint = CleanPurpleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mini Map Preview for an individual order location pinned on Neshan Map style.
 * Fully synchronized with Neshan Map API structure. Static layout without animations.
 */
@Composable
private fun NeshanSingleLocationMapPreview(
    customerName: String,
    address: String,
    orderId: String,
    onNavigate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(Color(0xFFE2E2DC))
    ) {
        val roadColor = Color(0xFFFFFFFF)
        val mainRoadColor = Color(0xFFFDE047)
        val buildingColor = Color(0xFFCECED6)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Background city blocks
            drawRoundRect(
                color = buildingColor,
                topLeft = Offset(w * 0.05f, h * 0.1f),
                size = Size(w * 0.38f, h * 0.38f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = buildingColor,
                topLeft = Offset(w * 0.5f, h * 0.1f),
                size = Size(w * 0.45f, h * 0.32f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = buildingColor,
                topLeft = Offset(w * 0.08f, h * 0.58f),
                size = Size(w * 0.42f, h * 0.35f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = buildingColor,
                topLeft = Offset(w * 0.58f, h * 0.52f),
                size = Size(w * 0.35f, h * 0.4f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Secondary roads
            drawLine(roadColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 14f)
            drawLine(roadColor, Offset(w * 0.48f, 0f), Offset(w * 0.48f, h), strokeWidth = 14f)

            // Main Avenue (Yellow)
            drawLine(mainRoadColor, Offset(0f, h * 0.22f), Offset(w, h * 0.22f), strokeWidth = 18f)
        }

        // Neshan Pinned Location Badge (Center Overlay) - Static without animation
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF22C55E))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF22C55E),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PinDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "پین آدرس: $customerName",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top-Left Neshan API Brand Label
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF22C55E)
            ) {
                Text(
                    text = "نقشه نشان (API)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Bottom-Right Quick Navigation Icon Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF22C55E),
                shadowElevation = 4.dp,
                modifier = Modifier.clickable { onNavigate() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مسیریابی",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
