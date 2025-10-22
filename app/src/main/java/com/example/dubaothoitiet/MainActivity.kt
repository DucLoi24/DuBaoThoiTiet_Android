package com.example.dubaothoitiet

import android.Manifest
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WindPower
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.dubaothoitiet.data.ForecastDay
import com.example.dubaothoitiet.data.Hour
import com.example.dubaothoitiet.data.WeatherResponse
import com.example.dubaothoitiet.ui.theme.DuBaoThoiTietTheme
import com.example.dubaothoitiet.viewmodel.AuthViewModel
import com.example.dubaothoitiet.viewmodel.LocationViewModel
import com.example.dubaothoitiet.viewmodel.UserViewModel
import com.example.dubaothoitiet.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class MainActivity : ComponentActivity() {
    private val weatherViewModel: WeatherViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DuBaoThoiTietTheme {
                WeatherApp(weatherViewModel, authViewModel, userViewModel, locationViewModel)
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
    locationViewModel: LocationViewModel
) {
    val weatherResult by weatherViewModel.weatherResult.observeAsState()
    var city by remember { mutableStateOf("Hanoi") }
    val user by userViewModel.user.observeAsState()
    var showAuthDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dự báo thời tiết") },
                actions = {
                    IconButton(onClick = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Vị trí hiện tại")
                    }
                    IconButton(onClick = {
                        if (user == null) {
                            showAuthDialog = true
                        } else {
                            authViewModel.logout()
                        }
                    }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Tài khoản")
                    }
                }
            )
        }
    ) { paddingValues ->
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
                city = city,
                onCityChange = { city = it },
                onSearch = {
                    val searchCity = removeVietnameseAccents(city)
                    weatherViewModel.getWeather(searchCity)
                },
                onTrackLocation = { weatherResponse: WeatherResponse ->
                    val userId = user?.userId
                    if (userId != null) {
                        locationViewModel.trackLocation(
                            userId = userId,
                            name = weatherResponse.location.name,
                            nameEn = removeVietnameseAccents(weatherResponse.location.name),
                            lat = weatherResponse.location.lat,
                            lon = weatherResponse.location.lon
                        )
                    }
                },
                isLoggedIn = user != null
            )
        }
    }
}


@Composable
fun WeatherScreen(
    weatherResult: Result<WeatherResponse>?,
    city: String,
    onCityChange: (String) -> Unit,
    onSearch: () -> Unit,
    onTrackLocation: (WeatherResponse) -> Unit,
    isLoggedIn: Boolean
) {
    var selectedDay by remember { mutableStateOf<ForecastDay?>(null) }
    var selectedHour by remember { mutableStateOf<Hour?>(null) }
    val weatherData = weatherResult?.getOrNull()

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
        Text("Dự báo 7 ngày", style = MaterialTheme.typography.titleMedium)
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
                            .padding(12.dp)
                            .width(120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = SimpleDateFormat("EEE, dd/MM", Locale.getDefault()).format(
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(forecastDay.date) ?: Date()
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Image(
                            painter = rememberAsyncImagePainter("https:${forecastDay.day.condition.icon}"),
                            contentDescription = forecastDay.day.condition.text,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "${forecastDay.day.maxTempC}° / ${forecastDay.day.minTempC}°",
                             style = MaterialTheme.typography.bodyMedium
                        )
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
                            .padding(12.dp)
                            .width(80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = hour.time.split(" ")[1],
                            fontWeight = FontWeight.Bold
                        )
                        Image(
                            painter = rememberAsyncImagePainter("https:${hour.condition.icon}"),
                            contentDescription = hour.condition.text,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "${hour.tempC}°",
                            fontWeight = FontWeight.Bold
                        )
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
