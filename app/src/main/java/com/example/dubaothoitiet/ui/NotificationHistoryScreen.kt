package com.example.dubaothoitiet.ui

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dubaothoitiet.data.*
import com.example.dubaothoitiet.viewmodel.NotificationHistoryViewModel
import com.example.dubaothoitiet.viewmodel.QuickFilter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Màn hình lịch sử thông báo
 * 
 * Validates: Requirements 14.1, 14.2, 14.3, 14.4, 14.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    viewModel: NotificationHistoryViewModel,
    onBackClick: () -> Unit
) {
    // Collect states từ ViewModel
    val notifications by viewModel.filteredNotifications.collectAsState()
    val selectedNotification by viewModel.selectedNotification.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    // UI states
    var showFilterDialog by remember { mutableStateOf(false) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Lịch sử Thông báo")
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount chưa đọc",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    // Filter button
                    BadgedBox(
                        badge = {
                            if (selectedType != null) {
                                Badge()
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Lọc"
                            )
                        }
                    }
                    
                    // Refresh button
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới"
                        )
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
                isLoading && notifications.isEmpty() -> {
                    // Loading state
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                notifications.isEmpty() -> {
                    // Empty state
                    EmptyStateView(
                        hasFilters = selectedType != null,
                        onClearFilters = { viewModel.clearFilters() }
                    )
                }
                else -> {
                    // Notification list
                    NotificationListContent(
                        notifications = notifications,
                        onNotificationClick = { notification ->
                            viewModel.selectNotification(notification)
                        }
                    )
                }
            }

            // Loading overlay
            AnimatedVisibility(
                visible = isLoading && notifications.isNotEmpty(),
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

    // Filter dialog
    if (showFilterDialog) {
        FilterDialog(
            viewModel = viewModel,
            onDismiss = { showFilterDialog = false }
        )
    }

    // Detail dialog
    selectedNotification?.let { notification ->
        NotificationDetailDialog(
            notification = notification,
            onDismiss = { viewModel.clearSelection() }
        )
    }
}

/**
 * Notification list content
 * Validates: Requirements 14.1, 14.2
 */
@Composable
private fun NotificationListContent(
    notifications: List<NotificationRecord>,
    onNotificationClick: (NotificationRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = notifications,
            key = { it.id }
        ) { notification ->
            NotificationListItem(
                notification = notification,
                onClick = { onNotificationClick(notification) }
            )
        }
    }
}

/**
 * Notification list item
 * Validates: Requirements 14.2
 */
@Composable
private fun NotificationListItem(
    notification: NotificationRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.read) 1.dp else 3.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getColorForNotificationType(notification.notificationType).copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getIconForNotificationType(notification.notificationType),
                        contentDescription = null,
                        tint = getColorForNotificationType(notification.notificationType),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = notification.title,
                    fontSize = 16.sp,
                    fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Body preview
                Text(
                    text = notification.body,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Timestamp and location
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(notification.receivedAt),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    // Priority badge
                    if (notification.priority == NotificationPriority.HIGH) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Ưu tiên cao",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Unread indicator
            if (!notification.read) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.Top)
                ) {}
            }
        }
    }
}

/**
 * Empty state view
 * Validates: Requirements 14.5
 */
@Composable
private fun EmptyStateView(
    hasFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsNone,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (hasFilters) 
                "Không tìm thấy thông báo" 
            else 
                "Chưa có thông báo nào",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (hasFilters)
                "Thử thay đổi bộ lọc để xem thêm thông báo"
            else
                "Bạn sẽ nhận được thông báo về thời tiết tại đây",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        if (hasFilters) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClearFilters) {
                Text("Xóa bộ lọc")
            }
        }
    }
}

/**
 * Filter dialog
 * Validates: Requirements 14.4
 */
@Composable
private fun FilterDialog(
    viewModel: NotificationHistoryViewModel,
    onDismiss: () -> Unit
) {
    val selectedType by viewModel.selectedType.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lọc thông báo") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type filter
                Text(
                    text = "Loại thông báo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // All types option
                    FilterTypeOption(
                        label = "Tất cả",
                        isSelected = selectedType == null,
                        onSelect = { viewModel.filterByType(null) }
                    )
                    
                    // Individual types
                    NotificationType.values().forEach { type ->
                        FilterTypeOption(
                            label = type.displayName,
                            isSelected = selectedType == type,
                            onSelect = { viewModel.filterByType(type) }
                        )
                    }
                }

                HorizontalDivider()

                // Quick date filters
                Text(
                    text = "Khoảng thời gian",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // All time option
                    FilterTypeOption(
                        label = "Tất cả",
                        isSelected = startDate == null && endDate == null,
                        onSelect = { viewModel.filterByDateRange(null, null) }
                    )
                    
                    // Quick filters
                    viewModel.getQuickFilterOptions().forEach { quickFilter ->
                        FilterTypeOption(
                            label = quickFilter.label,
                            isSelected = startDate == quickFilter.startTime && 
                                       endDate == quickFilter.endTime,
                            onSelect = { 
                                viewModel.filterByDateRange(
                                    quickFilter.startTime,
                                    quickFilter.endTime
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.clearFilters()
                    onDismiss()
                }
            ) {
                Text("Xóa bộ lọc")
            }
        }
    )
}

/**
 * Filter type option (radio button)
 */
@Composable
private fun FilterTypeOption(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 14.sp
        )
    }
}

/**
 * Notification detail dialog
 * Validates: Requirements 14.3
 */
@Composable
private fun NotificationDetailDialog(
    notification: NotificationRecord,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = getIconForNotificationType(notification.notificationType),
                contentDescription = null,
                tint = getColorForNotificationType(notification.notificationType),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = notification.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Body
                Text(
                    text = notification.body,
                    fontSize = 14.sp
                )

                HorizontalDivider()

                // Metadata
                DetailRow(
                    label = "Loại",
                    value = notification.notificationType.displayName
                )
                
                DetailRow(
                    label = "Mức độ ưu tiên",
                    value = when (notification.priority) {
                        NotificationPriority.HIGH -> "Cao"
                        NotificationPriority.MEDIUM -> "Trung bình"
                        NotificationPriority.LOW -> "Thấp"
                    }
                )
                
                DetailRow(
                    label = "Thời gian",
                    value = formatFullTimestamp(notification.receivedAt)
                )
                
                notification.locationId?.let {
                    DetailRow(
                        label = "Vị trí",
                        value = "Location ID: $it"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

/**
 * Detail row in dialog
 */
@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp
        )
    }
}

// ==================== Helper Functions ====================

/**
 * Get icon for notification type
 */
private fun getIconForNotificationType(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.ALERT -> Icons.Default.Warning
        NotificationType.MORNING_SUMMARY -> Icons.Default.WbSunny
        NotificationType.TOMORROW_FORECAST -> Icons.Default.CalendarToday
        NotificationType.WEEKLY_SUMMARY -> Icons.Default.DateRange
    }
}

/**
 * Get color for notification type
 */
private fun getColorForNotificationType(type: NotificationType): Color {
    return when (type) {
        NotificationType.ALERT -> Color(0xFFFF5722)
        NotificationType.MORNING_SUMMARY -> Color(0xFFFFC107)
        NotificationType.TOMORROW_FORECAST -> Color(0xFF2196F3)
        NotificationType.WEEKLY_SUMMARY -> Color(0xFF4CAF50)
    }
}

/**
 * Format timestamp to relative time
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Vừa xong"
        diff < 3600_000 -> "${diff / 60_000} phút trước"
        diff < 86400_000 -> "${diff / 3600_000} giờ trước"
        diff < 604800_000 -> "${diff / 86400_000} ngày trước"
        else -> {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * Format full timestamp
 */
private fun formatFullTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
