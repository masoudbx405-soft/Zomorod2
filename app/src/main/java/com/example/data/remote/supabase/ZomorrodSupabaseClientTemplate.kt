package com.example.data.remote.supabase

/**
 * راهنمای کامل و الگوی پیاده‌سازی رسمی با کتابخانه supabase-kt در کاتلین
 *
 * پیش‌نیازهای Gradle در libs.versions.toml:
 * -------------------------------------------------------------
 * [versions]
 * supabase = "3.1.4"
 * ktor = "3.1.1"
 *
 * [libraries]
 * supabase-postgrest = { module = "io.github.jan-tennert.supabase:postgrest-kt", version.ref = "supabase" }
 * supabase-realtime = { module = "io.github.jan-tennert.supabase:realtime-kt", version.ref = "supabase" }
 * supabase-auth = { module = "io.github.jan-tennert.supabase:auth-kt", version.ref = "supabase" }
 * supabase-storage = { module = "io.github.jan-tennert.supabase:storage-kt", version.ref = "supabase" }
 * ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
 * -------------------------------------------------------------
 *
 * نمونه کد راه‌اندازی کلاینت رسمی با supabase-kt:
 *
 * ```kotlin
 * import io.github.jan.supabase.createSupabaseClient
 * import io.github.jan.supabase.postgrest.Postgrest
 * import io.github.jan.supabase.postgrest.postgrest
 * import io.github.jan.supabase.realtime.Realtime
 * import io.github.jan.supabase.realtime.realtime
 * import io.github.jan.supabase.realtime.channel
 * import io.github.jan.supabase.realtime.postgresChangeFlow
 * import io.github.jan.supabase.realtime.PostgresAction
 * import io.github.jan.supabase.auth.Auth
 * import io.github.jan.supabase.storage.Storage
 *
 * object ZomorrodSupabaseClient {
 *     val client = createSupabaseClient(
 *         supabaseUrl = ZomorrodSupabaseConfig.DEFAULT_SUPABASE_URL,
 *         supabaseKey = ZomorrodSupabaseConfig.DEFAULT_ANON_KEY
 *     ) {
 *         install(Postgrest)
 *         install(Realtime)
 *         install(Auth)
 *         install(Storage)
 *     }
 *
 *     // دریافت سفارشات راننده
 *     suspend fun getDriverOrders(driverId: String): List<SupabaseOrderDto> {
 *         return client.postgrest[ZomorrodSupabaseConfig.Tables.ORDERS]
 *             .select {
 *                 filter {
 *                     eq("driver_id", driverId)
 *                 }
 *             }
 *             .decodeList<SupabaseOrderDto>()
 *     }
 *
 *     // اشتراک در رویدادهای زنده تغییر وضعیت سفارشات با Realtime
 *     fun listenToOrderChanges() = client.realtime.channel(ZomorrodSupabaseConfig.Channels.ORDERS_CHANNEL)
 *         .postgresChangeFlow<PostgresAction>(schema = "public") {
 *             table = ZomorrodSupabaseConfig.Tables.ORDERS
 *         }
 * }
 * ```
 */
object ZomorrodSupabaseDocs {
    const val DOCUMENTATION_TITLE = "راهنمای همگام‌سازی Supabase برای قالیشویی زمرد"
}
