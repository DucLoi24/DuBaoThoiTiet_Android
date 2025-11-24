package com.example.dubaothoitiet

import com.example.dubaothoitiet.data.*
import com.example.dubaothoitiet.data.database.NotificationPreferencesEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests cho notification models và converters
 */
class NotificationModelsTest {

    @Test
    fun `test NotificationPriority enum conversions`() {
        // Test fromString
        assertEquals(NotificationPriority.HIGH, NotificationPriority.fromString("high"))
        assertEquals(NotificationPriority.MEDIUM, NotificationPriority.fromString("medium"))
        assertEquals(NotificationPriority.LOW, NotificationPriority.fromString("low"))
        assertEquals(NotificationPriority.MEDIUM, NotificationPriority.fromString("invalid"))

        // Test toApiString
        assertEquals("high", NotificationPriority.HIGH.toApiString())
        assertEquals("medium", NotificationPriority.MEDIUM.toApiString())
        assertEquals("low", NotificationPriority.LOW.toApiString())
    }

    @Test
    fun `test NotificationSchedule enum conversions`() {
        // Test fromString
        assertEquals(NotificationSchedule.DAYTIME_ONLY, NotificationSchedule.fromString("daytime_only"))
        assertEquals(NotificationSchedule.FULL_24_7, NotificationSchedule.fromString("24_7"))
        assertEquals(NotificationSchedule.FULL_24_7, NotificationSchedule.fromString("invalid"))

        // Test toApiString
        assertEquals("daytime_only", NotificationSchedule.DAYTIME_ONLY.toApiString())
        assertEquals("24_7", NotificationSchedule.FULL_24_7.toApiString())

        // Test getDisplayName
        assertEquals("Chỉ ban ngày (6:00-22:00)", NotificationSchedule.DAYTIME_ONLY.getDisplayName())
        assertEquals("24/7", NotificationSchedule.FULL_24_7.getDisplayName())
    }

    @Test
    fun `test WeatherEventType enum conversions`() {
        // Test fromApiValue
        assertEquals(WeatherEventType.HEAVY_RAIN, WeatherEventType.fromApiValue("heavy_rain"))
        assertEquals(WeatherEventType.STORM, WeatherEventType.fromApiValue("storm"))
        assertEquals(WeatherEventType.EXTREME_HEAT, WeatherEventType.fromApiValue("extreme_heat"))
        assertNull(WeatherEventType.fromApiValue("invalid"))

        // Test getAllApiValues
        val allValues = WeatherEventType.getAllApiValues()
        assertTrue(allValues.contains("heavy_rain"))
        assertTrue(allValues.contains("storm"))
        assertEquals(6, allValues.size)
    }

    @Test
    fun `test NotificationType enum conversions`() {
        // Test fromApiValue
        assertEquals(NotificationType.ALERT, NotificationType.fromApiValue("alert"))
        assertEquals(NotificationType.MORNING_SUMMARY, NotificationType.fromApiValue("morning_summary"))
        assertNull(NotificationType.fromApiValue("invalid"))
    }

    @Test
    fun `test NotificationPreferences default creation`() {
        val preferences = NotificationPreferences.default(userId = 123)

        assertEquals(123, preferences.userId)
        assertEquals(NotificationSchedule.FULL_24_7, preferences.notificationSchedule)
        assertTrue(preferences.morningSummaryEnabled)
        assertTrue(preferences.tomorrowForecastEnabled)
        assertFalse(preferences.weeklySummaryEnabled)
        assertEquals("Asia/Ho_Chi_Minh", preferences.timezone)
        assertEquals(6, preferences.enabledEventTypes.size)
    }

    @Test
    fun `test NotificationPreferences to Entity conversion`() {
        val preferences = NotificationPreferences(
            userId = 123,
            enabledEventTypes = listOf(WeatherEventType.HEAVY_RAIN, WeatherEventType.STORM),
            notificationSchedule = NotificationSchedule.DAYTIME_ONLY,
            morningSummaryEnabled = true,
            tomorrowForecastEnabled = false,
            weeklySummaryEnabled = true,
            timezone = "Asia/Ho_Chi_Minh",
            lastSyncedAt = 1000L
        )

        val entity = preferences.toEntity()

        assertEquals(123, entity.userId)
        assertEquals(2, entity.enabledEventTypes.size)
        assertTrue(entity.enabledEventTypes.contains("heavy_rain"))
        assertTrue(entity.enabledEventTypes.contains("storm"))
        assertEquals("daytime_only", entity.notificationSchedule)
        assertTrue(entity.morningSummaryEnabled)
        assertFalse(entity.tomorrowForecastEnabled)
        assertTrue(entity.weeklySummaryEnabled)
        assertEquals(1000L, entity.lastSyncedAt)
    }

    @Test
    fun `test NotificationPreferences from Entity conversion`() {
        val entity = NotificationPreferencesEntity(
            userId = 456,
            enabledEventTypes = listOf("heavy_rain", "extreme_heat"),
            notificationSchedule = "24_7",
            morningSummaryEnabled = false,
            tomorrowForecastEnabled = true,
            weeklySummaryEnabled = false,
            timezone = "Asia/Ho_Chi_Minh",
            lastSyncedAt = 2000L
        )

        val preferences = NotificationPreferences.fromEntity(entity)

        assertEquals(456, preferences.userId)
        assertEquals(2, preferences.enabledEventTypes.size)
        assertTrue(preferences.enabledEventTypes.contains(WeatherEventType.HEAVY_RAIN))
        assertTrue(preferences.enabledEventTypes.contains(WeatherEventType.EXTREME_HEAT))
        assertEquals(NotificationSchedule.FULL_24_7, preferences.notificationSchedule)
        assertFalse(preferences.morningSummaryEnabled)
        assertTrue(preferences.tomorrowForecastEnabled)
        assertEquals(2000L, preferences.lastSyncedAt)
    }

    @Test
    fun `test NotificationPreferences to DTO conversion`() {
        val preferences = NotificationPreferences(
            userId = 789,
            enabledEventTypes = listOf(WeatherEventType.STORM),
            notificationSchedule = NotificationSchedule.DAYTIME_ONLY,
            morningSummaryEnabled = true,
            tomorrowForecastEnabled = true,
            weeklySummaryEnabled = false,
            timezone = "Asia/Ho_Chi_Minh"
        )

        val dto = preferences.toDTO()

        assertEquals(1, dto.enabledEventTypes.size)
        assertEquals("storm", dto.enabledEventTypes[0])
        assertEquals("daytime_only", dto.notificationSchedule)
        assertTrue(dto.morningSummaryEnabled)
        assertTrue(dto.tomorrowForecastEnabled)
        assertFalse(dto.weeklySummaryEnabled)
    }

    @Test
    fun `test NotificationPreferences round trip Entity conversion`() {
        val original = NotificationPreferences.default(userId = 999)
        val entity = original.toEntity()
        val converted = NotificationPreferences.fromEntity(entity)

        assertEquals(original.userId, converted.userId)
        assertEquals(original.enabledEventTypes.size, converted.enabledEventTypes.size)
        assertEquals(original.notificationSchedule, converted.notificationSchedule)
        assertEquals(original.morningSummaryEnabled, converted.morningSummaryEnabled)
        assertEquals(original.tomorrowForecastEnabled, converted.tomorrowForecastEnabled)
        assertEquals(original.weeklySummaryEnabled, converted.weeklySummaryEnabled)
        assertEquals(original.timezone, converted.timezone)
    }

    @Test
    fun `test LocationNotificationPreferences conversions`() {
        val locationPref = LocationNotificationPreferences(
            userId = 123,
            locationId = 456,
            notificationsEnabled = true,
            lastSyncedAt = 1000L
        )

        // Test toEntity
        val entity = locationPref.toEntity()
        assertEquals(123, entity.userId)
        assertEquals(456, entity.locationId)
        assertTrue(entity.notificationsEnabled)
        assertEquals(1000L, entity.lastSyncedAt)

        // Test fromEntity
        val converted = LocationNotificationPreferences.fromEntity(entity)
        assertEquals(locationPref.userId, converted.userId)
        assertEquals(locationPref.locationId, converted.locationId)
        assertEquals(locationPref.notificationsEnabled, converted.notificationsEnabled)
        assertEquals(locationPref.lastSyncedAt, converted.lastSyncedAt)

        // Test toDTO
        val dto = locationPref.toDTO()
        assertEquals(456, dto.locationId)
        assertTrue(dto.notificationsEnabled)
    }

    @Test
    fun `test NotificationRecord conversions`() {
        val record = NotificationRecord(
            id = 1L,
            userId = 123,
            locationId = 456,
            notificationType = NotificationType.ALERT,
            title = "Test Alert",
            body = "Test Body",
            priority = NotificationPriority.HIGH,
            receivedAt = 1000L,
            data = mapOf("key" to "value"),
            read = false
        )

        // Test toEntity
        val entity = record.toEntity()
        assertEquals(1L, entity.id)
        assertEquals(123, entity.userId)
        assertEquals(456, entity.locationId)
        assertEquals("alert", entity.notificationType)
        assertEquals("Test Alert", entity.title)
        assertEquals("high", entity.priority)
        assertFalse(entity.read)

        // Test fromEntity
        val converted = NotificationRecord.fromEntity(entity)
        assertEquals(record.id, converted.id)
        assertEquals(record.userId, converted.userId)
        assertEquals(record.notificationType, converted.notificationType)
        assertEquals(record.priority, converted.priority)

        // Test toHistoryDTO
        val dto = record.toHistoryDTO("Location Name")
        assertEquals(1L, dto.id)
        assertEquals("alert", dto.notificationType)
        assertEquals("Location Name", dto.locationName)
        assertFalse(dto.read)
    }

    @Test
    fun `test ApiErrorResponse getErrorMessage`() {
        val error1 = ApiErrorResponse(error = "Error message", detail = null, message = null)
        assertEquals("Error message", error1.getErrorMessage())

        val error2 = ApiErrorResponse(error = null, detail = "Detail message", message = null)
        assertEquals("Detail message", error2.getErrorMessage())

        val error3 = ApiErrorResponse(error = null, detail = null, message = "Message")
        assertEquals("Message", error3.getErrorMessage())

        val error4 = ApiErrorResponse(error = null, detail = null, message = null)
        assertEquals("Đã xảy ra lỗi không xác định", error4.getErrorMessage())
    }
}
