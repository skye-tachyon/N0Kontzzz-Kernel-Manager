package id.nkz.nokontzzzmanager.manager

import android.util.Log
import id.nkz.nokontzzzmanager.data.repository.RootRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class FpsData(
    val currentFps: Float = 0f,
    val fps1Low: Float = 0f,
    val fps01Low: Float = 0f,
    val frameTimeMs: Float = 0f,
    val jankCount: Int = 0,
    val isTracking: Boolean = false
)

@Singleton
class FpsMonitorManager @Inject constructor(
    private val rootRepository: RootRepository
) {
    private val _fpsData = MutableStateFlow(FpsData())
    val fpsData: StateFlow<FpsData> = _fpsData

    private var isMonitoring = false
    private var currentPackageName: String? = null
    
    // Baseline refresh rate info
    private var refreshRate = 60f

    suspend fun startMonitoring(packageName: String) {
        if (isMonitoring) return
        isMonitoring = true
        currentPackageName = packageName
        _fpsData.value = _fpsData.value.copy(isTracking = true)

        withContext(Dispatchers.IO) {
            refreshRate = getRefreshRate()
            
            while (isMonitoring) {
                val layerName = getActiveLayerName(packageName)
                if (layerName != null) {
                    val latencyData = getSurfaceFlingerLatency(layerName)
                    processLatencyData(latencyData)
                }
                delay(1000) // update every second
            }
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
        currentPackageName = null
        _fpsData.value = _fpsData.value.copy(isTracking = false, currentFps = 0f, fps1Low = 0f, fps01Low = 0f)
    }

    private suspend fun getRefreshRate(): Float {
        return try {
            val result = rootRepository.run("dumpsys display | grep -i 'fps=' | grep -E -o '[0-9]+.[0-9]+'")
            result.getOrNull()?.trim()?.toFloatOrNull() ?: 60f
        } catch (e: Exception) {
            60f
        }
    }

    private suspend fun getActiveLayerName(packageName: String): String? {
        return try {
            // Find SurfaceFlinger layer that matches package name and contains SurfaceView or BLAST
            val result = rootRepository.run("dumpsys SurfaceFlinger --list")
            val output = result.getOrNull() ?: return null
            
            output.lines().firstOrNull { 
                it.contains(packageName) && (it.contains("SurfaceView") || it.contains("BLAST")) 
            } ?: output.lines().firstOrNull { it.contains(packageName) }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getSurfaceFlingerLatency(layerName: String): String {
        return try {
            val result = rootRepository.run("dumpsys SurfaceFlinger --latency \"$layerName\"")
            result.getOrNull() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun processLatencyData(rawData: String) {
        if (rawData.isBlank()) return

        val lines = rawData.lines()
        if (lines.isEmpty()) return

        try {
            val refreshPeriodNs = lines.firstOrNull()?.toLongOrNull() ?: return
            
            val frameTimes = mutableListOf<Float>()
            var janks = 0

            // SurfaceFlinger output typically starts with the refresh period, then 128 lines of timestamps
            for (i in 1 until lines.size) {
                val parts = lines[i].trim().split("\\s+".toRegex())
                if (parts.size >= 3) {
                    val start = parts[0].toLongOrNull() ?: 0L
                    val submit = parts[1].toLongOrNull() ?: 0L
                    val end = parts[2].toLongOrNull() ?: 0L

                    // Ignore pending frames (Long.MAX_VALUE)
                    if (start == 0L || end == 0L || end == Long.MAX_VALUE) continue

                    val frameTimeNs = end - start
                    val frameTimeMs = frameTimeNs / 1_000_000f
                    
                    if (frameTimeMs > 0 && frameTimeMs < 1000) { // sanity check
                        frameTimes.add(frameTimeMs)
                        if (frameTimeMs > (1000f / refreshRate) * 1.5f) {
                            janks++
                        }
                    }
                }
            }

            if (frameTimes.isNotEmpty()) {
                val fps = 1000f / (frameTimes.average().toFloat())
                
                frameTimes.sortDescending()
                val top1PercentIndex = (frameTimes.size * 0.01).toInt().coerceAtMost(frameTimes.size - 1)
                val top01PercentIndex = (frameTimes.size * 0.001).toInt().coerceAtMost(frameTimes.size - 1)
                
                val fps1Low = 1000f / (frameTimes[top1PercentIndex].coerceAtLeast(1f))
                val fps01Low = 1000f / (frameTimes[top01PercentIndex].coerceAtLeast(1f))

                _fpsData.value = _fpsData.value.copy(
                    currentFps = fps.coerceAtMost(refreshRate),
                    fps1Low = fps1Low.coerceAtMost(refreshRate),
                    fps01Low = fps01Low.coerceAtMost(refreshRate),
                    frameTimeMs = frameTimes.average().toFloat(),
                    jankCount = janks
                )
            }
        } catch (e: Exception) {
            Log.e("FpsMonitor", "Error parsing latency data", e)
        }
    }
}
