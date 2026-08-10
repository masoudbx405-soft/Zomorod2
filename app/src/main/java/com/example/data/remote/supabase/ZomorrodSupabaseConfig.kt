package com.example.data.remote.supabase

/**
 * پیکربندی و تنظیمات ارتباط با پروژه Supabase قالیشویی زمرد
 * سامانه متمرکز: https://panel.yaselectrical.ir
 */
object ZomorrodSupabaseConfig {
    // آدرس سرور متمرکز قالیشویی زمرد
    const val DEFAULT_SUPABASE_URL = "https://panel.yaselectrical.ir"

    // کلید دسترسی عمومی (Anon Key) پروژه
    const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpvbW9ycm9kLXBhbmVsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MDk4NTYwMDAsImV4cCI6MjAyNTQzMjAwMH0.anon_key_zomorrod_secure"

    // نام جداول دیتابیس در Supabase
    object Tables {
        const val DRIVERS = "drivers"
        const val ORDERS = "orders"
        const val CARPET_ITEMS = "carpet_items"
        const val DRIVER_SETTLEMENTS = "driver_settlements"
        const val CHAT_MESSAGES = "chat_messages"
        const val GPS_LOGS = "driver_gps_logs"
        const val SERVICE_ITEMS = "service_items"
        const val PRICING_CATALOG = "pricing_catalog"
    }

    // نام باکت‌های ذخیره‌سازی Supabase Storage
    object Buckets {
        const val SIGNATURES = "signatures"
        const val RECEIPTS = "receipts"
        const val DEFECT_PHOTOS = "carpet-defects"
    }

    // کانال‌های دریافت برخط Realtime
    object Channels {
        const val ORDERS_CHANNEL = "zomorrod-orders-realtime"
        const val CHAT_CHANNEL = "zomorrod-chat-realtime"
        const val DRIVER_TRACKING_CHANNEL = "zomorrod-driver-tracking"
    }
}
