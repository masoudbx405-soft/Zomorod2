package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object ZomorrodNotificationManager {

    private const val CHANNEL_ID = "zomorrod_orders_channel"
    private const val CHANNEL_NAME = "اعلان‌های سفارشات قالیشویی زمرد"
    private const val CHANNEL_DESC = "اعلان‌های اختصاص سفارش جدید و تغییر وضعیت سفارش در سرور"

    private var notificationIdCounter = 1000

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    enableLights(true)
                }
                val notificationManager: NotificationManager? =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendNewOrderNotification(
        context: Context,
        orderId: String,
        customerName: String,
        address: String
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_order_id", orderId)
            }

            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                orderId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "📌 اختصاص سفارش جدید #$orderId"
            val message = "مشتری: $customerName\nآدرس: $address"

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText("سفارش جدید #$orderId برای راننده اختصاص یافت: $customerName")
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(notificationIdCounter++, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendOrderStatusChangeNotification(
        context: Context,
        orderId: String,
        customerName: String,
        newStatusTitle: String
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_order_id", orderId)
            }

            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                orderId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "🔔 تغییر وضعیت سفارش #$orderId"
            val message = "وضعیت جدید سفارش $customerName در سرور به «$newStatusTitle» تغییر یافت."

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(notificationIdCounter++, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendTestNotification(context: Context) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("✅ تست سیستم اعلان‌های قالیشویی زمرد")
                .setContentText("اتصال سیستم اعلان محلی راننده برقرار است و پیام‌ها دریافت می‌شوند.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(999, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
