package com.example.dubaothoitiet.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database cho notification system
 * Version 1: Initial database với 3 entities
 * Version 2: Thêm notificationsEnabled field vào NotificationPreferencesEntity
 */
@Database(
    entities = [
        NotificationPreferencesEntity::class,
        LocationNotificationPreferencesEntity::class,
        NotificationRecordEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NotificationDatabase : RoomDatabase() {
    
    abstract fun notificationPreferencesDao(): NotificationPreferencesDao
    abstract fun locationNotificationPreferencesDao(): LocationNotificationPreferencesDao
    abstract fun notificationRecordDao(): NotificationRecordDao
    
    companion object {
        @Volatile
        private var INSTANCE: NotificationDatabase? = null
        
        private const val DATABASE_NAME = "notification_database"
        
        fun getDatabase(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations() // Thêm migrations khi cần
                    .fallbackToDestructiveMigration() // Chỉ dùng trong development
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Migration từ version 1 sang 2 (ví dụ cho tương lai)
         * Uncomment và sử dụng khi cần migrate database
         */
        /*
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Thêm column mới hoặc thay đổi schema
                // database.execSQL("ALTER TABLE notification_preferences ADD COLUMN new_column TEXT")
            }
        }
        */
    }
}
