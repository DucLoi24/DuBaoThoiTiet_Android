package com.example.dubaothoitiet

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WindPower
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.dubaothoitiet.data.TrackedLocation
import com.example.dubaothoitiet.viewmodel.TrackedLocationsViewModel
import com.example.dubaothoitiet.ui.WeatherChartsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackedLocationsScreen(
    userId: String,
    viewModel: TrackedLocationsViewModel,
    onBackClick: () -> Unit,
    onLocationClick: (String) -> Unit,
    onLocationSettingsClick: (Int) -> Unit = {}
) {
    val trackedLocations by viewModel.trackedLocations.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val locationPreferences by viewModel.locationPreferences.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    val snackbarHostState = androidx.compose.material3.SnackbarHostState()

    LaunchedEffect(userId) {
        viewModel.loadTrackedLocations(userId)
    }
    
    // Show success message
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }
    
    // Show error message
    LaunchedEffect(error) {
        error?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { location ->
        DeleteLocationDialog(
            location = location,
            onConfirm = {
                viewModel.deleteLocation(location)
            },
            onDismiss = {
                viewModel.hideDeleteConfirmation()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Các vị trí đã theo dõi") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                trackedLocations.isEmpty() -> {
                    Text(
                        text = "Chưa có vị trí nào được theo dõi",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = Color.Gray
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(trackedLocations) { location ->
                            TrackedLocationCard(
                                location = location,
                                notificationsEnabled = locationPreferences[location.id]?.notificationsEnabled ?: true,
                                onLocationClick = { onLocationClick(location.name) },
                                onNotificationToggle = { enabled ->
                                    viewModel.toggleLocationNotification(location.id, enabled)
                                },
                                onSettingsClick = {
                                    onLocationSettingsClick(location.id)
                                },
                                onDeleteClick = {
                                    viewModel.showDeleteConfirmation(location)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackedLocationCard(
    location: TrackedLocation,
    notificationsEnabled: Boolean,
    onLocationClick: () -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main content - clickable to view weather details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLocationClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon thời tiết
                Image(
                    painter = rememberAsyncImagePainter("https:${location.icon}"),
                    contentDescription = location.conditionText,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Thông tin chính
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = location.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = location.conditionText,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Nhiệt độ lớn
                    Text(
                        text = "${location.tempC.toInt()}°C",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Thông tin bổ sung
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Tỉ lệ mưa
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Tỉ lệ mưa",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF448AFF)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${location.chanceOfRain}%",
                            fontSize = 14.sp,
                            color = Color(0xFF448AFF)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Tốc độ gió
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WindPower,
                            contentDescription = "Gió",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${location.windKph.toInt()} km/h",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Notification controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Notification toggle with icon and label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = "Trạng thái thông báo",
                        modifier = Modifier.size(20.dp),
                        tint = if (notificationsEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = if (notificationsEnabled) "Thông báo đang bật" else "Thông báo đã tắt",
                        fontSize = 14.sp,
                        color = if (notificationsEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onNotificationToggle,
                        modifier = Modifier.height(24.dp)
                    )
                }
                
                // Action buttons
                // Delete button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa vị trí",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Delete confirmation dialog
 * Validates: Requirements 13.5
 */
@Composable
fun DeleteLocationDialog(
    location: TrackedLocation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = "Xóa vị trí?")
        },
        text = {
            Text(
                text = "Bạn có chắc chắn muốn xóa vị trí \"${location.name}\" khỏi danh sách theo dõi?\n\n" +
                       "Tất cả cài đặt thông báo cho vị trí này cũng sẽ bị xóa."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Xóa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
