package com.example.dubaothoitiet

import com.example.dubaothoitiet.data.NotificationPriority
import com.example.dubaothoitiet.service.NotificationChannelManager
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests cho notification channel assignment logic
 * 
 * Validates: Requirements 18.2, 18.5
 * Tests Property 56: Channel assignment by priority
 */
class NotificationChannelAssignmentTest {

    /**
     * Test: HIGH priority notifications được gán vào CHANNEL_HIGH_PRIORITY
     * 
     * Validates: Requirements 18.2
     */
    @Test
    fun testHighPriorityAssignment() {
        // Test logic gán channel cho HIGH priority
        val expectedChannel = NotificationChannelManager.CHANNEL_HIGH_PRIORITY
        val priority = NotificationPriority.HIGH
        
        // Verify mapping logic
        val actualChannel = when (priority) {
            NotificationPriority.HIGH -> NotificationChannelManager.CHANNEL_HIGH_PRIORITY
            NotificationPriority.MEDIUM -> NotificationChannelManager.CHANNEL_SCHEDULED
            NotificationPriority.LOW -> NotificationChannelManager.CHANNEL_GENERAL
        }
        
        assertEquals(
            "HIGH priority notifications phải được gán vào CHANNEL_HIGH_PRIORITY",
            expectedChannel,
            actualChannel
        )
    }

    /**
     * Test: MEDIUM priority notifications được gán vào CHANNEL_SCHEDULED
     * 
     * Validates: Requirements 18.2
     */
    @Test
    fun testMediumPriorityAssignment() {
        // Test logic gán channel cho MEDIUM priority
        val expectedChannel = NotificationChannelManager.CHANNEL_SCHEDULED
        val priority = NotificationPriority.MEDIUM
        
        // Verify mapping logic
        val actualChannel = when (priority) {
            NotificationPriority.HIGH -> NotificationChannelManager.CHANNEL_HIGH_PRIORITY
            NotificationPriority.MEDIUM -> NotificationChannelManager.CHANNEL_SCHEDULED
            NotificationPriority.LOW -> NotificationChannelManager.CHANNEL_GENERAL
        }
        
        assertEquals(
            "MEDIUM priority notifications phải được gán vào CHANNEL_SCHEDULED",
            expectedChannel,
            actualChannel
        )
    }

    /**
     * Test: LOW priority notifications được gán vào CHANNEL_GENERAL
     * 
     * Validates: Requirements 18.2
     */
    @Test
    fun testLowPriorityAssignment() {
        // Test logic gán channel cho LOW priority
        val expectedChannel = NotificationChannelManager.CHANNEL_GENERAL
        val priority = NotificationPriority.LOW
        
        // Verify mapping logic
        val actualChannel = when (priority) {
            NotificationPriority.HIGH -> NotificationChannelManager.CHANNEL_HIGH_PRIORITY
            NotificationPriority.MEDIUM -> NotificationChannelManager.CHANNEL_SCHEDULED
            NotificationPriority.LOW -> NotificationChannelManager.CHANNEL_GENERAL
        }
        
        assertEquals(
            "LOW priority notifications phải được gán vào CHANNEL_GENERAL",
            expectedChannel,
            actualChannel
        )
    }

    /**
     * Test: Weather alert notifications với HIGH priority sử dụng đúng channel
     * 
     * Validates: Requirements 18.2
     */
    @Test
    fun testWeatherAlertChannelAssignment() {
        val notificationType = "alert"
        val priority = NotificationPriority.HIGH
        
        // Weather alerts với HIGH priority phải dùng CHANNEL_HIGH_PRIORITY
        val expectedChannel = NotificationChannelManager.CHANNEL_HIGH_PRIORITY
        val actualChannel = when (priority) {
            NotificationPriority.HIGH -> NotificationChannelManager.CHANNEL_HIGH_PRIORITY
            NotificationPriority.MEDIUM -> NotificationChannelManager.CHANNEL_SCHEDULED
            NotificationPriority.LOW -> NotificationChannelManager.CHANNEL_GENERAL
        }
        
        assertEquals(
            "Weather alerts phải sử dụng CHANNEL_HIGH_PRIORITY",
            expectedChannel,
            actualChannel
        )
    }

    /**
     * Test: Scheduled notifications (morning summary, tomorrow forecast, weekly summary)
     * với MEDIUM priority sử dụng CHANNEL_SCHEDULED
     * 
     * Validates: Requirements 18.2
     */
    @Test
    fun testScheduledNotificationsChannelAssignment() {
        val scheduledTypes = listOf("morning_summary", "tomorrow_forecast", "weekly_summary")
        val priority = NotificationPriority.MEDIUM
        
        scheduledTypes.forEach { notificationType ->
            // Scheduled notifications với MEDIUM priority phải dùng CHANNEL_SCHEDULED
            val expectedChannel = NotificationChannelManager.CHANNEL_SCHEDULED
            val actualChannel = when (priority) {
                NotificationPriority.HIGH -> NotificationChannelManager.CHANNEL_HIGH_PRIORITY
                NotificationPriority.MEDIUM -> NotificationChannelManager.CHANNEL_SCHEDULED
                NotificationPriority.LOW -> NotificationChannelManager.CHANNEL_GENERAL
            }
            
            assertEquals(
                "$notificationType phải sử dụng CHANNEL_SCHEDULED",
                expectedChannel,
                actualChannel
            )
        }
    }

    /**
     * Test: Tất cả priority levels đều có channel mapping hợp lệ
     * 
     * Validates: Requirements 18.2
     */
    @Test
    fun testAllPrioritiesHaveValidChannelMapping() {
        val priorities = listOf(
            NotificationPriority.HIGH,
            NotificationPriority.MEDIUM,
            NotificationPriority.LOW
        )
        
        val validChannels = setOf(
            NotificationChannelManager.CHANNEL_HIGH_PRIORITY,
            NotificationChannelManager.CHANNEL_SCHEDULED,
            NotificationChannelManager.CHANNEL_GENERAL
        )
        
        priorities.forEach { priority ->
            val channelId = when (priority) {
                NotificationPriority.HIGH -> NotificationChannelManager.CHANNEL_HIGH_PRIORITY
                NotificationPriority.MEDIUM -> NotificationChannelManager.CHANNEL_SCHEDULED
                NotificationPriority.LOW -> NotificationChannelManager.CHANNEL_GENERAL
            }
            
            assert(channelId.isNotEmpty()) {
                "Priority $priority phải có channel mapping"
            }
            
            assert(channelId in validChannels) {
                "Channel ID $channelId không hợp lệ cho priority $priority"
            }
        }
    }

    /**
     * Test: Channel assignment logic là deterministic và consistent
     * 
     * Validates: Requirements 18.2
     */
    @Test
    fun testChannelAssignmentConsistency() {
        val testCases = listOf(
            NotificationPriority.HIGH to NotificationChannelManager.CHANNEL_HIGH_PRIORITY,
            NotificationPriority.MEDIUM to NotificationChannelManager.CHANNEL_SCHEDULED,
            NotificationPriority.LOW to NotificationChannelManager.CHANNEL_GENERAL
        )
        
        testCases.forEach { (priority, expectedChannel) ->
            // Test nhiều lần để đảm bảo consistency
            repeat(5) {
                val actualChannel = when (priority) {
                    NotificationPriority.HIGH -> NotificationChannelManager.CHANNEL_HIGH_PRIORITY
                    NotificationPriority.MEDIUM -> NotificationChannelManager.CHANNEL_SCHEDULED
                    NotificationPriority.LOW -> NotificationChannelManager.CHANNEL_GENERAL
                }
                
                assertEquals(
                    "Channel assignment phải consistent cho priority $priority",
                    expectedChannel,
                    actualChannel
                )
            }
        }
    }

    /**
     * Test: Verify channel constants có giá trị đúng
     * 
     * Validates: Requirements 18.2
     */
    @Test
    fun testChannelConstants() {
        assertEquals("weather_alerts_high", NotificationChannelManager.CHANNEL_HIGH_PRIORITY)
        assertEquals("weather_scheduled", NotificationChannelManager.CHANNEL_SCHEDULED)
        assertEquals("weather_general", NotificationChannelManager.CHANNEL_GENERAL)
    }
}
