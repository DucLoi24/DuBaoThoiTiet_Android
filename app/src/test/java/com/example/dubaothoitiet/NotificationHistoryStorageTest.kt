package com.example.dubaothoitiet

import com.example.dubaothoitiet.data.NotificationPriority
import com.example.dubaothoitiet.data.NotificationRecord
import com.example.dubaothoitiet.data.NotificationType
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests cho notification history storage
 * 
 * Validates: Requirements 17.5
 */
class NotificationHistoryStorageTest {

    @Test
    fun `notification record should contain all required fields`() {
        // Arrange
        val userId = 1
        val locationId = 123
        val title = "Cảnh báo mưa lớn"
        val body = "Mưa lớn dự kiến trong 2 giờ tới"
        val priority = NotificationPriority.HIGH
        val notificationType = NotificationType.ALERT
        val receivedAt = System.currentTimeMillis()
        val data = mapOf(
            "alert_type" to "heavy_rain",
            "severity" to "high"
        )

        // Act
        val record = NotificationRecord(
            userId = userId,
            locationId = locationId,
            notificationType = notificationType,
            title = title,
            body = body,
            priority = priority,
            receivedAt = receivedAt,
            data = data,
            read = false
        )

        // Assert
        assertEquals(userId, record.userId)
        assertEquals(locationId, record.locationId)
        assertEquals(notificationType, record.notificationType)
        assertEquals(title, record.title)
        assertEquals(body, record.body)
        assertEquals(priority, record.priority)
        assertEquals(receivedAt, record.receivedAt)
        assertEquals(data, record.data)
        assertFalse(record.read)
    }

    @Test
    fun `notification record should convert to entity correctly`() {
        // Arrange
        val record = NotificationRecord(
            userId = 1,
            locationId = 123,
            notificationType = NotificationType.ALERT,
            title = "Test Alert",
            body = "Test Body",
            priority = NotificationPriority.HIGH,
            receivedAt = System.currentTimeMillis(),
            data = mapOf("key" to "value"),
            read = false
        )

        // Act
        val entity = record.toEntity()

        // Assert
        assertEquals(record.userId, entity.userId)
        assertEquals(record.locationId, entity.locationId)
        assertEquals(record.notificationType.apiValue, entity.notificationType)
        assertEquals(record.title, entity.title)
        assertEquals(record.body, entity.body)
        assertEquals(record.priority.toApiString(), entity.priority)
        assertEquals(record.receivedAt, entity.receivedAt)
        assertEquals(record.data, entity.data)
        assertEquals(record.read, entity.read)
    }

    @Test
    fun `notification record should handle empty data map`() {
        // Arrange & Act
        val record = NotificationRecord(
            userId = 1,
            locationId = null,
            notificationType = NotificationType.MORNING_SUMMARY,
            title = "Morning Summary",
            body = "Today's weather",
            priority = NotificationPriority.MEDIUM,
            receivedAt = System.currentTimeMillis(),
            data = emptyMap(),
            read = false
        )

        // Assert
        assertNotNull(record.data)
        assertTrue(record.data.isEmpty())
    }

    @Test
    fun `notification record should handle null location id`() {
        // Arrange & Act
        val record = NotificationRecord(
            userId = 1,
            locationId = null,
            notificationType = NotificationType.WEEKLY_SUMMARY,
            title = "Weekly Summary",
            body = "This week's weather",
            priority = NotificationPriority.LOW,
            receivedAt = System.currentTimeMillis(),
            data = emptyMap(),
            read = false
        )

        // Assert
        assertNull(record.locationId)
    }

    @Test
    fun `notification types should have correct api values`() {
        // Assert
        assertEquals("alert", NotificationType.ALERT.apiValue)
        assertEquals("morning_summary", NotificationType.MORNING_SUMMARY.apiValue)
        assertEquals("tomorrow_forecast", NotificationType.TOMORROW_FORECAST.apiValue)
        assertEquals("weekly_summary", NotificationType.WEEKLY_SUMMARY.apiValue)
    }

    @Test
    fun `notification priorities should convert to api strings correctly`() {
        // Assert
        assertEquals("high", NotificationPriority.HIGH.toApiString())
        assertEquals("medium", NotificationPriority.MEDIUM.toApiString())
        assertEquals("low", NotificationPriority.LOW.toApiString())
    }

    @Test
    fun `notification record should preserve data integrity through conversion`() {
        // Arrange
        val originalData = mapOf(
            "alert_type" to "storm",
            "severity" to "high",
            "location_name" to "Hà Nội",
            "wind_speed" to "75"
        )
        
        val record = NotificationRecord(
            userId = 1,
            locationId = 456,
            notificationType = NotificationType.ALERT,
            title = "Cảnh báo bão",
            body = "Bão mạnh cấp 10",
            priority = NotificationPriority.HIGH,
            receivedAt = System.currentTimeMillis(),
            data = originalData,
            read = false
        )

        // Act
        val entity = record.toEntity()

        // Assert - Data should be preserved
        assertEquals(originalData.size, entity.data.size)
        originalData.forEach { (key, value) ->
            assertEquals(value, entity.data[key])
        }
    }
}
