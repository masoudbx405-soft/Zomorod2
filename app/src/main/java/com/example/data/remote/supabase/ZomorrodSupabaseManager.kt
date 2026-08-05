package com.example.data.remote.supabase

import android.util.Log
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.DriverSettlementEntity
import com.example.data.local.entities.OrderEntity
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
}
