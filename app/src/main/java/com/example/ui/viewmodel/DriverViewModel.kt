package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ZomorrodDatabase
import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.GpsLogEntity
import com.example.data.local.model.OrderWithItems
import com.example.data.repository.ZomorrodRepository
import com.example.utils.BluetoothPrinterDevice
import com.example.utils.PrinterManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ZomorrodRepository
    private val prefs = application.getSharedPreferences("zomorrod_driver_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _savedDriverPhone = MutableStateFlow(prefs.getString("driver_phone", "09123456789") ?: "09123456789")
    val savedDriverPhone: StateFlow<String> = _savedDriverPhone

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _generatedOtp = MutableStateFlow("1234")
    val generatedOtp: StateFlow<String> = _generatedOtp

    init {
        val db = ZomorrodDatabase.getDatabase(application)
        repository = ZomorrodRepository(db.orderDao(), db.chatMessageDao(), db.gpsLogDao())
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun requestOtp(phone: String) {
        if (phone.length < 11 || !phone.startsWith("09")) {
            _authError.value = "لطفاً شماره همراه معتبر ۱۱ رقمی (مانند ۰۹۱۲۳۴۵۶۷۸۹) وارد کنید."
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            delay(1000)
            _authLoading.value = false
            _otpSent.value = true
            _generatedOtp.value = "1234"
            _syncToastMessage.value = "شماره راننده در پنل تایید شد. کد تایید ورود: ۱۲۳۴"
        }
    }

    fun verifyOtp(phone: String, code: String) {
        if (code != _generatedOtp.value && code != "1234") {
            _authError.value = "کد تایید وارد شده اشتباه است."
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            delay(800)
            _authLoading.value = false
            prefs.edit().putBoolean("is_logged_in", true).putString("driver_phone", phone).apply()
            _savedDriverPhone.value = phone
            _isLoggedIn.value = true
            _otpSent.value = false
            _syncToastMessage.value = "خوش آمدید! ورود موفقیت‌آمیز به اپلیکیشن راننده زمرد"
        }
    }

    fun resetOtpState() {
        _otpSent.value = false
        _authError.value = null
    }

    fun logoutDriver() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
        _otpSent.value = false
        _authError.value = null
        _syncToastMessage.value = "از حساب کاربری راننده خارج شدید."
    }

    val ordersList: StateFlow<List<OrderWithItems>> = repository.allOrders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.allChatMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unsyncedCount: StateFlow<Int> = repository.unsyncedOrdersCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val gpsLogs: StateFlow<List<GpsLogEntity>> = repository.recentGpsLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedOrderId = MutableStateFlow<String?>(null)
    val selectedOrderId: StateFlow<String?> = _selectedOrderId

    val selectedOrder: StateFlow<OrderWithItems?> = combine(ordersList, _selectedOrderId) { list, id ->
        if (id == null) list.firstOrNull() else list.find { it.order.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeTab = MutableStateFlow(0) // 0: Missions, 1: Pickup, 2: Delivery, 3: Chat, 4: GPS
    val activeTab: StateFlow<Int> = _activeTab

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _isGpsActive = MutableStateFlow(true)
    val isGpsActive: StateFlow<Boolean> = _isGpsActive

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncToastMessage = MutableStateFlow<String?>(null)
    val syncToastMessage: StateFlow<String?> = _syncToastMessage

    private val _showScannerDialog = MutableStateFlow(false)
    val showScannerDialog: StateFlow<Boolean> = _showScannerDialog

    private val _scanStage = MutableStateFlow(com.example.data.model.ScanStage.DELIVERY)
    val scanStage: StateFlow<com.example.data.model.ScanStage> = _scanStage

    val connectedPrinter: StateFlow<BluetoothPrinterDevice?> = PrinterManager.connectedPrinter
    val availablePrinters: StateFlow<List<BluetoothPrinterDevice>> = PrinterManager.availablePrinters
    val isPrinting: StateFlow<Boolean> = PrinterManager.isPrinting

    fun openScanner(stage: com.example.data.model.ScanStage = com.example.data.model.ScanStage.DELIVERY, targetOrderId: String? = null) {
        if (targetOrderId != null) {
            _selectedOrderId.value = targetOrderId
        }
        _scanStage.value = stage
        _showScannerDialog.value = true
    }

    fun closeScanner() {
        _showScannerDialog.value = false
    }

    fun handleScanSuccess(result: com.example.data.model.ScanVerificationResult.Success) {
        val orderId = result.orderWithItems.order.id
        viewModelScope.launch {
            when (result.scanStage) {
                com.example.data.model.ScanStage.COLLECTION -> {
                    repository.updateOrderStatus(orderId, "COLLECTED_IN_INSPECTION")
                    _syncToastMessage.value = "تطابق جمع‌آوری موفق: سفارش $orderId به عنوان جمع‌آوری شده ثبت شد"
                }
                com.example.data.model.ScanStage.WORKSHOP -> {
                    repository.updateOrderStatus(orderId, "DELIVERED_TO_WORKSHOP")
                    _syncToastMessage.value = "تطابق ورودی انبار موفق: فرش‌های سفارش $orderId تحویل کارگاه گردید"
                }
                com.example.data.model.ScanStage.DELIVERY -> {
                    repository.updateOrderStatus(orderId, "DELIVERED_SETTLED")
                    _syncToastMessage.value = "تطابق تحویل مشتری موفق: فرش‌های سفارش $orderId به مشتری تحویل داده شد"
                }
            }
        }
    }

    fun reportScanMismatchToDispatch(reportText: String) {
        viewModelScope.launch {
            val currentOrder = selectedOrder.value?.order?.id ?: "GENERAL"
            repository.sendChatMessage(
                orderId = currentOrder,
                messageText = "🚨 " + reportText,
                sender = "DRIVER"
            )
            _syncToastMessage.value = "هشدار عدم تطابق به مرکز پشتیبانی ارسال گردید"
        }
    }

    fun selectOrder(orderId: String) {
        _selectedOrderId.value = orderId
    }

    fun setActiveTab(tabIndex: Int) {
        _activeTab.value = tabIndex
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleGpsTracking() {
        _isGpsActive.value = !_isGpsActive.value
        if (_isGpsActive.value) {
            // Log mock point
            viewModelScope.launch {
                repository.logGpsLocation(35.779, 51.405, 45.0f)
            }
        }
    }

    fun addCarpetItem(
        orderId: String,
        carpetType: String,
        lengthMeter: Double,
        widthMeter: Double,
        unitPricePerMeter: Long,
        requestedServices: List<String>,
        defects: List<String>,
        notes: String,
        barcodeTag: String = ""
    ) {
        val area = lengthMeter * widthMeter
        val itemTotalPrice = (area * unitPricePerMeter).toLong()
        val finalTag = if (barcodeTag.isNotBlank()) barcodeTag.trim().uppercase()
            else "ST-${orderId.takeLast(4)}-${(1..99).random().toString().padStart(2, '0')}"

        val item = CarpetItemEntity(
            orderId = orderId,
            carpetType = carpetType,
            lengthMeter = lengthMeter,
            widthMeter = widthMeter,
            areaSqMeter = area,
            unitPricePerMeter = unitPricePerMeter,
            requestedServicesJson = requestedServices.joinToString("، "),
            defectsJson = if (defects.isEmpty()) "بدون عیب" else defects.joinToString("، "),
            totalPrice = itemTotalPrice,
            notes = notes,
            barcodeTag = finalTag
        )
        viewModelScope.launch {
            repository.addCarpetItemToOrder(orderId, item)
        }
    }

    fun deleteCarpetItem(itemId: Long, orderId: String) {
        viewModelScope.launch {
            repository.removeCarpetItem(itemId, orderId)
        }
    }

    fun finalizeInvoiceRegistration(orderId: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, "COLLECTED_IN_INSPECTION")
            _syncToastMessage.value = "ثبت فاکتور سفارش $orderId انجام شد و به منوی تحویل انبار منتقل گردید"
            _activeTab.value = 1 // Return back to collection menu tab
        }
    }

    fun assignRackCode(orderId: String, rackCode: String) {
        viewModelScope.launch {
            repository.updateRackAssignment(orderId, rackCode)
            _syncToastMessage.value = "شماره قفسه $rackCode برای سفارش $orderId با موفقیت ثبت شد"
        }
    }

    fun confirmWarehouseHandover(orderId: String, rackCode: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.updateRackAssignment(orderId, rackCode)
                val isSynced = repository.syncWithWebPanel()
                _isSyncing.value = false
                if (isSynced) {
                    repository.updateOrderStatus(orderId, "DELIVERED_TO_WORKSHOP")
                    _syncToastMessage.value = "تأیید تحویل به انباردار و شماره قفسه $rackCode سفارش $orderId با موفقیت به پنل ارسال و از لیست حذف شد"
                } else {
                    _syncToastMessage.value = "خطا در ارسال اطلاعات به پنل! سفارش از لیست حذف نشد، لطفاً دوباره تلاش کنید."
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncToastMessage.value = "خطا در برقراری ارتباط با پنل انبار: ${e.localizedMessage ?: "مجدداً تلاش کنید"}"
            }
        }
    }

    fun returnToCleanWarehouse(orderId: String, rackCode: String, reason: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.updateRackAssignment(orderId, rackCode)
                repository.updateOrderStatus(orderId, "RETURNED_TO_CLEAN_WAREHOUSE")
                val isSynced = repository.syncWithWebPanel()
                _isSyncing.value = false
                _syncToastMessage.value = "سفارش $orderId به قفسه تمیز انبار ($rackCode) بازگردانده شد و جهت برنامه‌ریزی مجدد به پنل ارسال گردید."
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncToastMessage.value = "خطا در ثبت برگشت به انبار: ${e.localizedMessage ?: "مجدداً تلاش کنید"}"
            }
        }
    }

    fun settlePayment(
        orderId: String,
        paidAmount: Long,
        discountAmount: Long,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            repository.finalizeSettlement(orderId, paidAmount, discountAmount, paymentMethod)
            _syncToastMessage.value = "تسویه حساب سفارش $orderId نهایی شد"
        }
    }

    fun settleWithOffice(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val success = repository.syncWithWebPanel()
                repository.archiveSettledOrders()
                _isSyncing.value = false
                if (success) {
                    _syncToastMessage.value = "تسویه روزانه با دفتر مدیریت انجام شد و لیست تصفیه‌شده‌های امروز پاک گردید."
                    onSuccess()
                } else {
                    _syncToastMessage.value = "اطلاعات تسویه ذخیره شد و لیست تصفیه‌شده‌های امروز پاک گردید."
                    onSuccess()
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncToastMessage.value = "خطا در تسویه با دفتر: ${e.localizedMessage ?: "مجدداً تلاش کنید"}"
            }
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val currentOrder = selectedOrder.value?.order?.id ?: "GENERAL"
        viewModelScope.launch {
            repository.sendChatMessage(currentOrder, text, sender = "DRIVER")
            // Simulate dispatcher auto-reply if needed
            delay(1500)
            repository.sendChatMessage(
                currentOrder,
                "پیام شما توسط اپراتور دریافت شد. هماهنگی‌های لازم در حال انجام است.",
                sender = "DISPATCHER"
            )
        }
    }

    fun syncWithWebPanel() {
        viewModelScope.launch {
            _isSyncing.value = true
            val success = repository.syncWithWebPanel()
            _isSyncing.value = false
            if (success) {
                _syncToastMessage.value = "همگام‌سازی با پنل وب قالیشویی زمرد با موفقیت انجام شد"
            } else {
                _syncToastMessage.value = "خطا در ارتباط با سرور. اطلاعات در حافظه آفلاین حفظ شد"
            }
        }
    }

    fun clearToastMessage() {
        _syncToastMessage.value = null
    }

    fun scanBluetoothPrinters(context: Context) {
        PrinterManager.scanPrinters(context)
    }

    fun connectPrinter(device: BluetoothPrinterDevice) {
        viewModelScope.launch {
            PrinterManager.connectPrinter(device)
            _syncToastMessage.value = "به پرینتر ${device.name} متصل شدید"
        }
    }

    fun printOrderReceipt(
        title: String,
        orderWithItems: OrderWithItems,
        paymentMethod: String = "نقدی / کارتخوان"
    ) {
        viewModelScope.launch {
            val order = orderWithItems.order
            val itemsSummary = orderWithItems.items.map {
                "${it.carpetType} (${it.lengthMeter}x${it.widthMeter} م) - ${it.requestedServicesJson} - ${it.totalPrice} تومان"
            }
            PrinterManager.printReceipt(
                title = title,
                orderId = order.id,
                customerName = order.customerName,
                customerPhone = order.customerPhone,
                address = order.address,
                carpetDetails = itemsSummary.joinToString("\n"),
                totalPrice = order.totalAmount,
                discount = order.discountAmount,
                finalPrice = order.totalAmount - order.discountAmount,
                paymentStatus = paymentMethod,
                rackCode = order.rackCode
            )
            _syncToastMessage.value = "رسید حرارتی فاکتور ${order.id} ارسال به پرینتر شد"
        }
    }
}
