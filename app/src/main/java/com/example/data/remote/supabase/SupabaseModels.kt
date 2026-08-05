package com.example.data.remote.supabase

import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.DriverSettlementEntity
import com.example.data.local.entities.OrderEntity

/**
 * مدل داده‌ای سفیران / رانندگان (جدول drivers در Supabase)
 */
data class SupabaseDriverDto(
    val id: String,
    val name: String,
    val phone: String,
    val vehicle_type: String = "وانت پیکان",
    val vehicle_plate: String = "ایران ۱۱ - ۱۲۳ ج ۴۵",
    val status: String = "ACTIVE",
    val current_lat: Double = 35.779,
    val current_lng: Double = 51.405,
    val battery_level: Int = 100,
    val speed: Double = 0.0,
    val app_status: String = "active",
    val total_collected_cash: Long = 0L,
    val total_collected_pos: Long = 0L,
    val last_online_at: String? = null
)

/**
 * مدل داده‌ای سفارشات (جدول orders در Supabase)
 */
data class SupabaseOrderDto(
    val id: String,
    val tracking_code: String,
    val customer_name: String,
    val customer_phone: String,
    val customer_address: String,
    val lat: Double,
    val lng: Double,
    val stage: String,
    val status: String,
    val order_type: String,
    val driver_id: String,
    val driver_name: String,
    val total_amount: Long,
    val discount_amount: Long,
    val final_payable: Long,
    val paid_amount: Long,
    val payment_method: String,
    val payment_status: String,
    val rack_code: String,
    val clean_rack_code: String,
    val return_reason: String,
    val customer_signature_url: String,
    val notes: String,
    val updated_at: String? = null
)

/**
 * مدل داده‌ای تسویه حساب روزانه راننده با دفتر (جدول driver_settlements در Supabase)
 */
data class SupabaseDriverSettlementDto(
    val id: String,
    val driver_id: String,
    val driver_name: String,
    val date: String,
    val total_cash: Long,
    val total_pos: Long,
    val total_card_to_card: Long,
    val total_online: Long,
    val total_amount: Long,
    val orders_count: Int,
    val returned_orders_count: Int,
    val status: String,
    val notes: String
)

/**
 * مدل داده‌ای پیام‌های چت پشتیبانی و دیسپچر (جدول chat_messages در Supabase)
 */
data class SupabaseChatMessageDto(
    val id: String,
    val driver_id: String,
    val sender: String,
    val sender_name: String,
    val text: String,
    val timestamp: String
)

/**
 * توابع تبدیل و مپینگ بین موجودیت‌های محلی Room و مدل‌های سرور Supabase
 */
fun DriverEntity.toSupabaseDto(): SupabaseDriverDto {
    return SupabaseDriverDto(
        id = this.id,
        name = this.name,
        phone = this.phone,
        vehicle_type = this.vehicleType,
        vehicle_plate = this.vehiclePlate,
        status = this.status,
        current_lat = this.currentLat,
        current_lng = this.currentLng,
        battery_level = this.batteryLevel,
        speed = this.speed.toDouble(),
        app_status = "active",
        total_collected_cash = this.totalCollectedCash,
        total_collected_pos = this.totalCollectedPos
    )
}

fun OrderEntity.toSupabaseDto(): SupabaseOrderDto {
    val payable = (this.totalAmount - this.discountAmount).coerceAtLeast(0L)
    val payStatus = if (this.paidAmount >= payable && payable > 0) "paid"
    else if (this.paidAmount > 0) "deposit"
    else "unpaid"

    return SupabaseOrderDto(
        id = this.id,
        tracking_code = if (this.trackingCode.isNotBlank()) this.trackingCode else this.id,
        customer_name = this.customerName,
        customer_phone = this.customerPhone,
        customer_address = this.address,
        lat = this.latitude,
        lng = this.longitude,
        stage = this.stage,
        status = this.status,
        order_type = this.orderType,
        driver_id = this.driverId,
        driver_name = this.driverName,
        total_amount = this.totalAmount,
        discount_amount = this.discountAmount,
        final_payable = payable,
        paid_amount = this.paidAmount,
        payment_method = this.paymentMethod,
        payment_status = payStatus,
        rack_code = this.rackCode,
        clean_rack_code = this.cleanRackCode,
        return_reason = this.returnReason,
        customer_signature_url = this.customerSignatureUrl,
        notes = this.notes
    )
}

fun DriverSettlementEntity.toSupabaseDto(): SupabaseDriverSettlementDto {
    return SupabaseDriverSettlementDto(
        id = this.id,
        driver_id = this.driverId,
        driver_name = this.driverName,
        date = this.date,
        total_cash = this.totalCash,
        total_pos = this.totalPos,
        total_card_to_card = this.totalCardToCard,
        total_online = this.totalOnline,
        total_amount = this.totalAmount,
        orders_count = this.ordersCount,
        returned_orders_count = this.returnedOrdersCount,
        status = this.status,
        notes = this.notes
    )
}

fun ChatMessageEntity.toSupabaseDto(): SupabaseChatMessageDto {
    return SupabaseChatMessageDto(
        id = "MSG-${this.id}-${this.timestamp}",
        driver_id = this.orderId,
        sender = if (this.sender == "DRIVER") "driver" else "operator",
        sender_name = this.senderName,
        text = this.messageText,
        timestamp = this.timestamp.toString()
    )
}
