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
    val isTracking: Boolean = false,
    val isBenchmarking: Boolean = false,
    val benchmarkStartTime: Long = 0L,
    val currentBenchmarkDuration: Long = 0L
)

@Singleton
class FpsMonitorManager @Inject constructor(
    private val rootRepository: RootRepository
) {
    private val _fpsData = MutableStateFlow(FpsData())
    val fpsData: StateFlow<FpsData> = _fpsData

    private var isMonitoring = false
    private var currentPackageName: String? = null
    
    // Benchmarking data
    private var isBenchmarking = false
    private var benchmarkStartTime = 0L
    private val recordedFrameTimes = mutableListOf<Float>()
    private var totalJankCount = 0
    private var totalBigJankCount = 0
    
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
                
                if (isBenchmarking) {
                    _fpsData.value = _fpsData.value.copy(
                        currentBenchmarkDuration = System.currentTimeMillis() - benchmarkStartTime
                    )
                }
                
                delay(1000) // update every second
            }
        }
    }

    fun stopMonitoring() {
        if (isBenchmarking) stopBenchmarking()
        isMonitoring = false
        currentPackageName = null
        _fpsData.value = _fpsData.value.copy(isTracking = false, currentFps = 0f, fps1Low = 0f, fps01Low = 0f)
    }

    fun startBenchmarking() {
        if (isBenchmarking || !isMonitoring) return
        isBenchmarking = true
        benchmarkStartTime = System.currentTimeMillis()
        recordedFrameTimes.clear()
        totalJankCount = 0
        totalBigJankCount = 0
        _fpsData.value = _fpsData.value.copy(
            isBenchmarking = true,
            benchmarkStartTime = benchmarkStartTime,
            currentBenchmarkDuration = 0L
        )
    }

    data class BenchmarkResult(
        val packageName: String,
        val startTime: Long,
        val durationMs: Long,
        val avgFps: Float,
        val fps1Low: Float,
        val fps01Low: Float,
        val jankCount: Int,
        val bigJankCount: Int,
        val frameTimes: List<Float>
    )

    fun stopBenchmarking(): BenchmarkResult? {
        if (!isBenchmarking) return null
        
        val durationMs = System.currentTimeMillis() - benchmarkStartTime
        isBenchmarking = false
        _fpsData.value = _fpsData.value.copy(isBenchmarking = false)
        
        if (recordedFrameTimes.isEmpty()) return null

        val avgFrameTime = recordedFrameTimes.average().toFloat()
        val avgFps = 1000f / avgFrameTime
        
        val sorted = recordedFrameTimes.sortedDescending()
        val p1Index = (sorted.size * 0.01).toInt().coerceAtMost(sorted.size - 1)
        val p01Index = (sorted.size * 0.001).toInt().coerceAtMost(sorted.size - 1)
        
        val fps1Low = 1000f / sorted[p1Index].coerceAtLeast(1f)
        val fps01Low = 1000f / sorted[p01Index].coerceAtLeast(1f)

        return BenchmarkResult(
            packageName = currentPackageName ?: "unknown",
            startTime = benchmarkStartTime,
            durationMs = durationMs,
            avgFps = avgFps.coerceAtMost(refreshRate),
            fps1Low = fps1Low.coerceAtMost(refreshRate),
            fps01Low = fps01Low.coerceAtMost(refreshRate),
            jankCount = totalJankCount,
            bigJankCount = totalBigJankCount,
            frameTimes = recordedFrameTimes.toList()
        )
    }

    private suspend fun getRefreshRate(): Float {
        return try {
            val result = rootRepository.run("dumpsys display | grep -i 'fps=' | grep -E -o '[0-9]+.[0-9]+'")
            result.trim().split("\n").firstOrNull()?.toFloatOrNull() ?: 60f
        } catch (e: Exception) {
            60f
        }
    }

    private suspend fun getActiveLayerName(packageName: String): String? {
        return try {
            // Find SurfaceFlinger layer that matches package name
            val output = rootRepository.run("dumpsys SurfaceFlinger --list")
            
            val lines = output.lines()
            
            // Priority 1: SurfaceView for games (most accurate for FPS)
            lines.firstOrNull { it.contains(packageName) && it.contains("SurfaceView", ignoreCase = true) }
            // Priority 2: BLAST BufferQueue (Android 12+)
            ?: lines.firstOrNull { it.contains(packageName) && it.contains("BLAST", ignoreCase = true) }
            // Priority 3: Any layer containing the package name
            ?: lines.firstOrNull { it.contains(packageName) }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getSurfaceFlingerLatency(layerName: String): String {
        return try {
            rootRepository.run("dumpsys SurfaceFlinger --latency \"$layerName\"")
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
            
            // VRR Compensation: Calculate dynamic refresh rate from SurfaceFlinger's actual refresh period
            val dynamicRefreshRate = if (refreshPeriodNs > 0) {
                1_000_000_000f / refreshPeriodNs.toFloat()
            } else {
                refreshRate // Fallback to baseline
            }
            
            val expectedFrameTimeMs = refreshPeriodNs / 1_000_000f

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
                        if (isBenchmarking) {
                            recordedFrameTimes.add(frameTimeMs)
                        }
                        
                        // Jank is determined based on the dynamic expected frame time
                        if (frameTimeMs > expectedFrameTimeMs * 1.5f) {
                            janks++
                            if (isBenchmarking) totalJankCount++
                            
                            if (frameTimeMs > expectedFrameTimeMs * 3f) {
                                if (isBenchmarking) totalBigJankCount++
                            }
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
                    currentFps = fps.coerceAtMost(dynamicRefreshRate),
                    fps1Low = fps1Low.coerceAtMost(dynamicRefreshRate),
                    fps01Low = fps01Low.coerceAtMost(dynamicRefreshRate),
                    frameTimeMs = frameTimes.average().toFloat(),
                    jankCount = janks
                )
            }
        } catch (e: Exception) {
            Log.e("FpsMonitor", "Error parsing latency data", e)
        }
    }
}
