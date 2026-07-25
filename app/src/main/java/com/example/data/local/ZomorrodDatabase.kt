package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.GpsLogDao
import com.example.data.local.dao.OrderDao
import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.GpsLogEntity
import com.example.data.local.entities.OrderEntity

@Database(
    entities = [
        OrderEntity::class,
        CarpetItemEntity::class,
        ChatMessageEntity::class,
        GpsLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ZomorrodDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun gpsLogDao(): GpsLogDao

    companion object {
        @Volatile
        private var INSTANCE: ZomorrodDatabase? = null

        fun getDatabase(context: Context): ZomorrodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZomorrodDatabase::class.java,
                    "zomorrod_driver_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
