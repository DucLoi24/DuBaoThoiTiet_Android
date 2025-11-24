package com.example.dubaothoitiet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dubaothoitiet.data.WeatherResponse

enum class ChartTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    RAIN("Lượng mưa", Icons.Default.WaterDrop),
    UV("Chỉ số UV", Icons.Default.WbSunny),
    AQI("Chất lượng KK", Icons.Default.Air)
}

@Composable
fun WeatherChartsScreen(
    weatherData: WeatherResponse?,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ChartTab.RAIN) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Tab selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChartTab.values().forEach { tab ->
                    Button(
                        onClick = { selectedTab = tab },
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == tab) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedTab == tab) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart content
            when (selectedTab) {
                ChartTab.RAIN -> RainChart(weatherData)
                ChartTab.UV -> UVChart(weatherData)
                ChartTab.AQI -> AQIChart(weatherData)
            }
        }
    }
}

@Composable
fun RainChart(weatherData: WeatherResponse?) {
    val hourlyData = remember(weatherData) {
        weatherData?.forecast?.forecastDay?.firstOrNull()?.hour?.take(24) ?: emptyList()
    }
    
    if (hourlyData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Không có dữ liệu lượng mưa")
        }
        return
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Lượng mưa 24 giờ tới",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 8.dp)
        ) {
            val maxPrecip = hourlyData.maxOfOrNull { it.precipMm ?: 0f } ?: 1f
            val barWidth = size.width / hourlyData.size
            val maxBarHeight = size.height - 40.dp.toPx()
            
            hourlyData.forEachIndexed { index, hour ->
                val precipMm = hour.precipMm ?: 0f
                val rainChance = hour.chanceOfRain
                val barHeight = if (maxPrecip > 0) (precipMm / maxPrecip) * maxBarHeight else 0f
                
                val barColor = when {
                    rainChance >= 70 -> Color(0xFF1976D2)
                    rainChance >= 40 -> Color(0xFF42A5F5)
                    else -> Color(0xFF90CAF9)
                }
                
                drawRect(
                    color = barColor,
                    topLeft = Offset(
                        x = index * barWidth + barWidth * 0.2f,
                        y = size.height - barHeight - 30.dp.toPx()
                    ),
                    size = Size(barWidth * 0.6f, barHeight)
                )
                
                if (rainChance > 0) {
                    drawContext.canvas.nativeCanvas.apply {
                        val text = "${rainChance}%"
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#1976D2")
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(
                            text,
                            index * barWidth + barWidth * 0.5f,
                            size.height - barHeight - 35.dp.toPx(),
                            paint
                        )
                    }
                }
                
                if (index % 3 == 0) {
                    val hourStr = hour.time.substring(11, 13)
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(
                            "${hourStr}h",
                            index * barWidth + barWidth * 0.5f,
                            size.height - 5.dp.toPx(),
                            paint
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(color = Color(0xFF1976D2), text = "≥70%")
            LegendItem(color = Color(0xFF42A5F5), text = "40-69%")
            LegendItem(color = Color(0xFF90CAF9), text = "<40%")
        }
    }
}

@Composable
fun UVChart(weatherData: WeatherResponse?) {
    val hourlyData = remember(weatherData) {
        weatherData?.forecast?.forecastDay?.firstOrNull()?.hour?.take(24) ?: emptyList()
    }
    
    if (hourlyData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Không có dữ liệu UV")
        }
        return
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Chỉ số UV 24 giờ tới",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 8.dp)
        ) {
            val maxUV = 14f
            val barWidth = size.width / hourlyData.size
            val maxBarHeight = size.height - 40.dp.toPx()
            
            hourlyData.forEachIndexed { index, hour ->
                val uv = hour.uv.toFloat()
                val barHeight = (uv / maxUV) * maxBarHeight
                
                val barColor = when {
                    uv >= 11 -> Color(0xFF9C27B0)
                    uv >= 8 -> Color(0xFFE91E63)
                    uv >= 6 -> Color(0xFFFF5722)
                    uv >= 3 -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }
                
                drawRect(
                    color = barColor,
                    topLeft = Offset(
                        x = index * barWidth + barWidth * 0.2f,
                        y = size.height - barHeight - 30.dp.toPx()
                    ),
                    size = Size(barWidth * 0.6f, barHeight)
                )
                
                if (uv >= 3) {
                    drawContext.canvas.nativeCanvas.apply {
                        val text = uv.toInt().toString()
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                        drawText(
                            text,
                            index * barWidth + barWidth * 0.5f,
                            size.height - barHeight - 35.dp.toPx(),
                            paint
                        )
                    }
                }
                
                if (index % 3 == 0) {
                    val hourStr = hour.time.substring(11, 13)
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(
                            "${hourStr}h",
                            index * barWidth + barWidth * 0.5f,
                            size.height - 5.dp.toPx(),
                            paint
                        )
                    }
                }
            }
        }
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color(0xFF4CAF50), text = "0-2: Thấp")
                LegendItem(color = Color(0xFFFF9800), text = "3-5: TB")
                LegendItem(color = Color(0xFFFF5722), text = "6-7: Cao")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color(0xFFE91E63), text = "8-10: Rất cao")
                LegendItem(color = Color(0xFF9C27B0), text = "11+: Cực cao")
            }
        }
    }
}

@Composable
fun AQIChart(weatherData: WeatherResponse?) {
    val hourlyData = remember(weatherData) {
        weatherData?.forecast?.forecastDay?.firstOrNull()?.hour?.take(24) ?: emptyList()
    }
    
    if (hourlyData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Không có dữ liệu AQI")
        }
        return
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Chất lượng không khí 24 giờ tới",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 8.dp)
        ) {
            val maxAQI = 300f
            val barWidth = size.width / hourlyData.size
            val maxBarHeight = size.height - 40.dp.toPx()
            
            hourlyData.forEachIndexed { index, hour ->
                val aqi = hour.airQuality?.usEpaIndex?.toFloat() ?: 0f
                val barHeight = (aqi.coerceAtMost(maxAQI) / maxAQI) * maxBarHeight
                
                val barColor = when {
                    aqi >= 301 -> Color(0xFF7E0023)
                    aqi >= 201 -> Color(0xFF8F3F97)
                    aqi >= 151 -> Color(0xFFFF0000)
                    aqi >= 101 -> Color(0xFFFF7E00)
                    aqi >= 51 -> Color(0xFFFFFF00)
                    else -> Color(0xFF00E400)
                }
                
                drawRect(
                    color = barColor,
                    topLeft = Offset(
                        x = index * barWidth + barWidth * 0.2f,
                        y = size.height - barHeight - 30.dp.toPx()
                    ),
                    size = Size(barWidth * 0.6f, barHeight)
                )
                
                if (aqi >= 51) {
                    drawContext.canvas.nativeCanvas.apply {
                        val text = aqi.toInt().toString()
                        val paint = android.graphics.Paint().apply {
                            color = if (aqi >= 151) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                        drawText(
                            text,
                            index * barWidth + barWidth * 0.5f,
                            size.height - barHeight - 35.dp.toPx(),
                            paint
                        )
                    }
                }
                
                if (index % 3 == 0) {
                    val hourStr = hour.time.substring(11, 13)
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(
                            "${hourStr}h",
                            index * barWidth + barWidth * 0.5f,
                            size.height - 5.dp.toPx(),
                            paint
                        )
                    }
                }
            }
        }
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color(0xFF00E400), text = "0-50: Tốt", fontSize = 11.sp)
                LegendItem(color = Color(0xFFFFFF00), text = "51-100: TB", fontSize = 11.sp)
                LegendItem(color = Color(0xFFFF7E00), text = "101-150", fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color(0xFFFF0000), text = "151-200: Xấu", fontSize = 11.sp)
                LegendItem(color = Color(0xFF8F3F97), text = "201-300", fontSize = 11.sp)
                LegendItem(color = Color(0xFF7E0023), text = "301+", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontSize = fontSize
        )
    }
}
