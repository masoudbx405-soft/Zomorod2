package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.OrderWithItems
import com.example.utils.PrinterManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPreviewDialog(
    title: String,
    orderWithItems: OrderWithItems,
    paymentMethodLabel: String = "پیش‌فاکتور اولیه",
    isPrinting: Boolean,
    onDismiss: () -> Unit,
    onPrintConfirm: () -> Unit
) {
    val order = orderWithItems.order
    val itemsSummary = orderWithItems.items.map {
        "${it.carpetType} (${it.lengthMeter}x${it.widthMeter}متر) - ${it.requestedServicesJson} - ${it.totalPrice} تومان"
    }

    val receiptText = PrinterManager.buildEscPosThermalReceiptText(
        title = title,
        orderId = order.id,
        customerName = order.customerName,
        customerPhone = order.customerPhone,
        address = order.address,
        carpetItemsSummary = if (itemsSummary.isEmpty()) listOf("هنوز فرشی ثبت نشده است") else itemsSummary,
        totalPrice = order.totalAmount,
        discount = order.discountAmount,
        netPayable = order.totalAmount - order.discountAmount,
        paymentMethod = paymentMethodLabel,
        rackCode = order.rackCode
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("پیش‌نمایش چاپ فاکتور حرارتی (ESC/POS)", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated ESC/POS Receipt Paper
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)), // Thermal Paper tint
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = receiptText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Black,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onPrintConfirm,
                    enabled = !isPrinting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("در حال ارسال به پرینتر...")
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ارسال دستور چاپ حرارتی بلوتوثی", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
