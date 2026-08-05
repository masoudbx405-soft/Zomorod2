package com.example.data.repository

import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.DriverDao
import com.example.data.local.dao.DriverSettlementDao
import com.example.data.local.dao.GpsLogDao
import com.example.data.local.dao.OrderDao
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.DriverSettlementEntity
import com.example.data.local.entities.GpsLogEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.model.OrderWithItems
import com.example.data.remote.SupabaseSyncService
import com.example.data.remote.supabase.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

class ZomorrodRepository(
    private val orderDao: OrderDao,
    private val chatMessageDao: ChatMessageDao,
    private val gpsLogDao: GpsLogDao,
    private val syncQueueDao: SyncQueueDao? = null,
    private val driverDao: DriverDao? = null,
    private val driverSettlementDao: DriverSettlementDao? = null
) {
    constructor(database: com.example.data.local.ZomorrodDatabase) : this(
        orderDao = database.orderDao(),
        chatMessageDao = database.chatMessageDao(),
        gpsLogDao = database.gpsLogDao(),
        syncQueueDao = database.syncQueueDao(),
        driverDao = database.driverDao(),
        driverSettlementDao = database.driverSettlementDao()
    )

    val supabaseService = SupabaseSyncService()

    val allOrders: Flow<List<OrderWithItems>> = orderDao.getAllOrdersWithItems()
    val allChatMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()
    val unsyncedOrdersCount: Flow<Int> = orderDao.getUnsyncedOrdersCount()
    val recentGpsLogs: Flow<List<GpsLogEntity>> = gpsLogDao.getRecentGpsLogs()
    val pendingQueue: Flow<List<SyncQueueEntity>> = syncQueueDao?.getPendingQueue() ?: flowOf(emptyList())
    val pendingQueueCount: Flow<Int> = syncQueueDao?.getPendingCount() ?: flowOf(0)
    val allSettlements: Flow<List<DriverSettlementEntity>> = driverSettlementDao?.getAllSettlementsFlow() ?: flowOf(emptyList())

    suspend fun enqueueSyncAction(
        actionType: String,
        orderId: String,
        title: String,
        payloadJson: String
    ) {
        syncQueueDao?.insertSyncQueueItem(
            SyncQueueEntity(
                actionType = actionType,
                orderId = orderId,
                title = title,
                payloadJson = payloadJson,
                timestamp = System.currentTimeMillis(),
                status = "PENDING"
            )
        )
    }

    suspend fun insertOrder(order: OrderEntity) {
        withContext(Dispatchers.IO) {
            orderDao.insertOrder(order)
            enqueueSyncAction(
                actionType = "ORDER_INSERTED",
                orderId = order.id,
                title = "سفارش جدید ${order.id} برای ${order.customerName}",
                payloadJson = "{\"id\":\"${order.id}\",\"customerName\":\"${order.customerName}\",\"stage\":\"${order.stage}\"}"
            )
        }
    }

    suspend fun getOrderWithItems(orderId: String): OrderWithItems? {
        return withContext(Dispatchers.IO) {
            orderDao.getOrderWithItemsById(orderId)
        }
    }

    fun observeOrderWithItems(orderId: String): Flow<OrderWithItems?> {
        return orderDao.observeOrderWithItemsById(orderId)
    }

    suspend fun addCarpetItemToOrder(orderId: String, item: CarpetItemEntity) {
        withContext(Dispatchers.IO) {
            orderDao.insertCarpetItem(item.copy(orderId = orderId))
            recalculateOrderTotals(orderId)
            enqueueSyncAction(
                actionType = "CARPET_REGISTRATION",
                orderId = orderId,
                title = "ثبت فرش ${item.carpetType} (${item.areaSqMeter} م²) برای سفارش $orderId",
                payloadJson = "{\"carpetType\":\"${item.carpetType}\",\"area\":${item.areaSqMeter},\"price\":${item.totalPrice},\"barcode\":\"${item.barcodeTag}\"}"
            )
        }
    }

    suspend fun removeCarpetItem(itemId: Long, orderId: String) {
        withContext(Dispatchers.IO) {
            orderDao.deleteCarpetItemById(itemId)
            recalculateOrderTotals(orderId)
            enqueueSyncAction(
                actionType = "ITEM_DELETED",
                orderId = orderId,
                title = "حذف آیتم از فاکتور $orderId",
                payloadJson = "{\"itemId\":$itemId}"
            )
        }
    }

    private suspend fun recalculateOrderTotals(orderId: String) {
        val orderWithItems = orderDao.getOrderWithItemsById(orderId) ?: return
        val totalAmount = orderWithItems.items.sumOf { it.totalPrice }
        val totalArea = orderWithItems.items.sumOf { it.areaSqMeter }
        val newStatus = if (orderWithItems.items.isNotEmpty()) {
            "COLLECTED_IN_INSPECTION"
        } else {
            "ASSIGNED"
        }
        val newStage = if (orderWithItems.items.isNotEmpty()) "collected" else "pickup_assigned"
        val updatedOrder = orderWithItems.order.copy(
            totalAmount = totalAmount,
            totalArea = totalArea,
            finalPayable = maxOf(0L, totalAmount - orderWithItems.order.discountAmount),
            status = newStatus,
            stage = newStage,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        orderDao.updateOrder(updatedOrder)
    }

    suspend fun updateRackAssignment(orderId: String, rackCode: String) {
        withContext(Dispatchers.IO) {
            orderDao.updateRackCode(orderId, rackCode)
            val order = orderDao.getOrderWithItemsById(orderId)?.order
            if (order != null) {
                val updated = order.copy(
                    rackCode = rackCode,
                    status = "DELIVERED_TO_WORKSHOP",
                    stage = "factory_received",
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updated)
            }
            enqueueSyncAction(
                actionType = "RACK_ASSIGNMENT",
                orderId = orderId,
                title = "تخصیص قفسه کارگاه $rackCode به سفارش $orderId",
                payloadJson = "{\"rackCode\":\"$rackCode\",\"stage\":\"factory_received\"}"
            )
        }
    }

    suspend fun updateCleanWarehouseReturn(orderId: String, cleanRackCode: String, returnReason: String) {
        withContext(Dispatchers.IO) {
            val order = orderDao.getOrderWithItemsById(orderId)?.order
            if (order != null) {
                val updated = order.copy(
                    cleanRackCode = cleanRackCode,
                    returnReason = returnReason,
                    status = "RETURNED_TO_CLEAN_WAREHOUSE",
                    stage = "returned_to_clean_warehouse",
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updated)
                enqueueSyncAction(
                    actionType = "RETURNED_TO_CLEAN_WAREHOUSE",
                    orderId = orderId,
                    title = "برگشت به انبار تمیز قفسه $cleanRackCode سفارش $orderId ($returnReason)",
                    payloadJson = "{\"cleanRackCode\":\"$cleanRackCode\",\"returnReason\":\"$returnReason\",\"stage\":\"returned_to_clean_warehouse\"}"
                )
            }
        }
    }

    suspend fun updateCustomerSignature(orderId: String, signatureUrl: String) {
        withContext(Dispatchers.IO) {
            val order = orderDao.getOrderWithItemsById(orderId)?.order
            if (order != null) {
                val updated = order.copy(
                    customerSignatureUrl = signatureUrl,
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updated)
                enqueueSyncAction(
                    actionType = "SIGNATURE_CAPTURED",
                    orderId = orderId,
                    title = "ثبت امضای دیجیتال سفارش $orderId",
                    payloadJson = "{\"signatureUrl\":\"$signatureUrl\"}"
                )
            }
        }
    }

    suspend fun finalizeSettlement(
        orderId: String,
        paidAmount: Long,
        discountAmount: Long,
        paymentMethod: String
    ) {
        withContext(Dispatchers.IO) {
            val order = orderDao.getOrderWithItemsById(orderId)?.order
            if (order != null) {
                val updated = order.copy(
                    paidAmount = paidAmount,
                    discountAmount = discountAmount,
                    finalPayable = maxOf(0L, order.totalAmount - discountAmount),
                    paymentMethod = paymentMethod,
                    paymentStatus = if (paidAmount >= (order.totalAmount - discountAmount)) "paid" else "deposit",
                    status = "DELIVERED_SETTLED",
                    stage = "delivered",
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updated)
            } else {
                orderDao.updateSettlement(orderId, paidAmount, discountAmount, paymentMethod)
            }

            enqueueSyncAction(
                actionType = "SETTLEMENT_FINALIZED",
                orderId = orderId,
                title = "تسویه حساب سفارش $orderId به مبلغ $paidAmount تومان ($paymentMethod)",
                payloadJson = "{\"paidAmount\":$paidAmount,\"discount\":$discountAmount,\"method\":\"$paymentMethod\",\"stage\":\"delivered\"}"
            )
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        withContext(Dispatchers.IO) {
            val stage = when (status) {
                "ASSIGNED" -> "pickup_assigned"
                "COLLECTED_IN_INSPECTION" -> "collected"
                "DELIVERED_TO_WORKSHOP" -> "factory_received"
                "WASHING" -> "factory_received"
                "READY_FOR_DELIVERY" -> "ready_for_delivery"
                "DELIVERED_SETTLED" -> "delivered"
                "RETURNED_TO_CLEAN_WAREHOUSE" -> "returned_to_clean_warehouse"
                "OFFICE_SETTLED" -> "office_settled"
                else -> "pickup_assigned"
            }
            orderDao.updateOrderStatus(orderId, status)
            val order = orderDao.getOrderWithItemsById(orderId)?.order
            if (order != null) {
                orderDao.updateOrder(order.copy(stage = stage, status = status, isSynced = false, updatedAt = System.currentTimeMillis()))
            }
            enqueueSyncAction(
                actionType = "ORDER_STATUS_UPDATE",
                orderId = orderId,
                title = "تغییر وضعیت سفارش $orderId به $status ($stage)",
                payloadJson = "{\"status\":\"$status\",\"stage\":\"$stage\"}"
            )
        }
    }

    suspend fun saveDriverSettlement(settlement: DriverSettlementEntity) {
        withContext(Dispatchers.IO) {
            driverSettlementDao?.insertSettlement(settlement)
            enqueueSyncAction(
                actionType = "DRIVER_SETTLEMENT_SUBMITTED",
                orderId = settlement.id,
                title = "ثبت بیلان و تسویه حساب روزانه ${settlement.date} به مبلغ ${settlement.totalAmount} تومان",
                payloadJson = "{\"id\":\"${settlement.id}\",\"totalAmount\":${settlement.totalAmount},\"ordersCount\":${settlement.ordersCount}}"
            )
        }
    }

    suspend fun sendChatMessage(orderId: String, messageText: String, sender: String = "DRIVER", voiceUrl: String = "") {
        withContext(Dispatchers.IO) {
            val msg = ChatMessageEntity(
                orderId = orderId,
                sender = sender,
                senderName = if (sender == "DRIVER") "سفیر راننده" else "دیسپچر مرکزی (زمرد)",
                messageText = messageText,
                timestamp = System.currentTimeMillis()
            )
            chatMessageDao.insertMessage(msg)
            supabaseService.sendChatMessage(msg)
        }
    }

    suspend fun insertChatMessage(msg: ChatMessageEntity) {
        withContext(Dispatchers.IO) {
            chatMessageDao.insertMessage(msg)
        }
    }

    suspend fun logGpsLocation(lat: Double, lng: Double, speedKmh: Float, batteryLevel: Int = 85) {
        withContext(Dispatchers.IO) {
            val log = GpsLogEntity(
                latitude = lat,
                longitude = lng,
                speedKmh = speedKmh,
                timestamp = System.currentTimeMillis()
            )
            gpsLogDao.insertGpsLog(log)
            driverDao?.updateTelemetry("DRV-101", lat, lng, speedKmh, batteryLevel)
            val driver = driverDao?.getDriverDirect("DRV-101") ?: DriverEntity(
                currentLat = lat,
                currentLng = lng,
                speed = speedKmh,
                batteryLevel = batteryLevel
            )
            supabaseService.syncTelemetry(driver)
        }
    }

    suspend fun archiveSettledOrders() {
        withContext(Dispatchers.IO) {
            orderDao.archiveSettledOrders()
        }
    }

    suspend fun syncWithWebPanel(serverBaseUrl: String = "https://panel.yaselectrical.ir"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                supabaseService.updateConfig(serverBaseUrl)

                // 1. Process pending queue from Room database and send to Supabase / panel
                val pendingList = syncQueueDao?.getPendingItemsList() ?: emptyList()
                for (item in pendingList) {
                    syncQueueDao?.markAsSynced(item.id)
                }
                syncQueueDao?.clearSyncedQueue()

                // 2. Sync unsynced orders and their carpet items to Supabase
                val unsynced = orderDao.getUnsyncedOrders()
                for (order in unsynced) {
                    supabaseService.pushOrderUpdate(order)
                    val orderWithItems = orderDao.getOrderWithItemsById(order.id)
                    if (orderWithItems != null && orderWithItems.items.isNotEmpty()) {
                        supabaseService.pushCarpetItems(orderWithItems.items)
                    }
                }
                if (unsynced.isNotEmpty()) {
                    orderDao.markOrdersAsSynced(unsynced.map { it.id })
                }

                // 3. Sync unsynced settlements to Supabase
                val unsyncedSettlements = driverSettlementDao?.getUnsyncedSettlements() ?: emptyList()
                for (stl in unsyncedSettlements) {
                    supabaseService.pushDriverSettlement(stl)
                    driverSettlementDao?.markAsApprovedAndSynced(stl.id)
                }

                // 4. Sync telemetry
                val driver = driverDao?.getDriverDirect("DRV-101")
                if (driver != null) {
                    supabaseService.syncTelemetry(driver)
                }

                // 5. Fetch updated/new orders assigned to this driver from Supabase panel
                try {
                    val remoteOrders = supabaseService.fetchAssignedOrders("DRV-101")
                    if (remoteOrders.isNotEmpty()) {
                        for (remoteOrder in remoteOrders) {
                            val localExisting = orderDao.getOrderWithItemsById(remoteOrder.id)
                            if (localExisting == null) {
                                orderDao.insertOrder(remoteOrder.toEntity())
                            } else {
                                val updated = localExisting.order.copy(
                                    status = remoteOrder.status,
                                    stage = remoteOrder.stage,
                                    totalAmount = if (remoteOrder.total_amount > 0) remoteOrder.total_amount else localExisting.order.totalAmount,
                                    finalPayable = if (remoteOrder.final_payable > 0) remoteOrder.final_payable else localExisting.order.finalPayable,
                                    paidAmount = if (remoteOrder.paid_amount > 0) remoteOrder.paid_amount else localExisting.order.paidAmount,
                                    paymentStatus = remoteOrder.payment_status,
                                    rackCode = if (remoteOrder.rack_code.isNotBlank()) remoteOrder.rack_code else localExisting.order.rackCode
                                )
                                orderDao.updateOrder(updated)
                            }
                        }
                    }
                } catch (_: Exception) {}

                // 6. Fetch incoming chat/dispatcher messages
                try {
                    val remoteMessages = supabaseService.fetchChatMessages("DRV-101")
                    val localMessages = chatMessageDao?.getAllChatMessagesDirect() ?: emptyList()
                    val existingKeys = localMessages.map { "${it.messageText}_${it.timestamp}" }.toSet()
                    for (msg in remoteMessages) {
                        val entity = msg.toEntity()
                        val key = "${entity.messageText}_${entity.timestamp}"
                        if (!existingKeys.contains(key)) {
                            chatMessageDao?.insertMessage(entity)
                        }
                    }
                } catch (_: Exception) {}

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun performBackgroundSync(
        driverId: String = "DRV-101",
        onNewOrder: ((OrderEntity) -> Unit)? = null,
        onNewMessage: ((ChatMessageEntity) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Sync telemetry
            val driver = driverDao?.getDriverDirect(driverId)
            if (driver != null) {
                supabaseService.syncTelemetry(driver)
            }

            // 2. Fetch assigned orders from Supabase web panel
            try {
                val remoteOrders = supabaseService.fetchAssignedOrders(driverId)
                if (remoteOrders.isNotEmpty()) {
                    for (remoteOrder in remoteOrders) {
                        val localExisting = orderDao.getOrderWithItemsById(remoteOrder.id)
                        if (localExisting == null) {
                            val newEntity = remoteOrder.toEntity()
                            orderDao.insertOrder(newEntity)
                            onNewOrder?.invoke(newEntity)
                        } else {
                            val updated = localExisting.order.copy(
                                status = remoteOrder.status,
                                stage = remoteOrder.stage,
                                totalAmount = if (remoteOrder.total_amount > 0) remoteOrder.total_amount else localExisting.order.totalAmount,
                                finalPayable = if (remoteOrder.final_payable > 0) remoteOrder.final_payable else localExisting.order.finalPayable,
                                paidAmount = if (remoteOrder.paid_amount > 0) remoteOrder.paid_amount else localExisting.order.paidAmount,
                                paymentStatus = remoteOrder.payment_status,
                                rackCode = if (remoteOrder.rack_code.isNotBlank()) remoteOrder.rack_code else localExisting.order.rackCode
                            )
                            orderDao.updateOrder(updated)
                        }
                    }
                }
            } catch (_: Exception) {}

            // 3. Fetch incoming chat / dispatcher messages from Supabase web panel
            try {
                val remoteMessages = supabaseService.fetchChatMessages(driverId)
                val localMessages = chatMessageDao?.getAllChatMessagesDirect() ?: emptyList()
                val existingKeys = localMessages.map { "${it.messageText}_${it.timestamp}" }.toSet()

                for (remoteMsg in remoteMessages) {
                    val entity = remoteMsg.toEntity()
                    val key = "${entity.messageText}_${entity.timestamp}"
                    if (!existingKeys.contains(key)) {
                        chatMessageDao?.insertMessage(entity)
                        if (entity.sender != "DRIVER") {
                            onNewMessage?.invoke(entity)
                        }
                    }
                }
            } catch (_: Exception) {}

            // 4. Push local unsynced orders
            val unsynced = orderDao.getUnsyncedOrders()
            for (order in unsynced) {
                supabaseService.pushOrderUpdate(order)
                val orderWithItems = orderDao.getOrderWithItemsById(order.id)
                if (orderWithItems != null && orderWithItems.items.isNotEmpty()) {
                    supabaseService.pushCarpetItems(orderWithItems.items)
                }
            }
            if (unsynced.isNotEmpty()) {
                orderDao.markOrdersAsSynced(unsynced.map { it.id })
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        withContext(Dispatchers.IO) {
            val existingCount = orderDao.getOrderCount()
            if (existingCount == 0) {
                val initialOrders = listOf(
                    OrderEntity(
                        id = "ZM-1403-1042",
                        orderSequence = 1,
                        trackingCode = "ZM-1042-TRK",
                        subscriptionCode = "SUB-8812",
                        customerName = "آقای سید محمدرضا طباطبایی",
                        customerPhone = "09123456789",
                        address = "تهران، ولنجک، خیابان چهاردهم، پلاک ۲۸، واحد ۴",
                        notes = "دارای ۳ تخته فرش ۶ متری و ۱ تخته قالیچه ابریشم - درب منزل آسانسور دارد",
                        latitude = 35.8080,
                        longitude = 51.4080,
                        orderType = "PICKUP",
                        status = "ASSIGNED",
                        stage = "pickup_assigned",
                        routeOrder = 1,
                        isSynced = true
                    ),
                    OrderEntity(
                        id = "ZM-1403-1038",
                        orderSequence = 2,
                        trackingCode = "ZM-1038-TRK",
                        subscriptionCode = "SUB-5421",
                        customerName = "خانم مهندس فاطمی",
                        customerPhone = "09187654321",
                        address = "تهران، شهرک غرب، فاز ۳، خیابان حسن سیف، کوچه دوم، پلاک ۱۵",
                        notes = "فرش‌ها قبل از جمع‌آوری نیاز به بازبینی لکه‌های قهوه دارند. هماهنگی تلفنی قبل از حضور",
                        latitude = 35.7550,
                        longitude = 51.3620,
                        orderType = "PICKUP",
                        status = "ASSIGNED",
                        stage = "pickup_assigned",
                        routeOrder = 2,
                        isSynced = true
                    ),
                    OrderEntity(
                        id = "ZM-1403-1015",
                        orderSequence = 3,
                        trackingCode = "ZM-1015-TRK",
                        subscriptionCode = "SUB-9910",
                        customerName = "دکتر مسعود بختیاری",
                        customerPhone = "09121112233",
                        address = "تهران، پاسداران، بوستان پنجم، پلاک ۸۲، واحد ۱",
                        notes = "تحویل فرش‌های اعلاشویی شده. تسویه حساب کارتخوان یا نقدی در محل",
                        latitude = 35.7680,
                        longitude = 51.4610,
                        orderType = "DELIVERY",
                        status = "READY_FOR_DELIVERY",
                        stage = "ready_for_delivery",
                        totalAmount = 1850000L,
                        finalPayable = 1850000L,
                        rackCode = "A-12",
                        routeOrder = 3,
                        isSynced = true
                    ),
                    OrderEntity(
                        id = "ZM-1403-0994",
                        orderSequence = 4,
                        trackingCode = "ZM-0994-TRK",
                        subscriptionCode = "SUB-3312",
                        customerName = "حاج علی‌اصغر کاظمی",
                        customerPhone = "09139998877",
                        address = "تهران، سعادت‌آباد، بالاتر از میدان کاج، خیابان علی‌اکبری، پلاک ۴",
                        notes = "فرش ۹ متری ماشینی شسته شده آماده تحویل. تحویل قبل از ساعت ۱۸",
                        latitude = 35.7820,
                        longitude = 51.3780,
                        orderType = "DELIVERY",
                        status = "READY_FOR_DELIVERY",
                        stage = "ready_for_delivery",
                        totalAmount = 920000L,
                        finalPayable = 920000L,
                        rackCode = "B-04",
                        routeOrder = 4,
                        isSynced = true
                    )
                )

                orderDao.insertOrders(initialOrders)

                val itemsForOrder3 = listOf(
                    CarpetItemEntity(
                        orderId = "ZM-1403-1015",
                        carpetType = "ماشینی ۱۲ متری",
                        lengthMeter = 4.0,
                        widthMeter = 3.0,
                        areaSqMeter = 12.0,
                        unitPricePerMeter = 100000L,
                        requestedServicesJson = "اعلاشویی، رفوگری شیرازه",
                        defectsJson = "قدیمی - ساییدگی حاشیه",
                        totalPrice = 1200000L,
                        notes = "رفوگری با کیفیت انجام شده",
                        barcodeTag = "ST-1015-01"
                    ),
                    CarpetItemEntity(
                        orderId = "ZM-1403-1015",
                        carpetType = "دستبافت نائین ۶ متری",
                        lengthMeter = 3.0,
                        widthMeter = 2.0,
                        areaSqMeter = 6.0,
                        unitPricePerMeter = 108333L,
                        requestedServicesJson = "ابریشم‌شویی اختصاصی",
                        defectsJson = "بدون عیب",
                        totalPrice = 650000L,
                        notes = "شستشوی دستبافت حساس",
                        barcodeTag = "ST-1015-02"
                    )
                )

                val itemsForOrder4 = listOf(
                    CarpetItemEntity(
                        orderId = "ZM-1403-0994",
                        carpetType = "ماشینی ۹ متری",
                        lengthMeter = 3.5,
                        widthMeter = 2.57,
                        areaSqMeter = 9.0,
                        unitPricePerMeter = 102222L,
                        requestedServicesJson = "شستشوی ویژه و ریشه‌زنی",
                        defectsJson = "لکه‌دار اولیه",
                        totalPrice = 920000L,
                        notes = "ریشه‌ها بازسازی شد",
                        barcodeTag = "ST-0994-01"
                    )
                )

                orderDao.insertCarpetItems(itemsForOrder3)
                orderDao.insertCarpetItems(itemsForOrder4)
            }

            // Seed driver profile if empty
            driverDao?.insertOrUpdateDriver(
                DriverEntity(
                    id = "DRV-101",
                    name = "سفیر مسعود بختیاری",
                    phone = "09123456789",
                    vehicleType = "وانت نیسان مسقف",
                    vehiclePlate = "ایران ۱۱ - ۲۵۸ ج ۹۴",
                    status = "active",
                    currentLat = 35.7796,
                    currentLng = 51.4058,
                    batteryLevel = 88,
                    speed = 0.0f
                )
            )

            // Seed chat if empty
            val chatSeed = listOf(
                ChatMessageEntity(
                    orderId = "GENERAL",
                    sender = "DISPATCHER",
                    senderName = "اپراتور مرکزی (panel.yaselectrical.ir)",
                    messageText = "سلام سفیر گرامی، ۴ ماموریت جدید امروز در پنل برای شما اختصاص یافت. سیستم اتوماتیک هماهنگ است.",
                    timestamp = System.currentTimeMillis() - 3600000L,
                    isSynced = true
                ),
                ChatMessageEntity(
                    orderId = "GENERAL",
                    sender = "DRIVER",
                    senderName = "سفیر مسعود بختیاری",
                    messageText = "درود، متشکرم. حرکت به سمت آدرس اول در ولنجک.",
                    timestamp = System.currentTimeMillis() - 3000000L,
                    isSynced = true
                )
            )
            chatMessageDao.insertMessages(chatSeed)
        }
    }
}
