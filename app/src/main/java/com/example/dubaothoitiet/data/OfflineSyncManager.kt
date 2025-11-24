package com.example.dubaothoitiet.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Manager để tự động sync pending updates khi có mạng trở lại
 * Validates: Requirements 11.5, 15.4
 */
class OfflineSyncManager(
    private val repository: NotificationRepository,
    private val scope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "OfflineSyncManager"
    }
    
    private var isMonitoring = false
    
    /**
     * Bắt đầu monitor network và auto-sync
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "Already monitoring network status")
            return
        }
        
        isMonitoring = true
        Log.d(TAG, "Starting network monitoring and auto-sync")
        
        scope.launch {
            repository.observeNetworkAndAutoSync().collectLatest { isOnline ->
                if (isOnline) {
                    val pendingCount = repository.getPendingUpdateCount()
                    if (pendingCount > 0) {
                        Log.d(TAG, "Network available, syncing $pendingCount pending updates")
                        
                        val result = repository.syncPendingUpdates()
                        when (result) {
                            is Result.Success -> {
                                Log.d(TAG, "Successfully synced all pending updates")
                            }
                            is Result.Error -> {
                                Log.w(TAG, "Some updates failed to sync: ${result.message}")
                            }
                            else -> {}
                        }
                    } else {
                        Log.d(TAG, "Network available but no pending updates")
                    }
                }
            }
        }
    }
    
    /**
     * Dừng monitoring (gọi khi app bị destroy)
     */
    fun stopMonitoring() {
        isMonitoring = false
        Log.d(TAG, "Stopped network monitoring")
    }
    
    /**
     * Trigger manual sync (có thể gọi từ UI)
     */
    suspend fun manualSync(): Result<Unit> {
        Log.d(TAG, "Manual sync triggered")
        return repository.syncPendingUpdates()
    }
    
    /**
     * Lấy số lượng updates đang chờ
     */
    fun getPendingCount(): Int {
        return repository.getPendingUpdateCount()
    }
}
