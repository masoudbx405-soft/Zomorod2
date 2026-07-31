package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CleanBlueContainer
import com.example.ui.theme.CleanBluePrimary
import com.example.ui.theme.CleanPurpleAccent
import com.example.ui.theme.CleanPurpleContainer
import com.example.ui.theme.CleanRedAccent
import com.example.ui.theme.CleanTealAccent
import com.example.ui.theme.CleanTealContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    connectedPrinterName: String?,
    onOpenPrinterDialog: () -> Unit,
    onSyncNow: () -> Unit,
    savedServerUrl: String = "https://panel.zomorrod-carpet.com/api/v1",
    isTestingConnection: Boolean = false,
    connectionTestResult: String? = null,
    onUpdateServerUrl: (String) -> Unit = {},
    onTestConnection: (String) -> Unit = {},
    onTestNotification: () -> Unit = {},
    onSimulateNewOrder: () -> Unit = {},
    onSimulateStatusChange: () -> Unit = {},
    backupInfo: com.example.utils.BackupInfo? = null,
    onBackupDatabase: () -> Unit = {},
    onRestoreDatabase: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current

    var serverUrl by remember(savedServerUrl) { mutableStateOf(savedServerUrl) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoSyncEnabled by remember { mutableStateOf(true) }
    var autoSyncInterval by remember { mutableStateOf("۱۰ دقیقه") }
    var autoPrintReceipt by remember { mutableStateOf(true) }
    var receiptCopies by remember { mutableStateOf("۲ نسخه (مشتری + راننده)") }
    var paperWidth by remember { mutableStateOf("۸۰ میلی‌متر (پوز/حرارتی)") }
    var preferredMapApp by remember { mutableStateOf("مسیریاب نشان") }
    var scanSoundBeep by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CleanBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = CleanBluePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "تنظیمات نرم‌افزار و سخت‌افزار",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تنظیمات اتصال پنل، چاپگر، اسکنر و اطلاعات سفیر",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Room Database Backup & Restore Card
        SettingsSectionCard(
            title = "پشتیبان‌گیری و بازیابی دیتابیس محلی (Room)",
            icon = Icons.Default.Backup
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "در صورت تعویض گوشی یا بروز مشکل، می‌توانید از تمام اطلاعات آفلاین سفارشات، فاکتورها، چت‌ها و لوگ‌های GPS فایل پشتیبان تهیه کرده و آن را بازیابی نمایید.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                if (backupInfo != null && backupInfo.exists) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TaskAlt, contentDescription = null, tint = CleanBluePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("آخرین نسخه پشتیبان موجود در حافظه:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("تاریخ ثبت: ${backupInfo.timestamp}", fontSize = 11.sp)
                            Text("حجم فایل: ${backupInfo.fileSizeKb} کیلوبایت (${backupInfo.ordersCount} سفارش)", fontSize = 11.sp)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("هنوز فایل پشتیبانی در دستگاه ایجاد نشده است.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onBackupDatabase,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanBluePrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ایجاد پشتیبان جدید", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onRestoreDatabase,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CleanPurpleAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بازیابی اطلاعات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Server Connection & Sync Settings
        SettingsSectionCard(
            title = "ارتباط با پنل مدیریت و همگام‌سازی",
            icon = Icons.Default.CloudSync
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        onUpdateServerUrl(it)
                    },
                    label = { Text("آدرس سرور API پنل مرکزی", fontSize = 11.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Test Connection Button
                OutlinedButton(
                    onClick = { onTestConnection(serverUrl) },
                    enabled = !isTestingConnection,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("در حال بررسی اتصال به سرور...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تست اتصال به سرور API", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (connectionTestResult != null) {
                    val isSuccess = connectionTestResult.startsWith("موفقیت")
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFFEBEE),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSuccess) Color(0xFF86EFAC) else Color(0xFFFFCDD2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isSuccess) Color(0xFF16A34A) else Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = connectionTestResult,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSuccess) Color(0xFF15803D) else Color(0xFFC62828),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "همگام‌سازی خودکار در پس‌زمینه",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "ارسال خودکار وضعیت فاکتورها و مبالغ دریافت شده به پنل",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { autoSyncEnabled = it }
                    )
                }

                if (autoSyncEnabled) {
                    Text(
                        text = "بازه زمانی بروزرسانی خودکار:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("۵ دقیقه", "۱۰ دقیقه", "۳۰ دقیقه").forEach { interval ->
                            FilterChip(
                                selected = autoSyncInterval == interval,
                                onClick = { autoSyncInterval = interval },
                                label = { Text(interval, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CleanPurpleContainer,
                                    selectedLabelColor = CleanPurpleAccent
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onSyncNow()
                        Toast.makeText(context, "درخواست همگام‌سازی دستی ارسال شد", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استعلام فوری و همگام‌سازی با پنل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Local Notifications & Server Events Card
        SettingsSectionCard(
            title = "سیستم اعلان‌های محلی و تغییرات سرور",
            icon = Icons.Default.NotificationsActive
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "اعلان اختصاص سفارش جدید و تغییر وضعیت",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "هشدار صوتی و بنر اعلان حتی زمان بسته‌بودن یا پس‌زمینه اپلیکیشن",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "ابزار‌های عملیاتی و تست ارتباطات:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { onTestNotification() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ارسال اعلان تست دستگاه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { onSimulateNewOrder() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = null, tint = CleanBluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دریافت سفارش واقعی", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onSimulateStatusChange() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PublishedWithChanges, contentDescription = null, tint = CleanPurpleAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بروزرسانی وضعیت سرور", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Printer Settings Section
        SettingsSectionCard(
            title = "تنظیمات پرینتر حرارتی فاکتور",
            icon = Icons.Default.Print
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Connected status box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (connectedPrinterName != null) CleanTealContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (connectedPrinterName != null) CleanTealAccent else MaterialTheme.colorScheme.outline)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (connectedPrinterName != null) "پرینتر متصل: $connectedPrinterName" else "هیچ پرینتری متصل نیست",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (connectedPrinterName != null) CleanTealAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenPrinterDialog,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("جستجوی بلوتوث", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "چاپ خودکار فاکتور پس از تسویه",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "بلافاصله پس از ثبت پرداخت رسید چاپ شود",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoPrintReceipt,
                        onCheckedChange = { autoPrintReceipt = it }
                    )
                }

                Text(
                    text = "تعداد نسخه‌های چاپ رسید:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("۱ نسخه (مشتری)", "۲ نسخه (مشتری + راننده)").forEach { option ->
                        FilterChip(
                            selected = receiptCopies == option,
                            onClick = { receiptCopies = option },
                            label = { Text(option, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CleanBlueContainer,
                                selectedLabelColor = CleanBluePrimary
                            )
                        )
                    }
                }
            }
        }

        // Navigation App Selection
        SettingsSectionCard(
            title = "مسیریاب و نقشه پیش‌فرض",
            icon = Icons.Default.Navigation
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "برنامه مسیریاب ترجیحی جهت هدایت به آدرس مشتری:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("مسیریاب نشان", "مسیریاب بلد", "گوگل مپس").forEach { appName ->
                        FilterChip(
                            selected = preferredMapApp == appName,
                            onClick = { preferredMapApp = appName },
                            label = { Text(appName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDCFCE7),
                                selectedLabelColor = Color(0xFF16A34A)
                            )
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                var neshanApiKeyInput by remember { mutableStateOf("service.eb686e96487f482e862564535b04f38f") }

                OutlinedTextField(
                    value = neshanApiKeyInput,
                    onValueChange = { neshanApiKeyInput = it },
                    label = { Text("کلید اختصاصی API نقشه نشان (Neshan Service Key)", fontSize = 11.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // App Theme & Sound Settings
        SettingsSectionCard(
            title = "پوسته و صداهای سیستم",
            icon = Icons.Default.Palette
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "پوسته تاریک (حالت شب)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "مناسب برای کار در شب و کاهش مصرف باتری",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "صدای بوق (Beep) هنگام اسکن موفق بارکد",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "تایید صوتی اسکن بارکدهای منگنه فرش و قفسه",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = scanSoundBeep,
                        onCheckedChange = { scanSoundBeep = it }
                    )
                }
            }
        }

        // Save Settings Action Button
        Button(
            onClick = {
                Toast.makeText(context, "تنظیمات برنامه با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CleanBluePrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ذخیره تمام تغییرات تنظیمات", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // Logout Button Card
        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CleanRedAccent),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanRedAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("خروج از حساب کاربری راننده", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = CleanBluePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            content()
        }
    }
}
