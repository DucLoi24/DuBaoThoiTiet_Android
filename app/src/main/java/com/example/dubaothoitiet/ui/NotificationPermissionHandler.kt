package com.example.dubaothoitiet.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat

/**
 * Composable function để xử lý notification permission request với UI đầy đủ
 * 
 * Bao gồm:
 * - Rationale dialog giải thích tại sao cần permission
 * - Xử lý trạng thái granted/denied
 * - Hướng dẫn mở Settings nếu permission bị từ chối vĩnh viễn
 * 
 * Chỉ hoạt động trên Android 13 (API 33) trở lên
 * 
 * @param onPermissionResult Callback khi user grant/deny permission
 * 
 * Validates: Requirements 12.1
 * Task 32: Notification permission request flow
 */
@Composable
fun NotificationPermissionHandler(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showDeniedDialog by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("NotificationPermission", "Permission result: $isGranted")
        
        // Lưu kết quả vào SharedPreferences
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("notification_permission_granted", isGranted)
            .putBoolean("notification_permission_requested", true)
            .apply()
        
        if (!isGranted) {
            // Hiển thị dialog giải thích khi bị từ chối
            showDeniedDialog = true
        }
        
        onPermissionResult(isGranted)
    }

    // Request permission khi app khởi động
    LaunchedEffect(Unit) {
        // Chỉ request trên Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val alreadyRequested = prefs.getBoolean("notification_permission_requested", false)
            
            // Kiểm tra xem đã có permission chưa
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                Log.d("NotificationPermission", "Permission already granted")
                prefs.edit()
                    .putBoolean("notification_permission_granted", true)
                    .apply()
                onPermissionResult(true)
            } else if (!alreadyRequested && !permissionRequested) {
                // Hiển thị rationale trước khi request permission
                Log.d("NotificationPermission", "Showing rationale dialog")
                showRationaleDialog = true
            } else {
                Log.d("NotificationPermission", "Permission already requested before")
                val wasGranted = prefs.getBoolean("notification_permission_granted", false)
                onPermissionResult(wasGranted)
            }
        } else {
            // Android < 13 không cần request permission
            Log.d("NotificationPermission", "Android < 13, permission not needed")
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("notification_permission_granted", true)
                .apply()
            onPermissionResult(true)
        }
    }

    // Rationale Dialog - Giải thích tại sao cần permission
    if (showRationaleDialog) {
        NotificationPermissionRationaleDialog(
            onDismiss = {
                showRationaleDialog = false
                // Nếu user dismiss mà không cho phép, coi như từ chối
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("notification_permission_requested", true)
                    .putBoolean("notification_permission_granted", false)
                    .apply()
                onPermissionResult(false)
            },
            onConfirm = {
                showRationaleDialog = false
                permissionRequested = true
                // Request permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    }

    // Denied Dialog - Hướng dẫn khi permission bị từ chối
    if (showDeniedDialog) {
        NotificationPermissionDeniedDialog(
            onDismiss = {
                showDeniedDialog = false
            },
            onOpenSettings = {
                showDeniedDialog = false
                // Mở Settings để user có thể cấp permission thủ công
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }
}

/**
 * Dialog hiển thị rationale - giải thích tại sao app cần notification permission
 * 
 * Task 32: Hiển thị rationale cho notification permission
 */
@Composable
fun NotificationPermissionRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Cho phép thông báo",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Ứng dụng cần quyền thông báo để:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("• Cảnh báo thời tiết nguy hiểm (mưa lớn, bão, nhiệt độ cực đoan)")
                    Text("• Gửi tóm tắt thời tiết buổi sáng")
                    Text("• Thông báo dự báo thời tiết ngày mai")
                    Text("• Cập nhật tóm tắt thời tiết hàng tuần")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Không cho phép")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cho phép")
                    }
                }
            }
        }
    }
}

/**
 * Dialog hiển thị khi permission bị từ chối
 * Hướng dẫn user mở Settings để cấp permission
 * 
 * Task 32: Xử lý permission denied state
 */
@Composable
fun NotificationPermissionDeniedDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications Disabled",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Thông báo bị tắt",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Bạn sẽ không nhận được:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("• Cảnh báo thời tiết nguy hiểm")
                    Text("• Tóm tắt thời tiết hàng ngày")
                    Text("• Dự báo thời tiết")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Bạn có thể bật lại thông báo trong Cài đặt hệ thống.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Đóng")
                    }
                    
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mở Cài đặt")
                    }
                }
            }
        }
    }
}

/**
 * Helper function để kiểm tra notification permission
 * 
 * @param context Application context
 * @return true nếu có permission hoặc không cần permission (Android < 13)
 */
fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        // Android < 13 không cần permission
        true
    }
}

/**
 * Helper function để kiểm tra xem đã request permission chưa
 * 
 * @param context Application context
 * @return true nếu đã request permission
 */
fun wasNotificationPermissionRequested(context: Context): Boolean {
    return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .getBoolean("notification_permission_requested", false)
}

/**
 * Helper function để kiểm tra xem notification features có được bật không
 * Dựa trên permission status
 * 
 * Task 32: Vô hiệu hóa tính năng thông báo nếu permission bị từ chối
 * 
 * @param context Application context
 * @return true nếu notification features được bật
 */
fun areNotificationFeaturesEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val permissionGranted = prefs.getBoolean("notification_permission_granted", false)
    
    // Nếu Android < 13, luôn enabled
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }
    
    // Nếu Android >= 13, check permission
    return permissionGranted && hasNotificationPermission(context)
}
