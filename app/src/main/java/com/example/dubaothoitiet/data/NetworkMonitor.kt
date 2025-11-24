package com.example.dubaothoitiet.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Giám sát trạng thái kết nối mạng
 * Validates: Requirements 11.5, 15.4
 */
class NetworkMonitor(context: Context) {
    
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    companion object {
        private const val TAG = "NetworkMonitor"
    }
    
    /**
     * Kiểm tra xem thiết bị có kết nối mạng không
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Observe trạng thái kết nối mạng với reactive updates
     * Trả về Flow<Boolean> - true khi có mạng, false khi mất mạng
     */
    fun observeNetworkStatus(): Flow<Boolean> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            private val networks = mutableSetOf<Network>()
            
            override fun onAvailable(network: Network) {
                networks.add(network)
                Log.d(TAG, "Network available: $network")
                trySend(true)
            }
            
            override fun onLost(network: Network) {
                networks.remove(network)
                Log.d(TAG, "Network lost: $network")
                // Chỉ emit false nếu không còn network nào
                if (networks.isEmpty()) {
                    trySend(false)
                }
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val isValidated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                
                Log.d(TAG, "Network capabilities changed: hasInternet=$hasInternet, isValidated=$isValidated")
                
                if (hasInternet && isValidated) {
                    networks.add(network)
                    trySend(true)
                } else {
                    networks.remove(network)
                    if (networks.isEmpty()) {
                        trySend(false)
                    }
                }
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Emit trạng thái ban đầu
        trySend(isNetworkAvailable())
        
        awaitClose {
            Log.d(TAG, "Unregistering network callback")
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()
}
