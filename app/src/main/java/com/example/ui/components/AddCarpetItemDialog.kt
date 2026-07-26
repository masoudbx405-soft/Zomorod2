package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.FarsiUtils
import com.example.ui.theme.CleanPurpleAccent
import com.example.ui.theme.CleanPurpleContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCarpetItemDialog(
    orderId: String,
    onDismiss: () -> Unit,
    onConfirm: (
        carpetType: String,
        length: Double,
        width: Double,
        unitPrice: Long,
        services: List<String>,
        defects: List<String>,
        notes: String,
        barcodeTag: String
    ) -> Unit
) {
    // Step 1: Pre-printed Stapled Barcode Tag
    var barcodeTagText by remember {
        mutableStateOf("ST-${orderId.takeLast(4)}-${(10..99).random()}")
    }

    val carpetTypes = listOf(
        "ماشینی ۶ متری (۲×۳)",
        "ماشینی ۹ متری (۲٫۵×۳٫۵)",
        "ماشینی ۱۲ متری (۳×۴)",
        "دستبافت نائین",
        "دستبافت ابریشم",
        "گلیم / گبه / جاجیم",
        "موکت / سجاده / مدرن",
        "سایر ابعاد (سفارشی)"
    )
    var selectedType by remember { mutableStateOf(carpetTypes[0]) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    var lengthText by remember { mutableStateOf("3.0") }
    var widthText by remember { mutableStateOf("2.0") }
    var unitPriceText by remember { mutableStateOf("100000") }

    val availableServices = listOf(
        "شستشوی ویژه (اعلا)",
        "ابریشم‌شویی",
        "رفوگری و ریشه‌زنی",
        "شیرازه‌دوزی",
        "لکه‌بری تخصصی",
        "ضدالعفونی و اتو"
    )
    val selectedServices = remember { mutableStateListOf("شستشوی ویژه (اعلا)") }
    var servicesDropdownExpanded by remember { mutableStateOf(false) }

    val availableDefects = listOf(
        "بدون عیب اولیه",
        "سوختگی جزئی",
        "پوسیدگی حاشیه",
        "پارگی / شکافتگی",
        "بیدزدگی",
        "تغییر رنگ / لکه شدید"
    )
    val selectedDefects = remember { mutableStateListOf("بدون عیب اولیه") }
    var defectsDropdownExpanded by remember { mutableStateOf(false) }

    var customNotes by remember { mutableStateOf("") }

    val length = lengthText.toDoubleOrNull() ?: 0.0
    val width = widthText.toDoubleOrNull() ?: 0.0
    val area = length * width
    val unitPrice = unitPriceText.toLongOrNull() ?: 0L
    val totalPrice = (area * unitPrice).toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CleanPurpleContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    tint = CleanPurpleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ثبت اقلام فرش",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "سفارش کد ${FarsiUtils.toFarsiDigits(orderId)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "بستن",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Stapled Barcode Card (Compact & Modern)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CleanPurpleContainer.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanPurpleAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = barcodeTagText,
                            onValueChange = { barcodeTagText = it.uppercase() },
                            label = { Text("شناسه / کد فرش", fontSize = 11.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = CleanPurpleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                barcodeTagText = "ST-${orderId.takeLast(4)}-${(10..99).random()}"
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("کد جدید", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Carpet Type Selection (Dropdown Menu)
                Text(
                    text = "۱. انتخاب نوع فرش:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CleanPurpleAccent
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع و دسته فرش") },
                        leadingIcon = {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = CleanPurpleAccent)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        carpetTypes.forEach { typeOption ->
                            DropdownMenuItem(
                                text = { Text(typeOption, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (selectedType == typeOption) CleanPurpleAccent else Color.Transparent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    selectedType = typeOption
                                    typeDropdownExpanded = false
                                    when {
                                        typeOption.contains("۱۲") -> { lengthText = "4.0"; widthText = "3.0" }
                                        typeOption.contains("۹") -> { lengthText = "3.5"; widthText = "2.5" }
                                        typeOption.contains("۶") -> { lengthText = "3.0"; widthText = "2.0" }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Dimensions & Price Calculation Card
                Text(
                    text = "۲. ابعاد و نرخ شستشو:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CleanPurpleAccent
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = lengthText,
                        onValueChange = { lengthText = it },
                        label = { Text("طول (متر)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Height, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = widthText,
                        onValueChange = { widthText = it },
                        label = { Text("عرض (متر)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = { unitPriceText = it },
                    label = { Text("نرخ شستشو (تومان هر متر مربع)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live Summary Area & Price Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CleanPurpleContainer.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanPurpleAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AspectRatio, contentDescription = null, tint = CleanPurpleAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("مساحت کل:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    FarsiUtils.formatArea(area),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CleanPurpleAccent
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = CleanPurpleAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("مبلغ کل این فرش:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    FarsiUtils.formatPrice(totalPrice),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CleanPurpleAccent
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Requested Services (Dropdown Menu)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CleanHands, contentDescription = null, tint = CleanPurpleAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "۳. انتخاب خدمات درخواستی:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CleanPurpleAccent
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = servicesDropdownExpanded,
                    onExpandedChange = { servicesDropdownExpanded = !servicesDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedServices.isEmpty()) "هیچ خدماتی انتخاب نشده" else selectedServices.joinToString("، "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("لیست خدمات درخواستی") },
                        leadingIcon = {
                            Icon(Icons.Default.Build, contentDescription = null, tint = CleanPurpleAccent)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = servicesDropdownExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = servicesDropdownExpanded,
                        onDismissRequest = { servicesDropdownExpanded = false }
                    ) {
                        availableServices.forEach { service ->
                            val isSelected = selectedServices.contains(service)
                            DropdownMenuItem(
                                text = { Text(service, fontSize = 13.sp) },
                                leadingIcon = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = CleanPurpleAccent)
                                    )
                                },
                                onClick = {
                                    if (isSelected) selectedServices.remove(service)
                                    else selectedServices.add(service)
                                }
                            )
                        }
                    }
                }

                if (selectedServices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedServices.forEach { service ->
                            FilterChip(
                                selected = true,
                                onClick = { selectedServices.remove(service) },
                                label = { Text(service, fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(12.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CleanPurpleContainer,
                                    selectedLabelColor = CleanPurpleAccent
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Initial Defects / Flaws (Dropdown Menu)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "۴. ثبت عیوب اولیه (قبل از شستشو):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = defectsDropdownExpanded,
                    onExpandedChange = { defectsDropdownExpanded = !defectsDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedDefects.isEmpty()) "بدون عیب ثبت‌شده" else selectedDefects.joinToString("، "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("لیست عیوب فرش") },
                        leadingIcon = {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = defectsDropdownExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = defectsDropdownExpanded,
                        onDismissRequest = { defectsDropdownExpanded = false }
                    ) {
                        availableDefects.forEach { defect ->
                            val isSelected = selectedDefects.contains(defect)
                            DropdownMenuItem(
                                text = { Text(defect, fontSize = 13.sp) },
                                leadingIcon = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                                    )
                                },
                                onClick = {
                                    if (defect == "بدون عیب اولیه") {
                                        selectedDefects.clear()
                                        selectedDefects.add("بدون عیب اولیه")
                                    } else {
                                        selectedDefects.remove("بدون عیب اولیه")
                                        if (isSelected) selectedDefects.remove(defect)
                                        else selectedDefects.add(defect)
                                    }
                                }
                            )
                        }
                    }
                }

                if (selectedDefects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedDefects.forEach { defect ->
                            FilterChip(
                                selected = true,
                                onClick = {
                                    selectedDefects.remove(defect)
                                    if (selectedDefects.isEmpty()) selectedDefects.add("بدون عیب اولیه")
                                },
                                label = { Text(defect, fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(12.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Notes
                OutlinedTextField(
                    value = customNotes,
                    onValueChange = { customNotes = it },
                    label = { Text("یادداشت / توضیحات راننده", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.NoteAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (length > 0 && width > 0 && unitPrice >= 0 && barcodeTagText.isNotBlank()) {
                            onConfirm(
                                selectedType,
                                length,
                                width,
                                unitPrice,
                                selectedServices.toList(),
                                selectedDefects.toList(),
                                customNotes,
                                barcodeTagText
                            )
                            onDismiss()
                        }
                    },
                    enabled = barcodeTagText.isNotBlank() && length > 0 && width > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ثبت و افزودن به فاکتور", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

