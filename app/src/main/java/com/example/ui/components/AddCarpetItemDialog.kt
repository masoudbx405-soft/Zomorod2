package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.PanelCatalogItem
import com.example.data.model.PanelCatalogState
import com.example.utils.FarsiUtils
import com.example.ui.theme.CleanPurpleAccent
import com.example.ui.theme.CleanPurpleContainer
import com.example.ui.theme.CleanBluePrimary
import com.example.ui.theme.CleanTealAccent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCarpetItemDialog(
    orderId: String,
    panelCatalogState: PanelCatalogState = PanelCatalogState(),
    onRefreshCatalog: () -> Unit = {},
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
    val context = LocalContext.current
    val fallbackCatalog = remember {
        com.example.data.remote.supabase.ZomorrodSupabaseManager().getDefaultPanelCatalog()
    }

    val dynamicCarpetTypes = remember(panelCatalogState.carpetTypes) {
        if (panelCatalogState.carpetTypes.isNotEmpty()) panelCatalogState.carpetTypes
        else fallbackCatalog.filter { it.category == "CARPET_TYPE" }
    }

    val dynamicServices = remember(panelCatalogState.services) {
        if (panelCatalogState.services.isNotEmpty()) panelCatalogState.services
        else fallbackCatalog.filter { it.category == "SERVICE" }
    }

    val dynamicDefects = remember(panelCatalogState.defects) {
        if (panelCatalogState.defects.isNotEmpty()) panelCatalogState.defects
        else fallbackCatalog.filter { it.category == "DEFECT" }
    }

    // Step 1: Pre-printed Stapled Barcode Tag
    var barcodeTagText by remember {
        mutableStateOf("ST-${orderId.takeLast(4)}-${(10..99).random()}")
    }
    var showStapleScanner by remember { mutableStateOf(false) }

    if (showStapleScanner) {
        StapleTagScannerModal(
            onDismiss = { showStapleScanner = false },
            onTagScanned = { scannedCode ->
                barcodeTagText = scannedCode.uppercase()
                showStapleScanner = false
            }
        )
    }

    val initialCarpet = dynamicCarpetTypes.firstOrNull()
    var selectedType by remember { mutableStateOf(initialCarpet?.name ?: "ماشینی ۶ متری (۲×۳)") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    var lengthText by remember { mutableStateOf(initialCarpet?.defaultLength?.toString() ?: "3.0") }
    var widthText by remember { mutableStateOf(initialCarpet?.defaultWidth?.toString() ?: "2.0") }
    var unitPriceText by remember { mutableStateOf(initialCarpet?.unitPrice?.toString() ?: "100000") }

    val selectedServices = remember {
        mutableStateListOf<String>().apply {
            val defaultSrv = dynamicServices.firstOrNull { it.isDefault } ?: dynamicServices.firstOrNull()
            if (defaultSrv != null) add(defaultSrv.name)
        }
    }
    var servicesDropdownExpanded by remember { mutableStateOf(false) }

    val selectedDefects = remember {
        mutableStateListOf<String>().apply {
            val defaultDef = dynamicDefects.firstOrNull { it.isDefault } ?: dynamicDefects.firstOrNull()
            if (defaultDef != null) add(defaultDef.name)
        }
    }
    var defectsDropdownExpanded by remember { mutableStateOf(false) }

    var customNotes by remember { mutableStateOf("") }

    val length = lengthText.toDoubleOrNull() ?: 0.0
    val width = widthText.toDoubleOrNull() ?: 0.0
    val area = length * width
    val unitPrice = unitPriceText.toLongOrNull() ?: 0L
    val basePrice = (area * unitPrice).toLong()
    val servicesTotal = selectedServices.sumOf { sName ->
        dynamicServices.find { it.name == sName }?.unitPrice ?: 0L
    }
    val totalPrice = basePrice + servicesTotal

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

                // Panel Sync Status Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CleanTealAccent.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanTealAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = CleanTealAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "اقلام و تعرفه‌های فراخوانی‌شده از پنل مرکزی",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTealAccent
                                )
                                Text(
                                    text = if (panelCatalogState.isFromLiveServer) "وضعیت: متصل به سرور پنل مرکزی (panel.yaselectrical.ir)"
                                           else "وضعیت: تعرفه‌های مصوب استاندارد زمرد",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshCatalog,
                            modifier = Modifier.size(28.dp)
                        ) {
                            if (panelCatalogState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CleanTealAccent
                                )
                            } else {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = "بروزرسانی از پنل",
                                    tint = CleanTealAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Stapled Barcode Card (Compact & Modern)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CleanPurpleContainer.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanPurpleAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = CleanPurpleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "اسکن بارکد منگنه‌شده فرش:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = CleanPurpleAccent
                                )
                            }

                            TextButton(
                                onClick = {
                                    barcodeTagText = "ST-${orderId.takeLast(4)}-${(10..99).random()}"
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = CleanPurpleAccent)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("کد تصادفی", fontSize = 10.sp, color = CleanPurpleAccent)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = barcodeTagText,
                                onValueChange = { barcodeTagText = it.uppercase() },
                                label = { Text("شناسه / کد فرش", fontSize = 11.sp) },
                                placeholder = { Text("اسکن یا ورود دستی...", fontSize = 11.sp) },
                                singleLine = true,
                                leadingIcon = {
                                    IconButton(onClick = { showStapleScanner = true }) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = "اسکن بارکد",
                                            tint = CleanPurpleAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (barcodeTagText.isNotBlank()) {
                                        IconButton(onClick = { barcodeTagText = "" }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "پاک کردن",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            // Primary Action: Camera Scan Barcode
                            Button(
                                onClick = { showStapleScanner = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اسکن بارکد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Carpet Type Selection (Dropdown Menu with Panel Items)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "۱. نوع و مشخصات فرش (تعرفه پنل):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CleanPurpleAccent
                    )
                    Text(
                        text = "${dynamicCarpetTypes.size} مورد در پنل",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        label = { Text("نوع فرش و تعرفه مصوب") },
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
                        dynamicCarpetTypes.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "نرخ پنل: ${FarsiUtils.formatPrice(item.unitPrice)} / ${item.unit}",
                                                fontSize = 11.sp,
                                                color = CleanPurpleAccent
                                            )
                                            if (item.description.isNotBlank()) {
                                                Text(
                                                    text = " • ${item.description}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (selectedType == item.name) CleanPurpleAccent else Color.Transparent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    selectedType = item.name
                                    typeDropdownExpanded = false
                                    if (item.defaultLength > 0.0) {
                                        lengthText = item.defaultLength.toString()
                                    }
                                    if (item.defaultWidth > 0.0) {
                                        widthText = item.defaultWidth.toString()
                                    }
                                    if (item.unitPrice > 0L) {
                                        unitPriceText = item.unitPrice.toString()
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

                // 4. Requested Services (Dropdown Menu from Panel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CleanHands, contentDescription = null, tint = CleanPurpleAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "۳. خدمات درخواستی و تکمیلی (پنل زمرد):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CleanPurpleAccent
                        )
                    }
                    Text(
                        text = "${dynamicServices.size} خدمت",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        label = { Text("لیست خدمات و تعرفه‌ها") },
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
                        dynamicServices.forEach { serviceItem ->
                            val isSelected = selectedServices.contains(serviceItem.name)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(serviceItem.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            if (serviceItem.description.isNotBlank()) {
                                                Text(serviceItem.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        if (serviceItem.unitPrice > 0L) {
                                            Text(
                                                text = "+ ${FarsiUtils.formatPrice(serviceItem.unitPrice)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CleanPurpleAccent
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = CleanPurpleAccent)
                                    )
                                },
                                onClick = {
                                    if (isSelected) selectedServices.remove(serviceItem.name)
                                    else selectedServices.add(serviceItem.name)
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
                        selectedServices.forEach { serviceName ->
                            val srv = dynamicServices.find { it.name == serviceName }
                            FilterChip(
                                selected = true,
                                onClick = { selectedServices.remove(serviceName) },
                                label = {
                                    Text(
                                        if (srv != null && srv.unitPrice > 0L) "$serviceName (${FarsiUtils.formatPrice(srv.unitPrice)})"
                                        else serviceName,
                                        fontSize = 11.sp
                                    )
                                },
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

                // 5. Initial Defects / Flaws (Dropdown Menu from Panel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Text(
                        text = "${dynamicDefects.size} مورد پنل",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        dynamicDefects.forEach { defectItem ->
                            val isSelected = selectedDefects.contains(defectItem.name)
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(defectItem.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        if (defectItem.description.isNotBlank()) {
                                            Text(defectItem.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                                    )
                                },
                                onClick = {
                                    if (defectItem.name.contains("بدون عیب")) {
                                        selectedDefects.clear()
                                        selectedDefects.add(defectItem.name)
                                    } else {
                                        selectedDefects.removeAll { it.contains("بدون عیب") }
                                        if (isSelected) selectedDefects.remove(defectItem.name)
                                        else selectedDefects.add(defectItem.name)
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
                        selectedDefects.forEach { defectName ->
                            FilterChip(
                                selected = true,
                                onClick = {
                                    selectedDefects.remove(defectName)
                                    if (selectedDefects.isEmpty()) {
                                        dynamicDefects.firstOrNull { it.name.contains("بدون عیب") }?.let {
                                            selectedDefects.add(it.name)
                                        }
                                    }
                                },
                                label = { Text(defectName, fontSize = 11.sp) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StapleTagScannerModal(
    onDismiss: () -> Unit,
    onTagScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var manualCodeInput by remember { mutableStateOf("") }
    var isFlashlightOn by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CleanPurpleAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "اسکن بارکد منگنه‌شده فرش",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "بارکد چاپ‌شده روی تگ منگنه فرش را در کادر اسکن قرار دهید",
                                color = CleanTealAccent,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Camera Frame
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121820))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (hasCameraPermission) {
                            RealCameraPreviewView(
                                isFlashlightOn = isFlashlightOn,
                                onBarcodeDetected = { scanned ->
                                    if (scanned.isNotBlank()) {
                                        onTagScanned(scanned)
                                    }
                                }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = CleanTealAccent,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "جهت اسکن بارکد منگنه فرش، مجوز دوربین را فعال کنید",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فعالسازی دوربین", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        // Flashlight Toggle
                        if (hasCameraPermission) {
                            IconButton(
                                onClick = { isFlashlightOn = !isFlashlightOn },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "چراغ‌قوه",
                                    tint = if (isFlashlightOn) Color.Yellow else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Manual Tag Input fallback
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "یا ورود دستی کد منگنه فرش:",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualCodeInput,
                                onValueChange = { manualCodeInput = it.uppercase() },
                                label = { Text("کد بارکد منگنه", color = Color.LightGray, fontSize = 11.sp) },
                                placeholder = { Text("مثال: TAG-1042-38", color = Color.Gray, fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CleanPurpleAccent,
                                    unfocusedBorderColor = Color.Gray
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    if (manualCodeInput.isNotBlank()) {
                                        onTagScanned(manualCodeInput.trim())
                                    }
                                },
                                enabled = manualCodeInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("ثبت کد", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "نمونه کدهای تست سریع:",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("ST-1042-01", "TAG-9014", "TAG-5520").forEach { sample ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onTagScanned(sample) },
                                    label = { Text(sample, fontSize = 11.sp, color = Color.White) },
                                    colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF334155))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

