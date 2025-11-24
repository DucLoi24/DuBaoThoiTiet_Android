package com.example.dubaothoitiet.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dubaothoitiet.data.*
import com.example.dubaothoitiet.viewmodel.NotificationSettingsViewModel

/**
 * Màn hình cài đặt thông báo
 * 
 * Task 32: Vô hiệu hóa tính năng thông báo nếu permission bị từ chối
 * Validates: Requirements 11.1, 11.3, 11.4, 12.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Kiểm tra notification permission
    val notificationFeaturesEnabled = remember { areNotificationFeaturesEnabled(context) }
    
    // Collect states từ ViewModel
    val preferences by viewModel.preferences.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val validationError by viewModel.validationError.collectAsState()

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Hiển thị error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearErrorMessage()
        }
    }

    // Hiển thị success messages
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }

    // Hiển thị validation errors
    LaunchedEffect(validationError) {
        validationError?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearValidationError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt Thông báo") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    // Sync button - chỉ enabled khi có permission
                    IconButton(
                        onClick = { viewModel.syncWithBackend() },
                        enabled = notificationFeaturesEnabled && syncStatus != SyncStatus.SYNCING
                    ) {
                        when (syncStatus) {
                            SyncStatus.SYNCING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Đồng bộ"
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Task 32: Hiển thị warning khi không có permission
                !notificationFeaturesEnabled -> {
                    NotificationPermissionWarning(
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                isLoading && preferences == null -> {
                    // Loading state khi chưa có dữ liệu
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                preferences == null -> {
                    // Error state
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Không thể tải cài đặt",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Thử lại")
                        }
                    }
                }
                else -> {
                    // Main content
                    NotificationSettingsContent(
                        preferences = preferences!!,
                        onNotificationsEnabledToggle = { enabled ->
                            viewModel.toggleNotificationsEnabled(enabled)
                        },
                        onEventTypeToggle = { eventType, enabled ->
                            viewModel.toggleEventType(eventType, enabled)
                        },
                        onScheduleChange = { schedule ->
                            viewModel.updateSchedule(schedule)
                        },
                        onScheduledNotificationToggle = { type, enabled ->
                            viewModel.toggleScheduledNotification(type, enabled)
                        }
                    )
                }
            }

            // Loading overlay khi đang sync
            AnimatedVisibility(
                visible = isLoading && preferences != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}


/**
 * Main content của màn hình cài đặt thông báo
 */
@Composable
private fun NotificationSettingsContent(
    preferences: NotificationPreferences,
    onNotificationsEnabledToggle: (Boolean) -> Unit,
    onEventTypeToggle: (WeatherEventType, Boolean) -> Unit,
    onScheduleChange: (NotificationSchedule) -> Unit,
    onScheduledNotificationToggle: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Bật/Tắt thông báo tổng thể
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (preferences.notificationsEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (preferences.notificationsEnabled) {
                                "Thông báo đang bật"
                            } else {
                                "Thông báo đang tắt"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (preferences.notificationsEnabled) {
                                "Bạn sẽ nhận được thông báo thời tiết"
                            } else {
                                "Bạn sẽ không nhận được thông báo"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Switch(
                        checked = preferences.notificationsEnabled,
                        onCheckedChange = onNotificationsEnabledToggle
                    )
                }
            }
        }
        
        // Section: Widget thời tiết
        item {
            WeatherWidgetToggle()
        }
        
        // Section: Cài đặt widget
        item {
            WidgetForecastHoursSetting()
        }
        
        // Divider
        item {
            HorizontalDivider()
        }
        
        // Section: Loại sự kiện thời tiết
        item {
            SectionHeader(
                title = "Loại Cảnh báo Thời tiết",
                icon = Icons.Default.Warning
            )
        }

        item {
            EventTypesSection(
                enabledEventTypes = preferences.enabledEventTypes,
                onEventTypeToggle = onEventTypeToggle
            )
        }

        // Section: Lịch trình thông báo
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Lịch trình Nhận Thông báo",
                icon = Icons.Default.Schedule
            )
        }

        item {
            NotificationScheduleSection(
                currentSchedule = preferences.notificationSchedule,
                onScheduleChange = onScheduleChange
            )
        }

        // Section: Thông báo định kỳ
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Thông báo Định kỳ",
                icon = Icons.Default.Notifications
            )
        }

        item {
            ScheduledNotificationsSection(
                morningSummaryEnabled = preferences.morningSummaryEnabled,
                tomorrowForecastEnabled = preferences.tomorrowForecastEnabled,
                weeklySummaryEnabled = preferences.weeklySummaryEnabled,
                onToggle = onScheduledNotificationToggle
            )
        }

        // Info card
        item {
            Spacer(modifier = Modifier.height(8.dp))
            InfoCard()
        }
    }
}

/**
 * Section header với icon
 */
@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Section cho event type toggles - Hiển thị theo nhóm
 * Validates: Requirements 11.1
 */
@Composable
private fun EventTypesSection(
    enabledEventTypes: List<WeatherEventType>,
    onEventTypeToggle: (WeatherEventType, Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Nhóm Lũ lụt và Mưa
        EventCategoryCard(
            title = "Lũ lụt và Mưa",
            icon = Icons.Default.WaterDrop,
            eventTypes = WeatherEventType.getByCategory("water"),
            enabledEventTypes = enabledEventTypes,
            onEventTypeToggle = onEventTypeToggle
        )
        
        // Nhóm Bão và Gió
        EventCategoryCard(
            title = "Bão và Gió",
            icon = Icons.Default.Air,
            eventTypes = WeatherEventType.getByCategory("storm"),
            enabledEventTypes = enabledEventTypes,
            onEventTypeToggle = onEventTypeToggle
        )
        
        // Nhóm Nhiệt độ
        EventCategoryCard(
            title = "Nhiệt độ",
            icon = Icons.Default.Thermostat,
            eventTypes = WeatherEventType.getByCategory("temperature"),
            enabledEventTypes = enabledEventTypes,
            onEventTypeToggle = onEventTypeToggle
        )
        
        // Nhóm Sức khỏe (UV & AQI)
        EventCategoryCard(
            title = "Sức khỏe (UV & Chất lượng không khí)",
            icon = Icons.Default.HealthAndSafety,
            eventTypes = WeatherEventType.getByCategory("health"),
            enabledEventTypes = enabledEventTypes,
            onEventTypeToggle = onEventTypeToggle
        )
        
        // Nhóm Cảnh báo chính thức
        EventCategoryCard(
            title = "Cảnh báo Chính thức",
            icon = Icons.Default.Announcement,
            eventTypes = WeatherEventType.getByCategory("official"),
            enabledEventTypes = enabledEventTypes,
            onEventTypeToggle = onEventTypeToggle
        )
    }
}

/**
 * Card cho một nhóm event types
 */
@Composable
private fun EventCategoryCard(
    title: String,
    icon: ImageVector,
    eventTypes: List<WeatherEventType>,
    enabledEventTypes: List<WeatherEventType>,
    onEventTypeToggle: (WeatherEventType, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header với toggle expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Thu gọn" else "Mở rộng",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Danh sách event types
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                eventTypes.forEach { eventType ->
                    EventTypeToggleItem(
                        eventType = eventType,
                        isEnabled = enabledEventTypes.contains(eventType),
                        onToggle = { enabled ->
                            onEventTypeToggle(eventType, enabled)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Toggle item cho một loại sự kiện thời tiết
 */
@Composable
private fun EventTypeToggleItem(
    eventType: WeatherEventType,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = getIconForEventType(eventType),
                contentDescription = null,
                tint = getColorForEventType(eventType),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = eventType.displayName,
                fontSize = 16.sp
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

/**
 * Section cho notification schedule
 * Validates: Requirements 11.1
 */
@Composable
private fun NotificationScheduleSection(
    currentSchedule: NotificationSchedule,
    onScheduleChange: (NotificationSchedule) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NotificationSchedule.values().forEach { schedule ->
                ScheduleOptionItem(
                    schedule = schedule,
                    isSelected = currentSchedule == schedule,
                    onSelect = { onScheduleChange(schedule) }
                )
            }
            
            // Thông tin bổ sung
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lưu ý: Cảnh báo ưu tiên cao sẽ luôn được gửi ngay lập tức bất kể lịch trình",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 36.dp)
            )
        }
    }
}

/**
 * Radio button item cho schedule option
 */
@Composable
private fun ScheduleOptionItem(
    schedule: NotificationSchedule,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = schedule.getDisplayName(),
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (schedule == NotificationSchedule.DAYTIME_ONLY) {
                Text(
                    text = "Thông báo sẽ được gửi từ 6:00 sáng đến 10:00 tối",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Section cho scheduled notifications
 * Validates: Requirements 11.1
 */
@Composable
private fun ScheduledNotificationsSection(
    morningSummaryEnabled: Boolean,
    tomorrowForecastEnabled: Boolean,
    weeklySummaryEnabled: Boolean,
    onToggle: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScheduledNotificationItem(
                title = "Tóm tắt Buổi sáng",
                description = "Nhận tóm tắt thời tiết lúc 7:00 sáng mỗi ngày",
                icon = Icons.Default.WbSunny,
                isEnabled = morningSummaryEnabled,
                onToggle = { onToggle("morning_summary", it) }
            )
            
            HorizontalDivider()
            
            ScheduledNotificationItem(
                title = "Dự báo Ngày mai",
                description = "Nhận dự báo thời tiết ngày mai lúc 8:00 tối",
                icon = Icons.Default.CalendarToday,
                isEnabled = tomorrowForecastEnabled,
                onToggle = { onToggle("tomorrow_forecast", it) }
            )
            
            HorizontalDivider()
            
            ScheduledNotificationItem(
                title = "Tóm tắt Tuần",
                description = "Nhận tóm tắt thời tiết tuần lúc 8:00 tối Chủ nhật",
                icon = Icons.Default.DateRange,
                isEnabled = weeklySummaryEnabled,
                onToggle = { onToggle("weekly_summary", it) }
            )
        }
    }
}

/**
 * Toggle item cho scheduled notification
 */
@Composable
private fun ScheduledNotificationItem(
    title: String,
    description: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

/**
 * Toggle cho Weather Widget
 */
@Composable
private fun WeatherWidgetToggle() {
    val context = LocalContext.current
    val sharedPrefs = remember { 
        context.getSharedPreferences("weather_widget_prefs", android.content.Context.MODE_PRIVATE) 
    }
    var widgetEnabled by remember { 
        mutableStateOf(sharedPrefs.getBoolean("widget_enabled", true)) 
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (widgetEnabled) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = if (widgetEnabled) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Widget Thời tiết",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (widgetEnabled) {
                            "Hiển thị thời tiết trên thanh thông báo"
                        } else {
                            "Widget đã tắt"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Switch(
                checked = widgetEnabled,
                onCheckedChange = { enabled ->
                    widgetEnabled = enabled
                    sharedPrefs.edit().putBoolean("widget_enabled", enabled).apply()
                    
                    // Start hoặc stop service
                    if (enabled) {
                        com.example.dubaothoitiet.service.WeatherNotificationService.start(context)
                    } else {
                        com.example.dubaothoitiet.service.WeatherNotificationService.stop(context)
                    }
                }
            )
        }
    }
}

/**
 * Setting cho số giờ dự báo trong widget
 */
@Composable
private fun WidgetForecastHoursSetting() {
    val context = LocalContext.current
    val sharedPrefs = remember { 
        context.getSharedPreferences("weather_widget_prefs", android.content.Context.MODE_PRIVATE) 
    }
    var forecastHours by remember { 
        mutableStateOf(sharedPrefs.getInt("forecast_hours", 3)) 
    }
    var showDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Dự báo nhiệt độ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Hiển thị nhiệt độ trong $forecastHours giờ tới",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            TextButton(onClick = { showDialog = true }) {
                Text("$forecastHours giờ")
            }
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Chọn số giờ dự báo") },
            text = {
                Column {
                    listOf(1, 2, 3, 4, 6, 8, 12, 24).forEach { hours ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = forecastHours == hours,
                                onClick = {
                                    forecastHours = hours
                                    sharedPrefs.edit().putInt("forecast_hours", hours).apply()
                                    showDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$hours giờ")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

/**
 * Info card với thông tin hữu ích
 */
@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Thông tin",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Cài đặt được tự động đồng bộ với server\n" +
                           "• Bạn có thể quản lý thông báo cho từng vị trí riêng biệt\n" +
                           "• Cảnh báo nguy hiểm luôn được ưu tiên cao nhất",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Helper function để lấy icon cho event type
 */
private fun getIconForEventType(eventType: WeatherEventType): ImageVector {
    return when (eventType.category) {
        "water" -> Icons.Default.WaterDrop
        "storm" -> Icons.Default.Air
        "temperature" -> if (eventType == WeatherEventType.EXTREME_HEAT) Icons.Default.WbSunny else Icons.Default.AcUnit
        "health" -> Icons.Default.HealthAndSafety
        "official" -> Icons.Default.Announcement
        "general" -> Icons.Default.LightMode
        else -> Icons.Default.Info
    }
}

/**
 * Helper function để lấy màu cho event type
 */
private fun getColorForEventType(eventType: WeatherEventType): Color {
    return when (eventType) {
        // Lũ lụt và Mưa
        WeatherEventType.FLOOD_RISK -> Color(0xFF1565C0) // Xanh đậm
        WeatherEventType.HEAVY_RAIN -> Color(0xFF2196F3) // Xanh
        WeatherEventType.MODERATE_RAIN -> Color(0xFF64B5F6) // Xanh nhạt
        
        // Bão và Gió
        WeatherEventType.SUPER_TYPHOON -> Color(0xFF880E4F) // Đỏ tím đậm
        WeatherEventType.TYPHOON -> Color(0xFF9C27B0) // Tím
        WeatherEventType.TROPICAL_STORM -> Color(0xFFAB47BC) // Tím nhạt
        WeatherEventType.TROPICAL_DEPRESSION -> Color(0xFFBA68C8) // Tím rất nhạt
        WeatherEventType.STRONG_WIND -> Color(0xFF7E57C2) // Tím xanh
        
        // Nhiệt độ
        WeatherEventType.EXTREME_HEAT -> Color(0xFFFF5722) // Đỏ cam
        WeatherEventType.EXTREME_COLD -> Color(0xFF00BCD4) // Xanh lơ
        
        // UV Index
        WeatherEventType.UV_EXTREME -> Color(0xFFD32F2F) // Đỏ đậm
        WeatherEventType.UV_VERY_HIGH -> Color(0xFFFF6F00) // Cam đậm
        WeatherEventType.UV_HIGH -> Color(0xFFFFA726) // Cam
        
        // AQI
        WeatherEventType.AQI_HAZARDOUS -> Color(0xFF6A1B9A) // Tím đậm (Maroon)
        WeatherEventType.AQI_VERY_UNHEALTHY -> Color(0xFF8E24AA) // Tím (Purple)
        WeatherEventType.AQI_UNHEALTHY -> Color(0xFFE53935) // Đỏ
        WeatherEventType.AQI_UNHEALTHY_SENSITIVE -> Color(0xFFFB8C00) // Cam
        
        // Cảnh báo chính thức
        WeatherEventType.OFFICIAL_ALERT -> Color(0xFFC62828) // Đỏ đậm
        
        // Thời tiết đẹp
        WeatherEventType.SUNNY -> Color(0xFFFFC107) // Vàng
    }
}

/**
 * Warning screen khi notification permission bị từ chối
 * 
 * Task 32: Vô hiệu hóa tính năng thông báo nếu permission bị từ chối
 */
@Composable
private fun NotificationPermissionWarning(
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsOff,
            contentDescription = "Notifications Disabled",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Thông báo bị tắt",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Ứng dụng không có quyền gửi thông báo. Các tính năng thông báo đã bị vô hiệu hóa.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bạn sẽ không nhận được:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text("• Cảnh báo thời tiết nguy hiểm", color = MaterialTheme.colorScheme.onErrorContainer)
                Text("• Tóm tắt thời tiết buổi sáng", color = MaterialTheme.colorScheme.onErrorContainer)
                Text("• Dự báo thời tiết ngày mai", color = MaterialTheme.colorScheme.onErrorContainer)
                Text("• Tóm tắt thời tiết hàng tuần", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mở Cài đặt để Bật Thông báo")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Để bật thông báo, vui lòng vào Cài đặt > Ứng dụng > Dự báo Thời tiết > Thông báo",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
