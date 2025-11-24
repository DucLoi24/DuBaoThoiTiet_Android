package com.example.dubaothoitiet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dubaothoitiet.data.database.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Test cho NotificationDatabase
 * Verify rằng database và các DAOs hoạt động đúng
 */
@RunWith(AndroidJUnit4::class)
class NotificationDatabaseTest {
    
    private lateinit var database: NotificationDatabase
    private lateinit var preferencesDao: NotificationPreferencesDao
    private lateinit var locationPreferencesDao: LocationNotificationPreferencesDao
    private lateinit var recordDao: NotificationRecordDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Sử dụng in-memory database cho testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            NotificationDatabase::class.java
        ).build()
        
        preferencesDao = database.notificationPreferencesDao()
        locationPreferencesDao = database.locationNotificationPreferencesDao()
        recordDao = database.notificationRecordDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun testInsertAndGetPreferences() = runBlocking {
        // Tạo preferences
        val preferences = NotificationPreferencesEntity(
            userId = 1,
            enabledEventTypes = listOf("heavy_rain", "storm"),
            notificationSchedule = "24_7",
            morningSummaryEnabled = true,
            tomorrowForecastEnabled = true,
            weeklySummaryEnabled = false
        )
        
        // Insert
        preferencesDao.insertPreferences(preferences)
        
        // Get và verify
        val retrieved = preferencesDao.getPreferences(1)
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.userId)
        assertEquals(2, retrieved?.enabledEventTypes?.size)
        assertTrue(retrieved?.enabledEventTypes?.contains("heavy_rain") == true)
        assertEquals("24_7", retrieved?.notificationSchedule)
    }
    
    @Test
    fun testInsertAndGetLocationPreferences() = runBlocking {
        // Tạo location preferences
        val locationPref = LocationNotificationPreferencesEntity(
            userId = 1,
            locationId = 100,
            notificationsEnabled = true
        )
        
        // Insert
        locationPreferencesDao.insertLocationPreference(locationPref)
        
        // Get và verify
        val retrieved = locationPreferencesDao.getLocationPreference(1, 100)
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.userId)
        assertEquals(100, retrieved?.locationId)
        assertTrue(retrieved?.notificationsEnabled == true)
    }
    
    @Test
    fun testInsertAndGetNotificationRecord() = runBlocking {
        // Tạo notification record
        val record = NotificationRecordEntity(
            userId = 1,
            locationId = 100,
            notificationType = "alert",
            title = "Cảnh báo mưa lớn",
            body = "Mưa lớn dự kiến trong 2 giờ tới",
            priority = "high",
            receivedAt = System.currentTimeMillis(),
            data = mapOf("severity" to "high", "duration" to "2h")
        )
        
        // Insert
        val id = recordDao.insertRecord(record)
        assertTrue(id > 0)
        
        // Get và verify
        val retrieved = recordDao.getRecordById(id)
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.userId)
        assertEquals("alert", retrieved?.notificationType)
        assertEquals("Cảnh báo mưa lớn", retrieved?.title)
        assertEquals("high", retrieved?.priority)
        assertEquals(2, retrieved?.data?.size)
    }
    
    @Test
    fun testGetAllRecordsSortedByTime() = runBlocking {
        // Insert multiple records
        val record1 = NotificationRecordEntity(
            userId = 1,
            locationId = 100,
            notificationType = "alert",
            title = "Record 1",
            body = "Body 1",
            priority = "high",
            receivedAt = 1000L
        )
        
        val record2 = NotificationRecordEntity(
            userId = 1,
            locationId = 100,
            notificationType = "summary",
            title = "Record 2",
            body = "Body 2",
            priority = "medium",
            receivedAt = 2000L
        )
        
        recordDao.insertRecord(record1)
        recordDao.insertRecord(record2)
        
        // Get all records
        val records = recordDao.getAllRecords(1)
        
        // Verify sorting (newest first)
        assertEquals(2, records.size)
        assertEquals("Record 2", records[0].title) // Newer record first
        assertEquals("Record 1", records[1].title)
    }
    
    @Test
    fun testDeleteLocationPreference() = runBlocking {
        // Insert
        val locationPref = LocationNotificationPreferencesEntity(
            userId = 1,
            locationId = 100,
            notificationsEnabled = true
        )
        locationPreferencesDao.insertLocationPreference(locationPref)
        
        // Verify exists
        var retrieved = locationPreferencesDao.getLocationPreference(1, 100)
        assertNotNull(retrieved)
        
        // Delete
        locationPreferencesDao.deleteLocationPreference(1, 100)
        
        // Verify deleted
        retrieved = locationPreferencesDao.getLocationPreference(1, 100)
        assertNull(retrieved)
    }
    
    @Test
    fun testUnreadCount() = runBlocking {
        // Insert records with different read status
        val record1 = NotificationRecordEntity(
            userId = 1,
            locationId = 100,
            notificationType = "alert",
            title = "Record 1",
            body = "Body 1",
            priority = "high",
            receivedAt = 1000L,
            read = false
        )
        
        val record2 = NotificationRecordEntity(
            userId = 1,
            locationId = 100,
            notificationType = "alert",
            title = "Record 2",
            body = "Body 2",
            priority = "high",
            receivedAt = 2000L,
            read = true
        )
        
        recordDao.insertRecord(record1)
        recordDao.insertRecord(record2)
        
        // Check unread count
        val unreadCount = recordDao.getUnreadCount(1)
        assertEquals(1, unreadCount)
    }
}
