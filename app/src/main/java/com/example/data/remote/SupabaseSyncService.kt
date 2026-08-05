package com.example.data.remote

import android.util.Log
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.DriverSettlementEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.remote.supabase.ZomorrodSupabaseConfig
import com.example.data.remote.supabase.ZomorrodSupabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * سرویس همگام‌سازی و ارتباط با Supabase در سامانه قالیشویی زمرد (panel.yaselectrical.ir)
 */
class SupabaseSyncService(
    private var baseUrl: String = ZomorrodSupabaseConfig.DEFAULT_SUPABASE_URL,
    private var apiKey: String = ZomorrodSupabaseConfig.DEFAULT_ANON_KEY
) {
    val supabaseManager = ZomorrodSupabaseManager(baseUrl, apiKey)

    fun updateConfig(url: String, key: String = apiKey) {
        this.baseUrl = url.trim().removeSuffix("/")
        this.apiKey = key.trim()
        supabaseManager.updateCredentials(this.baseUrl, this.apiKey)
    }

    fun getBaseUrl(): String = baseUrl

    suspend fun testConnection(targetUrl: String = baseUrl): Pair<Boolean, String> {
        supabaseManager.updateCredentials(targetUrl, apiKey)
        return supabaseManager.checkHealth()
    }

    suspend fun syncTelemetry(driver: DriverEntity): Boolean {
        return supabaseManager.syncDriverStatus(driver)
    }

    suspend fun pushOrderUpdate(order: OrderEntity): Boolean {
        return supabaseManager.upsertOrder(order)
    }

    suspend fun pushDriverSettlement(settlement: DriverSettlementEntity): Boolean {
        return supabaseManager.upsertDriverSettlement(settlement)
    }

    suspend fun sendChatMessage(msg: ChatMessageEntity): Boolean {
        return supabaseManager.sendChatMessage(msg)
    }

    suspend fun uploadCustomerSignature(orderId: String, signatureBase64: String): String? {
        return supabaseManager.uploadSignature(orderId, signatureBase64)
    }
}
