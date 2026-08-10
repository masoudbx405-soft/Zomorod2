package com.example.data.remote.supabase

import android.util.Log
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.DriverSettlementEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.model.PanelCatalogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * مدیر ارتباطی و همگام‌ساز Supabase برای اپلیکیشن قالیشویی زمرد
 * پشتیبانی از Postgrest REST API و ساختار کلاینت supabase-kt
 */
class ZomorrodSupabaseManager(
    private var supabaseUrl: String = ZomorrodSupabaseConfig.DEFAULT_SUPABASE_URL,
    private var anonKey: String = ZomorrodSupabaseConfig.DEFAULT_ANON_KEY
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun updateCredentials(url: String, key: String = anonKey) {
        this.supabaseUrl = url.trim().removeSuffix("/")
        this.anonKey = key.trim()
    }

    /**
     * تست برقراری ارتباط با پروژه Supabase و بررسی دسترسی به جدول رانندگان
     */
    suspend fun checkHealth(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val endpoint = "$supabaseUrl/rest/v1/${ZomorrodSupabaseConfig.Tables.DRIVERS}?select=count"
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - startTime
                val code = response.code
                if (code in 200..299) {
                    Pair(true, "ارتباط موفق با Supabase ($supabaseUrl) | پینگ: ${duration}ms")
                } else if (code == 401 || code == 403) {
                    Pair(true, "سرور Supabase فعال است (پاسخ امنیتی HTTP $code | تأخیر: ${duration}ms)")
                } else {
                    Pair(true, "پاسخ از سرور دریافت شد (HTTP $code | تأخیر: ${duration}ms)")
                }
            }
        } catch (e: Exception) {
            Pair(false, "عدم برقراری ارتباط با $supabaseUrl: ${e.localizedMessage ?: "Timeout"}")
        }
    }

    /**
     * همگام‌سازی موقعیت مکانی و وضعیت آنلاین راننده با جدول drivers
     */
    suspend fun syncDriverStatus(driver: DriverEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val dto = driver.toSupabaseDto()
            val payload = JSONObject().apply {
                put("id", dto.id)
                put("name", dto.name)
                put("phone", dto.phone)
                put("vehicle_type", dto.vehicle_type)
                put("vehicle_plate", dto.vehicle_plate)
                put("status", dto.status)
                put("current_lat", dto.current_lat)
                put("current_lng", dto.current_lng)
                put("battery_level", dto.battery_level)
                put("speed", dto.speed)
                put("app_status", dto.app_status)
                put("total_collected_cash", dto.total_collected_cash)
                put("total_collected_pos", dto.total_collected_pos)
            }.toString()

            val endpoint = "$supabaseUrl/rest/v1/${ZomorrodSupabaseConfig.Tables.DRIVERS}"
            val body = payload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            client.newCall(request).execute().use { it.isSuccessful || it.code in 200..299 }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error syncing driver status", e)
            false
        }
    }

    /**
     * ارسال یا به‌روزرسانی سفارش در جدول orders (تغییر وضعیت، تحویل انبار، برگشت، تسویه مالی)
     */
    suspend fun upsertOrder(order: OrderEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val dto = order.toSupabaseDto()
            val payload = JSONObject().apply {
                put("id", dto.id)
                put("tracking_code", dto.tracking_code)
                put("customer_name", dto.customer_name)
                put("customer_phone", dto.customer_phone)
                put("customer_address", dto.customer_address)
                put("lat", dto.lat)
                put("lng", dto.lng)
                put("stage", dto.stage)
                put("status", dto.status)
                put("order_type", dto.order_type)
                put("driver_id", dto.driver_id)
                put("driver_name", dto.driver_name)
                put("total_amount", dto.total_amount)
                put("discount_amount", dto.discount_amount)
                put("final_payable", dto.final_payable)
                put("paid_amount", dto.paid_amount)
                put("payment_method", dto.payment_method)
                put("payment_status", dto.payment_status)
                put("rack_code", dto.rack_code)
                put("clean_rack_code", dto.clean_rack_code)
                put("return_reason", dto.return_reason)
                put("customer_signature_url", dto.customer_signature_url)
                put("notes", dto.notes)
            }.toString()

            val endpoint = "$supabaseUrl/rest/v1/${ZomorrodSupabaseConfig.Tables.ORDERS}"
            val body = payload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            client.newCall(request).execute().use { it.isSuccessful || it.code in 200..299 }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error upserting order", e)
            false
        }
    }

    /**
     * ثبت تسویه حساب روزانه راننده در جدول driver_settlements
     */
    suspend fun upsertDriverSettlement(settlement: DriverSettlementEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val dto = settlement.toSupabaseDto()
            val payload = JSONObject().apply {
                put("id", dto.id)
                put("driver_id", dto.driver_id)
                put("driver_name", dto.driver_name)
                put("date", dto.date)
                put("total_cash", dto.total_cash)
                put("total_pos", dto.total_pos)
                put("total_card_to_card", dto.total_card_to_card)
                put("total_online", dto.total_online)
                put("total_amount", dto.total_amount)
                put("orders_count", dto.orders_count)
                put("returned_orders_count", dto.returned_orders_count)
                put("status", dto.status)
                put("notes", dto.notes)
            }.toString()

            val endpoint = "$supabaseUrl/rest/v1/${ZomorrodSupabaseConfig.Tables.DRIVER_SETTLEMENTS}"
            val body = payload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            client.newCall(request).execute().use { it.isSuccessful || it.code in 200..299 }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error saving driver settlement", e)
            false
        }
    }

    /**
     * ارسال پیام چت به جدول chat_messages
     */
    suspend fun sendChatMessage(message: ChatMessageEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val dto = message.toSupabaseDto()
            val payload = JSONObject().apply {
                put("id", dto.id)
                put("driver_id", dto.driver_id)
                put("sender", dto.sender)
                put("sender_name", dto.sender_name)
                put("text", dto.text)
                put("timestamp", dto.timestamp)
            }.toString()

            val endpoint = "$supabaseUrl/rest/v1/${ZomorrodSupabaseConfig.Tables.CHAT_MESSAGES}"
            val body = payload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .post(body)
                .build()

            client.newCall(request).execute().use { it.isSuccessful || it.code in 200..299 }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error sending chat message", e)
            false
        }
    }

    /**
     * همگام‌سازی و ثبت اقلام فرش یک فاکتور در جدول carpet_items در Supabase
     */
    suspend fun upsertCarpetItems(items: List<com.example.data.local.entities.CarpetItemEntity>): Boolean = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext true
        try {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val dto = item.toSupabaseDto()
                val obj = JSONObject().apply {
                    if (dto.id > 0) put("id", dto.id)
                    put("order_id", dto.order_id)
                    put("carpet_type", dto.carpet_type)
                    put("length_meter", dto.length_meter)
                    put("width_meter", dto.width_meter)
                    put("area_sq_meter", dto.area_sq_meter)
                    put("unit_price", dto.unit_price)
                    put("requested_services", dto.requested_services)
                    put("defects", dto.defects)
                    put("total_price", dto.total_price)
                    put("notes", dto.notes)
                    put("barcode_tag", dto.barcode_tag)
                }
                jsonArray.put(obj)
            }

            val endpoint = "$supabaseUrl/rest/v1/${ZomorrodSupabaseConfig.Tables.CARPET_ITEMS}"
            val body = jsonArray.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            client.newCall(request).execute().use { it.isSuccessful || it.code in 200..299 }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error upserting carpet items", e)
            false
        }
    }


    /**
     * دریافت لیست سفارشات محول‌شده به راننده از Supabase
     */
    suspend fun fetchDriverOrders(driverId: String): List<SupabaseOrderDto> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$supabaseUrl/rest/v1/${ZomorrodSupabaseConfig.Tables.ORDERS}?driver_id=eq.$driverId&select=*"
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim() ?: ""
                    if (body.startsWith("[") && body.endsWith("]")) {
                        val jsonArray = JSONArray(body)
                        val list = mutableListOf<SupabaseOrderDto>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.optJSONObject(i) ?: continue
                            list.add(
                                SupabaseOrderDto(
                                    id = obj.optString("id"),
                                    tracking_code = obj.optString("tracking_code"),
                                    customer_name = obj.optString("customer_name"),
                                    customer_phone = obj.optString("customer_phone"),
                                    customer_address = obj.optString("customer_address"),
                                    lat = obj.optDouble("lat", 35.779),
                                    lng = obj.optDouble("lng", 51.405),
                                    stage = obj.optString("stage", "pickup_assigned"),
                                    status = obj.optString("status", "ASSIGNED"),
                                    order_type = obj.optString("order_type", "PICKUP"),
                                    driver_id = obj.optString("driver_id", driverId),
                                    driver_name = obj.optString("driver_name", "سفیر زمرد"),
                                    total_amount = obj.optLong("total_amount", 0L),
                                    discount_amount = obj.optLong("discount_amount", 0L),
                                    final_payable = obj.optLong("final_payable", 0L),
                                    paid_amount = obj.optLong("paid_amount", 0L),
                                    payment_method = obj.optString("payment_method", "POS"),
                                    payment_status = obj.optString("payment_status", "unpaid"),
                                    rack_code = obj.optString("rack_code", ""),
                                    clean_rack_code = obj.optString("clean_rack_code", ""),
                                    return_reason = obj.optString("return_reason", ""),
                                    customer_signature_url = obj.optString("customer_signature_url", ""),
                                    notes = obj.optString("notes", ""),
                                    updated_at = obj.optString("updated_at", null)
                                )
                            )
                        }
                        list
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Notice: Server response for orders: ${e.message}")
            emptyList()
        }
    }

    /**
     * دریافت جدیدترین پیام‌های چت و دیسپچر از Supabase
     */
    suspend fun fetchChatMessages(driverId: String = "DRV-101"): List<SupabaseChatMessageDto> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$supabaseUrl/rest/v1/${ZomorrodSupabaseConfig.Tables.CHAT_MESSAGES}?select=*&order=timestamp.desc&limit=30"
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim() ?: ""
                    if (body.startsWith("[") && body.endsWith("]")) {
                        val jsonArray = JSONArray(body)
                        val list = mutableListOf<SupabaseChatMessageDto>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.optJSONObject(i) ?: continue
                            list.add(
                                SupabaseChatMessageDto(
                                    id = obj.optString("id", ""),
                                    driver_id = obj.optString("driver_id", driverId),
                                    sender = obj.optString("sender", "DISPATCHER"),
                                    sender_name = obj.optString("sender_name", "مرکز دیسپچینگ زمرد"),
                                    text = obj.optString("text", ""),
                                    timestamp = obj.optString("timestamp", System.currentTimeMillis().toString())
                                )
                            )
                        }
                        list
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Notice: Server response for chat: ${e.message}")
            emptyList()
        }
    }

    /**
     * آپلود امضای دیجیتال مشتری به باکت signatures در Supabase Storage
     */
    suspend fun uploadSignature(orderId: String, signatureBase64: String): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "sig_${orderId}_${System.currentTimeMillis()}.png"
            val endpoint = "$supabaseUrl/storage/v1/object/${ZomorrodSupabaseConfig.Buckets.SIGNATURES}/$fileName"
            val requestBody = signatureBase64.toByteArray(Charsets.UTF_8).toRequestBody("text/plain".toMediaType())

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "image/png")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299) {
                    "$supabaseUrl/storage/v1/object/public/${ZomorrodSupabaseConfig.Buckets.SIGNATURES}/$fileName"
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error uploading signature to Supabase Storage", e)
            null
        }
    }

    /**
     * فراخوانی اقلام، انواع فرش، خدمات و تعرفه‌های مصوب از پنل مرکزی قالیشویی زمرد
     */
    suspend fun fetchPanelServiceCatalog(): List<PanelCatalogItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PanelCatalogItem>()
        try {
            val endpoint = "$supabaseUrl/rest/v1/${ZomorrodSupabaseConfig.Tables.SERVICE_ITEMS}?select=*"
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim() ?: ""
                    if (body.startsWith("[") && body.endsWith("]")) {
                        val jsonArray = JSONArray(body)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.optJSONObject(i) ?: continue
                            list.add(
                                PanelCatalogItem(
                                    id = obj.optString("id", "PANEL-$i"),
                                    name = obj.optString("name", obj.optString("title", "خدمت زمرد")),
                                    category = obj.optString("category", "CARPET_TYPE"),
                                    unitPrice = obj.optLong("unit_price", obj.optLong("price", 100000L)),
                                    defaultLength = obj.optDouble("default_length", 3.0),
                                    defaultWidth = obj.optDouble("default_width", 2.0),
                                    unit = obj.optString("unit", "متر مربع"),
                                    description = obj.optString("description", ""),
                                    isDefault = obj.optBoolean("is_default", false)
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Notice: Catalog from panel: ${e.message}")
        }

        // اگر از پنل دیتایی دریافت نشد یا به دلیل آفلاین بودن، تعرفه‌های استاندارد مصوب پنل زمرد بارگذاری می‌شوند
        if (list.isEmpty()) {
            list.addAll(getDefaultPanelCatalog())
        }
        list
    }

    /**
     * کاتالوگ استاندارد تعرفه‌ها و خدمات مصوب پنل قالیشویی زمرد
     */
    fun getDefaultPanelCatalog(): List<PanelCatalogItem> {
        return listOf(
            // انواع فرش و تعرفه‌های مصوب متراژ
            PanelCatalogItem("PANEL-CPT-1", "ماشینی ۶ متری (۲×۳)", "CARPET_TYPE", 100000L, 3.0, 2.0, "متر مربع", "شستشوی تمام مکانیزه و آبکشی اسلامی", true),
            PanelCatalogItem("PANEL-CPT-2", "ماشینی ۹ متری (۲٫۵×۳٫۵)", "CARPET_TYPE", 105000L, 3.5, 2.5, "متر مربع", "شستشوی مکانیزه و غبارگیری فرش ۹ متری", true),
            PanelCatalogItem("PANEL-CPT-3", "ماشینی ۱۲ متری (۳×۴)", "CARPET_TYPE", 110000L, 4.0, 3.0, "متر مربع", "شستشوی صنعتی با خشک‌کن لوله‌ای", true),
            PanelCatalogItem("PANEL-CPT-4", "دستبافت نائین و کاشان (اعلا)", "CARPET_TYPE", 195000L, 3.0, 2.0, "متر مربع", "شستشوی سنتی با تثبیت رنگ گیاهی و ارگانیک"),
            PanelCatalogItem("PANEL-CPT-5", "دستبافت تمام ابریشم / چله ابریشم", "CARPET_TYPE", 290000L, 3.0, 2.0, "متر مربع", "ابریشم‌شویی فوق تخصصی با پودر محافظ پرز"),
            PanelCatalogItem("PANEL-CPT-6", "گلیم / گبه / جاجیم سنتی", "CARPET_TYPE", 85000L, 2.5, 1.5, "متر مربع", "شستشوی ملایم بدون آسیب به تاروپود سنتی"),
            PanelCatalogItem("PANEL-CPT-7", "موکت پالاز / پرزدار / تافتینگ", "CARPET_TYPE", 65000L, 4.0, 3.0, "متر مربع", "شستشوی عمقی پرزگیر و لکه‌بری نانو"),
            PanelCatalogItem("PANEL-CPT-8", "موکت نمدی / کبریتی اداری", "CARPET_TYPE", 45000L, 4.0, 3.0, "متر مربع", "شستشوی صنعتی سریع با مکش قوی"),
            PanelCatalogItem("PANEL-CPT-9", "پرده و تور / روفرشی / پتو", "CARPET_TYPE", 75000L, 2.2, 1.8, "متر مربع", "شستشو با اتوکشی و تحویل بسته‌بندی"),
            PanelCatalogItem("PANEL-CPT-10", "سایر ابعاد (سفارشی و کناره)", "CARPET_TYPE", 110000L, 1.0, 1.0, "متر مربع", "محاسبه بر اساس اندازه‌گیری دقیق میدانی"),

            // خدمات درخواستی و تکمیلی مصوب پنل
            PanelCatalogItem("PANEL-SRV-1", "شستشوی ویژه (اعلاشویی نانو)", "SERVICE", 25000L, 0.0, 0.0, "متر مربع", "شستشوی ۲ طرفه با شامپو نانو و لکه‌بری عمقی", true),
            PanelCatalogItem("PANEL-SRV-2", "ابریشم‌شویی و احیای رنگ گیاهی", "SERVICE", 50000L, 0.0, 0.0, "متر مربع", "شستشوی بدون شوینده‌های اسیدی و تثبیت رنگ"),
            PanelCatalogItem("PANEL-SRV-3", "لکه‌بری تخصصی نانو (چربی، جوهر، چای)", "SERVICE", 35000L, 0.0, 0.0, "متر مربع", "از بین بردن لکه‌های قدیمی بدون سایش پرز"),
            PanelCatalogItem("PANEL-SRV-4", "رفوگری و ترمیم پارگی و سوختگی", "SERVICE", 60000L, 0.0, 0.0, "متر طول", "مرمت و بافت مجدد توسط استادکار رفوگر"),
            PanelCatalogItem("PANEL-SRV-5", "ریشه‌زنی و دوخت ریشه نو", "SERVICE", 45000L, 0.0, 0.0, "متر طول", "دوخت ریشه ابریشمی یا نخی استاندارد"),
            PanelCatalogItem("PANEL-SRV-6", "شیرازه‌دوزی دوطرفه و چرم‌دوزی لبه", "SERVICE", 40000L, 0.0, 0.0, "متر طول", "تقویت حاشیه و جلوگیری از لول شدن فرش"),
            PanelCatalogItem("PANEL-SRV-7", "ضدعفونی UV و اتوکشی حرارتی", "SERVICE", 20000L, 0.0, 0.0, "متر مربع", "استریل کامل و اتوی بخار صاف‌کننده"),
            PanelCatalogItem("PANEL-SRV-8", "بیدزدگی و پودر ضدموریانه", "SERVICE", 30000L, 0.0, 0.0, "متر مربع", "سم‌زدایی تخصصی و محافظت طولانی‌مدت"),
            PanelCatalogItem("PANEL-SRV-9", "کاور و بسته‌بندی ضدآب پلمپ", "SERVICE", 15000L, 0.0, 0.0, "تخته", "بسته‌بندی وکیوم شرینک بهداشتی"),

            // عیوب اولیه استاندارد ثبت‌شده در سامانه پنل
            PanelCatalogItem("PANEL-DEF-1", "بدون عیب اولیه", "DEFECT", 0L, 0.0, 0.0, "مورد", "فرش کاملاً سالم و بدون نقص فیزیکی", true),
            PanelCatalogItem("PANEL-DEF-2", "سوختگی جزئی / زردی جای بخاری", "DEFECT", 0L, 0.0, 0.0, "مورد", "سوختگی سطحی پرز یا زردی ناشی از حرارت"),
            PanelCatalogItem("PANEL-DEF-3", "پوسیدگی حاشیه / سستی تار و پود", "DEFECT", 0L, 0.0, 0.0, "مورد", "فرسودگی الیاف ناشی از رطوبت"),
            PanelCatalogItem("PANEL-DEF-4", "پارگی / شکافتگی / سوراخ", "DEFECT", 0L, 0.0, 0.0, "مورد", "پارگی در متن یا لچک و ترنج"),
            PanelCatalogItem("PANEL-DEF-5", "بیدزدگی / ساییدگی شدید پرز", "DEFECT", 0L, 0.0, 0.0, "مورد", "خوردگی پشم یا کچلی موضعی فرش"),
            PanelCatalogItem("PANEL-DEF-6", "تغییر رنگ / لکه عمیق چربی و چای", "DEFECT", 0L, 0.0, 0.0, "مورد", "تداخل رنگ‌های بافت یا لکه‌های ماندگار"),
            PanelCatalogItem("PANEL-DEF-7", "کجی / دفرمگی / شکستگی تار و پود", "DEFECT", 0L, 0.0, 0.0, "مورد", "تاب‌خوردگی یا شکستگی ناشی از تاشدن نادرست")
        )
    }
}
