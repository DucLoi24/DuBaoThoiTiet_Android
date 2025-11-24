package com.example.dubaothoitiet

import android.Manifest
import android.content.pm.PackageManager // THÊM IMPORT
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.* // THÊM IMPORT Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp // THÊM IMPORT
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WindPower
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.* // THÊM IMPORT (DropdownMenu, DropdownMenuItem)
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import coil.compose.rememberAsyncImagePainter
import com.example.dubaothoitiet.data.ForecastDay
import com.example.dubaothoitiet.data.Hour
import com.example.dubaothoitiet.data.WeatherResponse
import com.example.dubaothoitiet.ui.theme.DuBaoThoiTietTheme
import com.example.dubaothoitiet.viewmodel.AuthViewModel
import com.example.dubaothoitiet.viewmodel.LocationViewModel
import com.example.dubaothoitiet.viewmodel.UserViewModel
import com.example.dubaothoitiet.viewmodel.WeatherViewModel
import com.example.dubaothoitiet.viewmodel.AlertViewModel
import com.example.dubaothoitiet.viewmodel.AdviceViewModel
import com.example.dubaothoitiet.viewmodel.AdviceState
import com.example.dubaothoitiet.viewmodel.CombinedAdviceViewModel
import com.example.dubaothoitiet.viewmodel.CombinedAdviceUiState
import com.example.dubaothoitiet.viewmodel.TrackedLocationsViewModel
import com.example.dubaothoitiet.data.ExtremeAlert
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import androidx.compose.material.icons.filled.WaterDrop
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val weatherViewModel: WeatherViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()
    private val combinedAdviceViewModel: CombinedAdviceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DuBaoThoiTietTheme {
                WeatherApp(
                    weatherViewModel, 
                    authViewModel, 
                    userViewModel, 
                    locationViewModel, 
                    combinedAdviceViewModel
                )
            }
        }
    }
}

fun removeVietnameseAccents(str: String): String {
    val nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD)
    val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(nfdNormalizedString).replaceAll("")
        .replace("đ", "d").replace("Đ", "D")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherApp(
    weatherViewModel: WeatherViewModel,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    locationViewModel: LocationViewModel,
    combinedAdviceViewModel: CombinedAdviceViewModel
) {
    val weatherResult by weatherViewModel.weatherResult.observeAsState()
    var city by remember { mutableStateOf("Hanoi") }
    val user by userViewModel.user.observeAsState()
    var showAuthDialog by remember { mutableStateOf(false) }
    var showTrackedLocations by remember { mutableStateOf(false) }
    var showNotificationSettings by remember { mutableStateOf(false) }
    var showNotificationHistory by remember { mutableStateOf(false) }
    var showLocationSettings by remember { mutableStateOf<Pair<Int, String>?>(null) } // Pair(locationId, locationName)

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val lat = location.latitude
                            val lon = location.longitude
                            city = "Vị trí hiện tại"
                            weatherViewModel.getWeather("$lat,$lon")
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Không thể lấy vị trí. Vui lòng thử lại.")
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e("WeatherApp", "SecurityException in location permission", e)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Lỗi bảo mật khi truy cập vị trí.")
                    }
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Quyền truy cập vị trí bị từ chối.")
                }
            }
        }
    )

    // --- [YÊU CẦU 1: TỰ ĐỘNG LẤY VỊ TRÍ KHI MỞ APP] ---
    LaunchedEffect(Unit) {
        // 1. Kiểm tra xem đã có quyền chưa
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 2. Nếu đã có quyền, lấy vị trí ngay
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val lat = location.latitude
                        val lon = location.longitude
                        city = "Vị trí hiện tại"
                        weatherViewModel.getWeather("$lat,$lon")
                    } else {
                        // Nếu có quyền nhưng không lấy được (GPS tắt?), yêu cầu lại
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            } catch (e: SecurityException) {
                // Lỗi bảo mật, yêu cầu lại quyền
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            // 3. Nếu chưa có quyền, yêu cầu quyền
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    // --- [KẾT THÚC YÊU CẦU 1] ---

    // Kiểm tra xem có đang hiển thị màn hình phụ không
    val showingSubScreen = showNotificationSettings || showNotificationHistory || 
                          showTrackedLocations || showLocationSettings != null

    LaunchedEffect(weatherResult) {
        val weatherData = weatherResult?.getOrNull()
        if (weatherData != null) {
            // Lấy tên không dấu để gọi API alerts
            val locationNameEn = removeVietnameseAccents(weatherData.location.name)
            combinedAdviceViewModel.checkOrFetchAdvice(locationNameEn)
        } else {
            combinedAdviceViewModel.resetState()
        }
    }

    // Tự động cập nhật thời tiết mỗi 1 phút
    LaunchedEffect(city, showingSubScreen) {
        while (true) {
            kotlinx.coroutines.delay(60 * 1000) // 1 phút
            if (city.isNotEmpty() && !showingSubScreen) {
                val searchCity = if (city == "Vị trí hiện tại") {
                    // Nếu đang xem vị trí hiện tại, lấy lại vị trí
                    try {
                        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null) {
                                    val lat = location.latitude
                                    val lon = location.longitude
                                    weatherViewModel.getWeather("$lat,$lon")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WeatherApp", "Error refreshing location", e)
                    }
                    null
                } else {
                    removeVietnameseAccents(city)
                }
                
                searchCity?.let { weatherViewModel.getWeather(it) }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Chỉ hiển thị TopAppBar khi ở màn hình chính
            if (!showingSubScreen) {
                TopAppBar(
                    title = { Text("Dự báo thời tiết") },
                    actions = {
                    // Nút 1: Lấy vị trí hiện tại (Giữ nguyên)
                    IconButton(onClick = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Vị trí hiện tại")
                    }

                    // --- [YÊU CẦU 2 & SỬA LỖI SMART CAST] ---
                    var showMenu by remember { mutableStateOf(false) }

                    // [SỬA LỖI]: Tạo một biến local từ 'user'
                    val currentUser = user

                    if (currentUser == null) {
                        // CHƯA ĐĂNG NHẬP: Bấm vào để mở Dialog
                        IconButton(onClick = { showAuthDialog = true }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Tài khoản")
                        }
                    } else {
                        // ĐÃ ĐĂNG NHẬP: Hiển thị tên và menu (Sử dụng currentUser)
                        Box {
                            Row(
                                modifier = Modifier
                                    .clickable { showMenu = true }
                                    .padding(8.dp), // Thêm padding cho dễ bấm
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // SỬA LỖI: Username của bạn đang được lưu trong trường 'email'
                                Text(currentUser.email ?: "User") // [SỬA LỖI]: Dùng currentUser
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.AccountCircle, contentDescription = "Tài khoản")
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Các vị trí đã theo dõi") },
                                    onClick = {
                                        Log.d("MainActivity", "TrackedLocations clicked, user=${currentUser?.userId}")
                                        showTrackedLocations = true
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.MyLocation, contentDescription = "Vị trí đã theo dõi")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cài đặt thông báo") },
                                    onClick = {
                                        showNotificationSettings = true
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, contentDescription = "Cài đặt thông báo")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Lịch sử thông báo") },
                                    onClick = {
                                        showNotificationHistory = true
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.History, contentDescription = "Lịch sử thông báo")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Đăng xuất") },
                                    onClick = {
                                        authViewModel.logout()
                                        userViewModel.onLogout(context)
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ExitToApp, contentDescription = "Đăng xuất")
                                    }
                                )
                            }
                        }
                    }
                    // --- [KẾT THÚC YÊU CẦU 2] ---
                    }
                )
            }
        }
    ) { paddingValues ->
        Log.d("MainActivity", "Scaffold body - showTrackedLocations=$showTrackedLocations, user=${user?.userId}, showNotificationSettings=$showNotificationSettings, showNotificationHistory=$showNotificationHistory")
        when {
            showNotificationSettings && user != null -> {
                val database = com.example.dubaothoitiet.data.database.NotificationDatabase.getDatabase(context)
                val networkMonitor = com.example.dubaothoitiet.data.NetworkMonitor(context)
                val notificationRepository = remember {
                    com.example.dubaothoitiet.data.NotificationRepository(
                        apiService = com.example.dubaothoitiet.api.RetrofitInstance.api,
                        database = database,
                        networkMonitor = networkMonitor
                    )
                }
                val viewModel: com.example.dubaothoitiet.viewmodel.NotificationSettingsViewModel = 
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.example.dubaothoitiet.viewmodel.NotificationSettingsViewModelFactory(
                            repository = notificationRepository,
                            userId = user!!.userId.toInt()
                        )
                    )
                
                BackHandler {
                    showNotificationSettings = false
                }
                
                com.example.dubaothoitiet.ui.NotificationSettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { showNotificationSettings = false }
                )
            }
            
            showNotificationHistory && user != null -> {
                val database = com.example.dubaothoitiet.data.database.NotificationDatabase.getDatabase(context)
                val networkMonitor = com.example.dubaothoitiet.data.NetworkMonitor(context)
                val notificationRepository = remember {
                    com.example.dubaothoitiet.data.NotificationRepository(
                        apiService = com.example.dubaothoitiet.api.RetrofitInstance.api,
                        database = database,
                        networkMonitor = networkMonitor
                    )
                }
                val viewModel: com.example.dubaothoitiet.viewmodel.NotificationHistoryViewModel = 
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.example.dubaothoitiet.viewmodel.NotificationHistoryViewModelFactory(
                            repository = notificationRepository,
                            userId = user!!.userId.toInt()
                        )
                    )
                
                BackHandler {
                    showNotificationHistory = false
                }
                
                com.example.dubaothoitiet.ui.NotificationHistoryScreen(
                    viewModel = viewModel,
                    onBackClick = { showNotificationHistory = false }
                )
            }
            
            showLocationSettings != null && user != null -> {
                val (locationId, locationName) = showLocationSettings!!
                val database = com.example.dubaothoitiet.data.database.NotificationDatabase.getDatabase(context)
                val networkMonitor = com.example.dubaothoitiet.data.NetworkMonitor(context)
                val notificationRepository = remember {
                    com.example.dubaothoitiet.data.NotificationRepository(
                        apiService = com.example.dubaothoitiet.api.RetrofitInstance.api,
                        database = database,
                        networkMonitor = networkMonitor
                    )
                }
                val viewModel: com.example.dubaothoitiet.viewmodel.TrackedLocationsViewModel = 
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.example.dubaothoitiet.viewmodel.TrackedLocationsViewModelFactory(
                            repository = notificationRepository
                        )
                    )
                
                val locationPreferences by viewModel.locationPreferences.collectAsState()
                val isLoading by viewModel.isLoading.observeAsState(false)
                
                // Load preferences nếu chưa có
                LaunchedEffect(locationId) {
                    if (!locationPreferences.containsKey(locationId)) {
                        viewModel.loadTrackedLocations(user!!.userId)
                    }
                }
                
                BackHandler {
                    showLocationSettings = null
                    showTrackedLocations = true
                }
                
                com.example.dubaothoitiet.ui.LocationNotificationSettingsScreen(
                    locationId = locationId,
                    locationName = locationName,
                    notificationsEnabled = locationPreferences[locationId]?.notificationsEnabled ?: true,
                    onNotificationToggle = { enabled ->
                        viewModel.toggleLocationNotification(locationId, enabled)
                    },
                    onBackClick = { 
                        showLocationSettings = null
                        showTrackedLocations = true
                    },
                    isLoading = isLoading
                )
            }
            
            showTrackedLocations && user != null -> {
                val database = com.example.dubaothoitiet.data.database.NotificationDatabase.getDatabase(context)
                val networkMonitor = com.example.dubaothoitiet.data.NetworkMonitor(context)
                val notificationRepository = remember {
                    com.example.dubaothoitiet.data.NotificationRepository(
                        apiService = com.example.dubaothoitiet.api.RetrofitInstance.api,
                        database = database,
                        networkMonitor = networkMonitor
                    )
                }
                val viewModel: com.example.dubaothoitiet.viewmodel.TrackedLocationsViewModel = 
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.example.dubaothoitiet.viewmodel.TrackedLocationsViewModelFactory(
                            repository = notificationRepository
                        )
                    )
                
                BackHandler {
                    showTrackedLocations = false
                }
                
                TrackedLocationsScreen(
                    userId = user!!.userId,
                    viewModel = viewModel,
                    onBackClick = { showTrackedLocations = false },
                    onLocationClick = { locationName ->
                        city = locationName
                        val searchCity = removeVietnameseAccents(locationName)
                        weatherViewModel.getWeather(searchCity)
                        combinedAdviceViewModel.resetState()
                        showTrackedLocations = false
                    },
                    onLocationSettingsClick = { locationId ->
                        // Tìm location name từ danh sách
                        val location = viewModel.trackedLocations.value?.find { it.id == locationId }
                        if (location != null) {
                            showLocationSettings = Pair(locationId, location.name)
                            showTrackedLocations = false
                        }
                    }
                )
            }
            
            else -> {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (showAuthDialog) {
                        Dialog(onDismissRequest = { showAuthDialog = false }) {
                            AuthScreen(authViewModel, userViewModel) {
                                showAuthDialog = false
                            }
                        }
                    }

                    WeatherScreen(
                weatherResult = weatherResult,
                combinedAdviceViewModel = combinedAdviceViewModel,
                city = city,
                onCityChange = { city = it },
                onSearch = {
                    val searchCity = removeVietnameseAccents(city)
                    weatherViewModel.getWeather(searchCity)
                    combinedAdviceViewModel.resetState()
                },
                onTrackLocation = { weatherResponse: WeatherResponse ->
                    // [SỬA LỖI]: Dùng currentUser để kiểm tra
                    val currentUser = user
                    if (currentUser != null) {
                        locationViewModel.trackLocation(
                            userId = currentUser.userId, // [SỬA LỖI]: Dùng currentUser
                            name = weatherResponse.location.name,
                            nameEn = removeVietnameseAccents(weatherResponse.location.name),
                            lat = weatherResponse.location.lat,
                            lon = weatherResponse.location.lon
                        )
                    }
                },
                isLoggedIn = (user != null) // Kiểm tra user gốc
            )
                }  // Đóng Column
            }      // Đóng else
        }          // Đóng when
    }              // Đóng Scaffold body
}


@Composable
fun WeatherScreen(
    weatherResult: Result<WeatherResponse>?,
    combinedAdviceViewModel: CombinedAdviceViewModel,
    city: String,
    onCityChange: (String) -> Unit,
    onSearch: () -> Unit,
    onTrackLocation: (WeatherResponse) -> Unit,
    isLoggedIn: Boolean
) {
    var selectedDay by remember { mutableStateOf<ForecastDay?>(null) }
    var selectedHour by remember { mutableStateOf<Hour?>(null) }
    val weatherData = weatherResult?.getOrNull()
    val combinedState = combinedAdviceViewModel.uiState
    val context = LocalContext.current

    // Reset selection when weather data changes
    LaunchedEffect(weatherData) {
        selectedDay = weatherData?.forecast?.forecastDay?.firstOrNull()
        selectedHour = null // Reset hour selection as well
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = city,
            onValueChange = onCityChange,
            label = { Text("Nhập tên thành phố") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) {
            Text("Tìm kiếm")
        }

        Spacer(modifier = Modifier.height(16.dp))

        weatherResult?.let { result ->
            when {
                result.isSuccess -> {
                    if (weatherData != null) {
                        CurrentWeather(weather = weatherData)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Biểu đồ thời tiết (Rain, UV, AQI)
                        com.example.dubaothoitiet.ui.WeatherChartsScreen(
                            weatherData = weatherData,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        CombinedAdviceSection(
                            uiState = combinedState,
                            onGenerateAdvice = {
                                val locationNameEn = removeVietnameseAccents(weatherData.location.name)
                                combinedAdviceViewModel.generateNewAdvice(locationNameEn)
                            },
                            onDismiss = { combinedAdviceViewModel.dismiss() }
                        )
                        
                        // Hiển thị cảnh báo thiên tai nếu có
                        weatherData.alerts?.alert?.let { alerts ->
                            if (alerts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                WeatherAlertsSection(alerts = alerts)
                            }
                        }

                        if (isLoggedIn) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { onTrackLocation(weatherData) }) {
                                Text("Theo dõi vị trí này")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        DailyForecast(
                            forecastDays = weatherData.forecast.forecastDay,
                            selectedDay = selectedDay,
                            onDaySelected = {
                                selectedDay = it
                                selectedHour = null // Reset hour selection when day changes
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HourlyForecast(
                            hours = selectedDay?.hour ?: emptyList(),
                            selectedHour = selectedHour,
                            onHourSelected = { selectedHour = it }
                        )

                        // Show extra details for the selected hour
                        selectedHour?.let {
                            Spacer(modifier = Modifier.height(16.dp))
                            HourlyDetailInfo(hour = it)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        AdditionalWeatherInfo(weather = weatherData)


                    } else {
                        Text("Không có dữ liệu thời tiết")
                    }
                }
                result.isFailure -> {
                    val exception = result.exceptionOrNull()
                    val errorMessage = when (exception) {
                        is HttpException -> {
                            when (exception.code()) {
                                400 -> "Yêu cầu không hợp lệ. Vui lòng kiểm tra lại tên thành phố."
                                404 -> "Không tìm thấy thành phố. Vui lòng thử lại."
                                else -> "Lỗi server: ${exception.code()} - ${exception.message()}"
                            }
                        }
                        is java.io.IOException -> "Lỗi mạng. Vui lòng kiểm tra kết nối internet."
                        else -> "Đã xảy ra lỗi không xác định."
                    }
                    Text(errorMessage, color = Color.Red, textAlign = TextAlign.Center)
                    Log.e("WeatherApp", "Error fetching weather", exception)
                }
            }
        }
    }
}

// ... (Các hàm Composable còn lại: CurrentWeather, DailyForecast, HourlyForecast, ...)
// ... (GIỮ NGUYÊN KHÔNG THAY ĐỔI) ...

@Composable
fun CurrentWeather(weather: WeatherResponse) {
    Text(
        text = weather.location.name,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = weather.location.country,
        fontSize = 18.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "${weather.current.tempC}°C",
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = weather.current.condition.text,
            fontSize = 18.sp
        )
        Image(
            painter = rememberAsyncImagePainter("https:${weather.current.condition.icon}"),
            contentDescription = weather.current.condition.text,
            modifier = Modifier.size(40.dp)
        )
    }
    Text(
        text = "Cập nhật lúc: ${weather.location.localtime}",
        fontSize = 12.sp,
        color = Color.Gray
    )
}

@Composable
fun DailyForecast(
    forecastDays: List<ForecastDay>,
    selectedDay: ForecastDay?,
    onDaySelected: (ForecastDay) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Dự báo 3 ngày", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            forecastDays.forEach { forecastDay ->
                val isSelected = forecastDay == selectedDay
                Card(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .clickable { onDaySelected(forecastDay) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp) // Tăng padding nếu muốn rộng hơn
                            .width(120.dp), // Có thể tăng width nếu cần
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center // Căn giữa nội dung
                    ) {
                        // Ngày tháng
                        Text(
                            text = SimpleDateFormat("EEE, dd/MM", Locale.getDefault()).format(
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(forecastDay.date) ?: Date()
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1 // Đảm bảo chỉ 1 dòng
                        )
                        Spacer(modifier = Modifier.height(4.dp)) // Thêm khoảng cách

                        // Icon thời tiết
                        Image(
                            painter = rememberAsyncImagePainter("https:${forecastDay.day.condition.icon}"),
                            contentDescription = forecastDay.day.condition.text,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp)) // Thêm khoảng cách

                        // Nhiệt độ Max/Min
                        Text(
                            // Làm tròn nhiệt độ cho gọn
                            "${forecastDay.day.maxTempC.roundToInt()}° / ${forecastDay.day.minTempC.roundToInt()}°",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold // Đậm vừa
                        )

                        // --- THÊM TỶ LỆ MƯA ---
                        // Chỉ hiển thị nếu tỷ lệ > 0%
                        if (forecastDay.day.dailyChanceOfRain > 0) {
                            Spacer(modifier = Modifier.height(4.dp)) // Khoảng cách
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center // Căn giữa icon và text
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop, // Icon giọt nước
                                    contentDescription = "Tỷ lệ mưa",
                                    modifier = Modifier.size(12.dp), // Kích thước icon nhỏ
                                    tint = Color(0xFF448AFF) // Màu xanh dương nhạt
                                )
                                Spacer(modifier = Modifier.width(2.dp)) // Khoảng cách nhỏ
                                Text(
                                    text = "${forecastDay.day.dailyChanceOfRain}%",
                                    style = MaterialTheme.typography.labelSmall, // Font nhỏ hơn
                                    color = Color(0xFF448AFF) // Cùng màu với icon
                                )
                            }
                        } else {
                            // Nếu không có mưa, thêm Spacer để giữ chiều cao ổn định (tùy chọn)
                            Spacer(modifier = Modifier.height(18.dp)) // Chiều cao tương đương Row tỷ lệ mưa
                        }
                        // --- KẾT THÚC TỶ LỆ MƯA ---
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyForecast(
    hours: List<Hour>,
    selectedHour: Hour?,
    onHourSelected: (Hour) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Dự báo hàng giờ", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            hours.forEach { hour ->
                val isSelected = hour == selectedHour
                Card(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .clickable { onHourSelected(hour) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(10.dp) // Giảm padding 1 chút
                            .width(75.dp), // Có thể giảm width nếu cần
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Thời gian (HH:mm)
                        Text(
                            text = hour.time.split(" ")[1], // Lấy phần HH:mm
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp // Giảm cỡ chữ
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Icon thời tiết
                        Image(
                            painter = rememberAsyncImagePainter("https:${hour.condition.icon}"),
                            contentDescription = hour.condition.text,
                            modifier = Modifier.size(32.dp) // Giảm kích thước icon
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Nhiệt độ
                        Text(
                            text = "${hour.tempC.roundToInt()}°", // Làm tròn
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp // Giảm cỡ chữ
                        )

                        // --- THÊM TỶ LỆ MƯA ---
                        if (hour.chanceOfRain > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "Tỷ lệ mưa",
                                    modifier = Modifier.size(10.dp), // Icon nhỏ hơn nữa
                                    tint = Color(0xFF448AFF)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${hour.chanceOfRain}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp, // Cỡ chữ nhỏ nhất
                                    color = Color(0xFF448AFF)
                                )
                            }
                        } else {
                            // Spacer giữ chiều cao
                            Spacer(modifier = Modifier.height(16.dp)) // Chiều cao tương đương Row tỷ lệ mưa
                        }
                        // --- KẾT THÚC TỶ LỆ MƯA ---
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyDetailInfo(hour: Hour) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Chi tiết lúc ${hour.time.split(" ")[1]}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InfoItem(value = "${hour.feelslikeC}°C", label = "Cảm giác như", icon = Icons.Default.Thermostat)
                InfoItem(value = "${hour.humidity}%", label = "Độ ẩm", icon = Icons.Default.WaterDrop)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InfoItem(value = "${hour.windKph} km/h", label = "Gió", icon = Icons.Default.WindPower)
                InfoItem(value = hour.uv.toString(), label = "Chỉ số UV", icon = Icons.Default.Visibility)
            }
        }
    }
}


@Composable
fun AdditionalWeatherInfo(weather: WeatherResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Thông tin bổ sung",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InfoItem(value = "${weather.current.feelslikeC}°C", label = "Cảm giác như", icon = Icons.Default.Thermostat)
                InfoItem(value = "${weather.current.humidity}%", label = "Độ ẩm", icon = Icons.Default.WaterDrop)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InfoItem(value = "${weather.current.windKph} km/h", label = "Gió", icon = Icons.Default.WindPower)
                InfoItem(value = "${weather.current.visKm} km", label = "Tầm nhìn", icon = Icons.Default.Visibility)
            }
        }
    }
}

@Composable
fun InfoItem(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun AlertSection(alertsResult: Result<List<ExtremeAlert>>?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("⚠️ Cảnh báo Rủi ro", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))

        when {
            // Đang tải hoặc chưa có kết quả
            alertsResult == null -> {
                // Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                //     CircularProgressIndicator(modifier = Modifier.size(24.dp))
                // }
                // Hoặc không hiển thị gì cả nếu đang tải
            }
            // Tải lỗi
            alertsResult.isFailure -> {
                Text("Không thể tải cảnh báo. Vui lòng kiểm tra mạng.", color = MaterialTheme.colorScheme.error)
            }
            // Tải thành công
            alertsResult.isSuccess -> {
                val alerts = alertsResult.getOrNull()
                if (alerts.isNullOrEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F7))) { // Màu xanh nhạt
                        Text(
                            text = "✅ Không có cảnh báo rủi ro nào được ghi nhận trong 24 giờ qua.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    // Hiển thị danh sách cảnh báo
                    alerts.forEach { alert ->
                        AlertCard(alert = alert)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: ExtremeAlert) {
    // Chọn màu nền dựa trên mức độ nghiêm trọng
    val cardColor = when (alert.severity.uppercase()) {
        "CRITICAL" -> Color(0xFFFDECEA) // Đỏ rất nhạt
        "HIGH" -> Color(0xFFFFF9C4) // Vàng nhạt
        "MEDIUM" -> Color(0xFFE3F2FD) // Xanh dương nhạt
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    // Chọn màu chữ dựa trên mức độ nghiêm trọng
    val textColor = when (alert.severity.uppercase()) {
        "CRITICAL" -> Color(0xFFB71C1C) // Đỏ đậm
        "HIGH" -> Color(0xFFF57F17) // Cam đậm
        "MEDIUM" -> Color(0xFF0D47A1) // Xanh dương đậm
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${alert.impactField}: ${alert.severity}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.forecastDetailsVi,
                style = MaterialTheme.typography.bodyMedium
            )
            // Hiển thị lời khuyên nếu có
            alert.actionableAdviceVi?.let { advice ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "➡️ Khuyến nghị: $advice",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Phân tích lúc: ${alert.analysisTime.replace("T", " ").substringBeforeLast(".")}", // Format lại thời gian chút
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AdviceResultSection(
    adviceState: AdviceState,
    onDismiss: () -> Unit // Hàm để ẩn Card này đi
) {
    // Chỉ hiển thị Card khi State không phải là Idle
    if (adviceState != AdviceState.Idle) {
        // Xác định màu nền và icon dựa trên trạng thái
        val cardColor: Color
        val title: String
        val icon: ImageVector? // Icon (có thể null)
        val message: String?
        val isLoading = adviceState is AdviceState.Loading

        when (adviceState) {
            is AdviceState.Loading -> {
                cardColor = MaterialTheme.colorScheme.surfaceVariant
                title = "Đang lấy lời khuyên từ AI..."
                icon = null // Không cần icon khi loading
                message = null
            }
            is AdviceState.Success -> {
                if (adviceState.response.type == "warning") {
                    cardColor = Color(0xFFFFF9C4) // Vàng nhạt cho warning
                    title = "⚠️ Cảnh báo từ AI"
                    icon = Icons.Default.Warning // Icon cảnh báo
                    message = adviceState.response.messageVi
                } else { // advice
                    cardColor = Color(0xFFE3F2FD) // Xanh nhạt cho advice
                    title = "💡 Lời khuyên từ AI"
                    icon = Icons.Default.Info // Icon thông tin
                    message = adviceState.response.messageVi
                }
            }
            is AdviceState.Error -> {
                cardColor = Color(0xFFFDECEA) // Đỏ nhạt cho error
                title = " Lỗi"
                icon = Icons.Default.Error // Icon lỗi
                message = adviceState.message
            }
            else -> { // Idle (sẽ không hiển thị Card)
                return
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // Đẩy nút "Ẩn" sang phải
                ) {
                    // Tiêu đề và Icon (nếu có)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        icon?.let {
                            Icon(imageVector = it, contentDescription = title, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Nút "Ẩn" (chỉ hiện khi không loading)
                    if (!isLoading) {
                        Text(
                            text = "Ẩn",
                            modifier = Modifier
                                .clickable(onClick = onDismiss)
                                .padding(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nội dung (Loading hoặc Message)
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier=Modifier.size(24.dp))
                    }
                } else {
                    message?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CombinedAdviceSection(
    uiState: CombinedAdviceUiState,
    onGenerateAdvice: () -> Unit,
    onDismiss: () -> Unit // Hàm để ẩn Card kết quả
) {
    // Sử dụng AnimatedVisibility để có hiệu ứng xuất hiện/biến mất (tùy chọn)
    // Hoặc dùng when như cũ nếu không cần hiệu ứng
    when (uiState) {
        CombinedAdviceUiState.Idle -> {
            // Không hiển thị gì ở trạng thái ban đầu hoặc sau khi ẩn
        }
        CombinedAdviceUiState.Loading -> {
            // Hiển thị loading indicator dạng Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(1.dp) // Giảm độ nổi bật khi loading
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Đang xử lý AI, vui lòng chờ...") // Thêm chữ "vui lòng chờ"
                }
            }
        }
        CombinedAdviceUiState.Stale -> {
            // Hiển thị nút bấm
            Button(
                onClick = onGenerateAdvice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💡 Nhận Lời khuyên/Cảnh báo AI")
            }
        }
        is CombinedAdviceUiState.Success -> {
            // Hiển thị kết quả advice/warning
            val adviceData = uiState.adviceData // Lấy dữ liệu từ state
            val cardColor: Color
            val title: String
            val icon: ImageVector?

            if (adviceData.type == "warning") {
                cardColor = Color(0xFFFFF9C4) // Vàng nhạt
                title = "⚠️ Cảnh báo từ AI"
                icon = Icons.Default.Warning
            } else { // advice hoặc trường hợp khác (mặc định là advice)
                cardColor = Color(0xFFE3F2FD) // Xanh nhạt
                title = "💡 Lời khuyên từ AI"
                icon = Icons.Default.Info
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Icon và Tiêu đề
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            icon?.let {
                                Icon(imageVector = it, contentDescription = title, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Nút "Ẩn"
                        Text(
                            text = "Ẩn",
                            modifier = Modifier
                                .clickable(onClick = onDismiss) // Gọi hàm onDismiss khi bấm
                                .padding(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Nội dung lời khuyên/cảnh báo
                    Text(
                        text = adviceData.messageVi ?: "Không có nội dung.", // Xử lý null
                        style = MaterialTheme.typography.bodyMedium
                    )
                    // Hiển thị thời gian tạo
                    adviceData.generatedTime?.let { time ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            // Định dạng lại chuỗi thời gian
                            text = "Cập nhật lúc: ${time.replace("T", " ").substringBeforeLast(".")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
        is CombinedAdviceUiState.Error -> {
            // Hiển thị lỗi dạng Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEA)), // Đỏ nhạt
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // Để thêm nút "Ẩn"
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = "Lỗi", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uiState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f)) // Cho text lỗi co giãn
                    }
                    // Nút "Ẩn" cho cả lỗi
                    Text(
                        text = "Ẩn",
                        modifier = Modifier
                            .clickable(onClick = onDismiss) // Gọi hàm onDismiss khi bấm
                            .padding(start = 8.dp), // Thêm padding trái
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            // Cân nhắc thêm nút "Thử lại" ở đây nếu muốn:
            // Button(onClick = onGenerateAdvice) { Text("Thử lại") }
        }
    }
}

@Composable
fun WeatherAlertsSection(alerts: List<com.example.dubaothoitiet.data.WeatherAlert>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        alerts.forEach { alert ->
            WeatherAlertCard(alert = alert)
        }
    }
}

@Composable
fun WeatherAlertCard(alert: com.example.dubaothoitiet.data.WeatherAlert) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.severity?.lowercase()) {
                "extreme" -> Color(0xFFD32F2F) // Đỏ đậm
                "severe" -> Color(0xFFFF6F00) // Cam đậm
                "moderate" -> Color(0xFFFFA726) // Cam nhạt
                else -> Color(0xFFFFF59D) // Vàng nhạt
            }
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Cảnh báo",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.event,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    alert.severity?.let {
                        Text(
                            text = "Mức độ: $it",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Thu gọn" else "Mở rộng",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Headline
            Text(
                text = alert.headline,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            // Expanded content
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Description
                Text(
                    text = alert.desc,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.95f)
                )
                
                // Instruction
                alert.instruction?.let { instruction ->
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Hướng dẫn:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = instruction,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        }
                    }
                }
                
                // Time info
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    alert.effective?.let {
                        Text(
                            text = "Hiệu lực: ${it.substring(0, 16).replace("T", " ")}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    alert.expires?.let {
                        Text(
                            text = "Hết hạn: ${it.substring(0, 16).replace("T", " ")}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
