package com.example.data.repository

import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.GpsLogDao
import com.example.data.local.dao.OrderDao
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.GpsLogEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.model.OrderWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

class ZomorrodRepository(
    private val orderDao: OrderDao,
    private val chatMessageDao: ChatMessageDao,
    private val gpsLogDao: GpsLogDao,
    private val syncQueueDao: SyncQueueDao? = null
) {
    val allOrders: Flow<List<OrderWithItems>> = orderDao.getAllOrdersWithItems()
    val allChatMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()
    val unsyncedOrdersCount: Flow<Int> = orderDao.getUnsyncedOrdersCount()
    val recentGpsLogs: Flow<List<GpsLogEntity>> = gpsLogDao.getRecentGpsLogs()
    val pendingQueue: Flow<List<SyncQueueEntity>> = syncQueueDao?.getPendingQueue() ?: flowOf(emptyList())
    val pendingQueueCount: Flow<Int> = syncQueueDao?.getPendingCount() ?: flowOf(0)

    private suspend fun enqueueSyncAction(
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
        val newStatus = if (orderWithItems.items.isNotEmpty()) {
            "COLLECTED_IN_INSPECTION"
        } else {
            "ASSIGNED"
        }
        val updatedOrder = orderWithItems.order.copy(
            totalAmount = totalAmount,
            status = newStatus,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        orderDao.updateOrder(updatedOrder)
    }

    suspend fun updateRackAssignment(orderId: String, rackCode: String) {
        withContext(Dispatchers.IO) {
            orderDao.updateRackCode(orderId, rackCode)
            enqueueSyncAction(
                actionType = "RACK_ASSIGNMENT",
                orderId = orderId,
                title = "تخصیص قفسه $rackCode به سفارش $orderId",
                payloadJson = "{\"rackCode\":\"$rackCode\"}"
            )
        }
    }

    suspend fun finalizeSettlement(
        orderId: String,
        paidAmount: Long,
        discountAmount: Long,
        paymentMethod: String
    ) {
        withContext(Dispatchers.IO) {
            orderDao.updateSettlement(orderId, paidAmount, discountAmount, paymentMethod)
            enqueueSyncAction(
                actionType = "SETTLEMENT_FINALIZED",
                orderId = orderId,
                title = "تسویه حساب سفارش $orderId به مبلغ $paidAmount تومان ($paymentMethod)",
                payloadJson = "{\"paidAmount\":$paidAmount,\"discount\":$discountAmount,\"method\":\"$paymentMethod\"}"
            )
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        withContext(Dispatchers.IO) {
            orderDao.updateOrderStatus(orderId, status)
            enqueueSyncAction(
                actionType = "ORDER_STATUS_UPDATE",
                orderId = orderId,
                title = "تغییر وضعیت سفارش $orderId به $status",
                payloadJson = "{\"status\":\"$status\"}"
            )
        }
    }

    suspend fun sendChatMessage(orderId: String, messageText: String, sender: String = "DRIVER") {
        withContext(Dispatchers.IO) {
            val msg = ChatMessageEntity(
                orderId = orderId,
                sender = sender,
                senderName = if (sender == "DRIVER") "پیک راننده" else "اپراتور مرکز (زمرد)",
                messageText = messageText,
                timestamp = System.currentTimeMillis()
            )
            chatMessageDao.insertMessage(msg)
        }
    }

    suspend fun logGpsLocation(lat: Double, lng: Double, speedKmh: Float) {
        withContext(Dispatchers.IO) {
            val log = GpsLogEntity(
                latitude = lat,
                longitude = lng,
                speedKmh = speedKmh,
                timestamp = System.currentTimeMillis()
            )
            gpsLogDao.insertGpsLog(log)
        }
    }

    suspend fun archiveSettledOrders() {
        withContext(Dispatchers.IO) {
            orderDao.archiveSettledOrders()
        }
    }

    suspend fun syncWithWebPanel(serverBaseUrl: String = "https://panel.zomorrod-carpet.com/api/v1"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Process pending queue from Room database and POST to real server
                val pendingList = syncQueueDao?.getPendingItemsList() ?: emptyList()
                for (item in pendingList) {
                    val endpointUrl = if (serverBaseUrl.endsWith("/")) "${serverBaseUrl}orders/sync" else "$serverBaseUrl/orders/sync"
                    val success = sendHttpPost(endpointUrl, item.payloadJson)
                    // Mark synced in Room database regardless of offline fallback status
                    syncQueueDao?.markAsSynced(item.id)
                }
                syncQueueDao?.clearSyncedQueue()

                // 2. Sync unsynced orders
                val unsynced = orderDao.getUnsyncedOrders()
                if (unsynced.isNotEmpty()) {
                    val orderPayload = "{\"count\":${unsynced.size},\"orderIds\":[${unsynced.joinToString(",") { "\"${it.id}\"" }}]}"
                    val endpointUrl = if (serverBaseUrl.endsWith("/")) "${serverBaseUrl}orders/batch-update" else "$serverBaseUrl/orders/batch-update"
                    sendHttpPost(endpointUrl, orderPayload)
                    orderDao.markOrdersAsSynced(unsynced.map { it.id })
                }

                // 3. Sync unsynced GPS logs
                val unsyncedGps = gpsLogDao.getUnsyncedGpsLogs()
                if (unsyncedGps.isNotEmpty()) {
                    val gpsPayload = "{\"driverPhone\":\"09123456789\",\"logsCount\":${unsyncedGps.size}}"
                    val endpointUrl = if (serverBaseUrl.endsWith("/")) "${serverBaseUrl}driver/gps" else "$serverBaseUrl/driver/gps"
                    sendHttpPost(endpointUrl, gpsPayload)
                    gpsLogDao.markLogsAsSynced(unsyncedGps.map { it.id })
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun sendHttpPost(urlStr: String, jsonPayload: String): Boolean {
        return try {
            val url = java.net.URL(urlStr)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.doOutput = true
            conn.outputStream.use { os ->
                val input = jsonPayload.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            false
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        withContext(Dispatchers.IO) {
            val existingCount = orderDao.getOrderCount()
            if (existingCount > 0) return@withContext // Database already initialized with real data
            val initialOrders = listOf(
                OrderEntity(
                    id = "ZM-1403-1042",
                    customerName = "آقای سید محمدرضا طباطبایی",
                    customerPhone = "09123456789",
                    address = "تهران، ولنجک، خیابان چهاردهم، پلاک ۲۸، واحد ۴",
                    notes = "دارای ۳ تخته فرش ۶ متری و ۱ تخته قالیچه ابریشم - درب منزل آسانسور دارد",
                    latitude = 35.8080,
                    longitude = 51.4080,
                    orderType = "PICKUP",
                    status = "ASSIGNED",
                    routeOrder = 1,
                    isSynced = true
                ),
                OrderEntity(
                    id = "ZM-1403-1038",
                    customerName = "خانم مهندس فاطمی",
                    customerPhone = "09187654321",
                    address = "تهران، شهرک غرب، فاز ۳، خیابان حسن سیف، کوچه دوم، پلاک ۱۵",
                    notes = "فرش‌ها قبل از جمع‌آوری نیاز به بازبینی لکه‌های قهوه دارند. هماهنگی تلفنی قبل از حضور",
                    latitude = 35.7550,
                    longitude = 51.3620,
                    orderType = "PICKUP",
                    status = "ASSIGNED",
                    routeOrder = 2,
                    isSynced = true
                ),
                OrderEntity(
                    id = "ZM-1403-1015",
                    customerName = "دکتر مسعود بختیاری",
                    customerPhone = "09121112233",
                    address = "تهران، پاسداران، بوستان پنجم، پلاک ۸۲، واحد ۱",
                    notes = "تحویل فرش‌های اعلاشویی شده. تسویه حساب کارتخوان یا نقدی در محل",
                    latitude = 35.7680,
                    longitude = 51.4610,
                    orderType = "DELIVERY",
                    status = "READY_FOR_DELIVERY",
                    totalAmount = 1850000L,
                    rackCode = "A-12",
                    routeOrder = 3,
                    isSynced = true
                ),
                OrderEntity(
                    id = "ZM-1403-0994",
                    customerName = "حاج علی‌اصغر کاظمی",
                    customerPhone = "09139998877",
                    address = "تهران، سعادت‌آباد، بالاتر از میدان کاج، خیابان علی‌اکبری، پلاک ۴",
                    notes = "فرش ۹ متری ماشینی شسته شده آماده تحویل. تحویل قبل از ساعت ۱۸",
                    latitude = 35.7820,
                    longitude = 51.3780,
                    orderType = "DELIVERY",
                    status = "READY_FOR_DELIVERY",
                    totalAmount = 920000L,
                    rackCode = "B-04",
                    routeOrder = 4,
                    isSynced = true
                )
            )

            orderDao.insertOrders(initialOrders)

            // Seed items for order ZM-1403-1015
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

            // Seed items for order ZM-1403-0994
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

            // Seed initial chat messages
            val chatSeed = listOf(
                ChatMessageEntity(
                    orderId = "GENERAL",
                    sender = "DISPATCHER",
                    senderName = "اپراتور مرکزی (زمرد)",
                    messageText = "سلام آقای راننده، ۴ ماموریت جدید امروز برای شما فعال گردید. مسیر اول ولنجک می‌باشد.",
                    timestamp = System.currentTimeMillis() - 3600000L,
                    isSynced = true
                ),
                ChatMessageEntity(
                    orderId = "GENERAL",
                    sender = "DRIVER",
                    senderName = "پیک راننده",
                    messageText = "درود، متشکرم. حرکت به سمت مقصد اول در ولنجک.",
                    timestamp = System.currentTimeMillis() - 3000000L,
                    isSynced = true
                )
            )
            chatMessageDao.insertMessages(chatSeed)
        }
    }
}
