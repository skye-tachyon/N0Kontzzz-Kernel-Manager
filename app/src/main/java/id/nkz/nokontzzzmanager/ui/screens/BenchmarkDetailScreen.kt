package id.nkz.nokontzzzmanager.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.BenchmarkEntity
import id.nkz.nokontzzzmanager.ui.components.SimpleLineChart
import id.nkz.nokontzzzmanager.viewmodel.BenchmarkDetailViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BenchmarkDetailScreen(
    navController: NavController,
    viewModel: BenchmarkDetailViewModel = hiltViewModel()
) {
    val benchmark by viewModel.benchmark.collectAsStateWithLifecycle()
    
    // Use Material 3 Color Roles for dynamic theme support and guaranteed contrast
    val colorFps = MaterialTheme.colorScheme.primary
    val colorLow1 = MaterialTheme.colorScheme.tertiary
    val colorLow01 = MaterialTheme.colorScheme.error
    val colorFrameTime = MaterialTheme.colorScheme.secondary
    val colorCpu = MaterialTheme.colorScheme.secondary
    val colorGpu = MaterialTheme.colorScheme.primary
    val colorTemp = MaterialTheme.colorScheme.error
    val colorCpuTemp = MaterialTheme.colorScheme.tertiary
    val colorGpuFreq = MaterialTheme.colorScheme.secondary

    benchmark?.let { b ->
        val frameIntervals = remember(b.frameTimeDataJson) { decodeJsonList(b.frameTimeDataJson) }
        val fpsOverTime = remember(frameIntervals) { calculateFpsOverTime(frameIntervals) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BenchmarkHeader(b)
            }

            item {
                BenchmarkSummaryCard(b, colorFps, colorLow1, colorLow01)
            }

            // FPS Graph (Aggregated per second for accuracy)
            item {
                ChartCard(
                    title = stringResource(R.string.benchmark_fps_stability),
                    icon = Icons.Default.Speed,
                    data = fpsOverTime,
                    lineColor = colorFps,
                    unit = "FPS",
                    targetValue = 60f
                )
            }

            // Frame Time Graph (Raw intervals to show jitter)
            item {
                ChartCard(
                    title = stringResource(R.string.benchmark_frame_time),
                    icon = Icons.Default.Timeline,
                    data = frameIntervals,
                    lineColor = colorFrameTime,
                    unit = "ms",
                    targetValue = 16.6f
                )
            }

            // CPU Usage Graph
            item {
                ChartCard(
                    title = stringResource(R.string.benchmark_cpu_usage),
                    icon = Icons.Default.Memory,
                    data = decodeJsonList(b.cpuUsageDataJson),
                    lineColor = colorCpu,
                    unit = "%"
                )
            }

            // CPU Temp Graph
            item {
                ChartCard(
                    title = stringResource(R.string.benchmark_cpu_temp),
                    icon = Icons.Default.DeviceThermostat,
                    data = decodeJsonList(b.cpuTempDataJson),
                    lineColor = colorCpuTemp,
                    unit = "°C"
                )
            }

            // GPU Usage Graph
            item {
                ChartCard(
                    title = stringResource(R.string.benchmark_gpu_usage),
                    icon = Icons.Default.GraphicEq,
                    data = decodeJsonList(b.gpuUsageDataJson),
                    lineColor = colorGpu,
                    unit = "%"
                )
            }

            // GPU Freq Graph
            item {
                ChartCard(
                    title = stringResource(R.string.benchmark_gpu_freq),
                    icon = Icons.Default.FlashOn,
                    data = decodeJsonList(b.gpuFreqDataJson),
                    lineColor = colorGpuFreq,
                    unit = "MHz"
                )
            }

            // Temperature Graph
            item {
                ChartCard(
                    title = stringResource(R.string.benchmark_battery_temp),
                    icon = Icons.Default.Thermostat,
                    data = decodeJsonList(b.tempDataJson),
                    lineColor = colorTemp,
                    unit = "°C"
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun BenchmarkHeader(benchmark: BenchmarkEntity) {
    val date = remember(benchmark.timestamp) {
        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(benchmark.timestamp))
    }

    Column {
        Text(
            text = benchmark.appName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BenchmarkSummaryCard(
    benchmark: BenchmarkEntity,
    colorFps: Color,
    colorLow1: Color,
    colorLow01: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(stringResource(R.string.benchmark_avg_fps), "${benchmark.avgFps.toInt()}", colorFps)
                StatItem(stringResource(R.string.benchmark_1low_fps), "${benchmark.fps1Low.toInt()}", colorLow1)
                StatItem(stringResource(R.string.benchmark_01low_fps), "${benchmark.fps01Low.toInt()}", colorLow01)
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            val statValueColor = MaterialTheme.colorScheme.onSurface
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(stringResource(R.string.benchmark_avg_cpu), "${benchmark.avgCpuUsage.toInt()}%", statValueColor)
                StatItem(stringResource(R.string.benchmark_avg_gpu), "${benchmark.avgGpuUsage.toInt()}%", statValueColor)
                StatItem(stringResource(R.string.benchmark_avg_temp), "${String.format("%.1f", benchmark.avgTemp)}°C", statValueColor)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(stringResource(R.string.benchmark_duration), "${benchmark.durationMs / 1000}s", statValueColor)
                StatItem(stringResource(R.string.benchmark_janks), "${benchmark.jankCount}", statValueColor)
                StatItem(stringResource(R.string.benchmark_big_janks), "${benchmark.bigJankCount}", statValueColor)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun ChartCard(
    title: String,
    icon: ImageVector,
    data: List<Float>,
    lineColor: Color,
    unit: String,
    targetValue: Float? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = lineColor, modifier = Modifier.size(20.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                if (data.isNotEmpty()) {
                    Text(
                        text = "Avg: ${data.average().toInt()} $unit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (data.isNotEmpty()) {
                SimpleLineChart(
                    data = data,
                    lineColor = lineColor,
                    fillColor = lineColor.copy(alpha = 0.1f),
                    targetValue = targetValue
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.benchmark_no_data), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

fun decodeJsonList(json: String?): List<Float> {
    return try {
        if (json != null) {
            Json.decodeFromString<List<Float>>(json)
        } else emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Aggregates raw frame intervals into FPS per second.
 * This provides a standard FPS graph that matches the user's perceived average.
 */
fun calculateFpsOverTime(frameIntervals: List<Float>): List<Float> {
    if (frameIntervals.isEmpty()) return emptyList()
    
    val result = mutableListOf<Float>()
    var currentWindowMs = 0f
    var frameCount = 0
    
    for (interval in frameIntervals) {
        currentWindowMs += interval
        frameCount++
        
        if (currentWindowMs >= 1000f) {
            result.add(frameCount.toFloat())
            // Carry over the overflow to the next window for precision
            currentWindowMs -= 1000f
            frameCount = 0
        }
    }
    
    // Add the last partial second if it's significant
    if (frameCount > 0 && currentWindowMs > 200f) {
        result.add(frameCount * (1000f / currentWindowMs))
    }
    
    return result
}
