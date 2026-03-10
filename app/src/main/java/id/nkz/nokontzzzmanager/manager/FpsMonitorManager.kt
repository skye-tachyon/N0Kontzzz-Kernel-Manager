package id.nkz.nokontzzzmanager.manager

import android.util.Log
import id.nkz.nokontzzzmanager.data.repository.RootRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val rootRepository: RootRepository,
    private val systemRepository: id.nkz.nokontzzzmanager.data.repository.SystemRepository
) {
    private val _fpsData = MutableStateFlow(FpsData())
    val fpsData: StateFlow<FpsData> = _fpsData

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null
    private var isMonitoring = false
    private var currentPackageName: String? = null
    
    // Benchmarking data
    private var isBenchmarking = false
    private var benchmarkStartTime = 0L
    private val recordedFrameTimes = mutableListOf<Float>()
    private val recordedCpuUsage = mutableListOf<Float>()
    private val recordedGpuUsage = mutableListOf<Float>()
    private val recordedTemp = mutableListOf<Float>()
    private val recordedCpuTemp = mutableListOf<Float>()
    private val recordedGpuFreq = mutableListOf<Int>()
    private var totalJankCount = 0
    private var totalBigJankCount = 0
    
    // Baseline refresh rate info
    private var refreshRate = 60f

    fun startMonitoring(packageName: String, preferredLayerPattern: String? = null, onLayerFound: (String) -> Unit = {}) {
        if (isMonitoring && currentPackageName == packageName) return
        
        Log.d("FpsMonitor", "Starting monitoring for $packageName (preferred: $preferredLayerPattern)")
        stopMonitoring()
        
        isMonitoring = true
        currentPackageName = packageName
        _fpsData.value = _fpsData.value.copy(isTracking = true)

        monitorJob = scope.launch {
            // Clear old latency data to ensure we get fresh frames
            rootRepository.run("dumpsys SurfaceFlinger --latency-clear")
            // Small delay to allow buffer to start filling
            delay(500)

            refreshRate = getRefreshRate()
            Log.d("FpsMonitor", "Detected base refresh rate: $refreshRate")

            var currentLayer: String? = null
            var candidateIndex = 0
            var allCandidates = listOf<String>()
            var layerValidated = false
            var currentLayerFailureCount = 0
            
            // Secondary job for polling system metrics (CPU, GPU, Temp) during benchmarking
            val metricsJob = launch {
                while (isActive) {
                    if (isBenchmarking) {
                        try {
                            val cpuInfo = systemRepository.getCpuRealtime()
                            val gpuInfo = systemRepository.getGpuRealtime()
                            val batteryInfo = systemRepository.getBatteryInfo()
                            
                            recordedCpuUsage.add(cpuInfo.cpuLoadPercentage ?: 0f)
                            recordedCpuTemp.add(cpuInfo.temp)
                            recordedGpuUsage.add(gpuInfo.usagePercentage ?: 0f)
                            recordedGpuFreq.add(gpuInfo.currentFreq)
                            recordedTemp.add(batteryInfo.temp)
                        } catch (e: Exception) {
                            Log.e("FpsMonitor", "Error polling metrics", e)
                        }
                    }
                    delay(1000)
                }
            }

            while (isMonitoring && isActive) {
                // Refresh candidates if we don't have a working layer
                if (currentLayer == null) {
                    allCandidates = getCandidateLayers(packageName, preferredLayerPattern)
                    candidateIndex = 0
                    if (allCandidates.isNotEmpty()) {
                        currentLayer = cleanLayerName(allCandidates[candidateIndex])
                        Log.d("FpsMonitor", "Trying candidate: $currentLayer")
                    }
                }

                if (currentLayer != null) {
                    val latencyData = getSurfaceFlingerLatency(currentLayer)
                    val success = processLatencyData(latencyData)

                    if (!success) {
                        currentLayerFailureCount++

                        // Patience logic: Give the preferred layer more time to "wake up"
                        val maxRetries = if (preferredLayerPattern != null && currentLayer.contains(preferredLayerPattern)) 5 else 2

                        if (currentLayerFailureCount >= maxRetries) {
                            Log.v("FpsMonitor", "Layer $currentLayer consistently empty, trying next.")
                            candidateIndex++
                            currentLayerFailureCount = 0

                            if (candidateIndex < allCandidates.size) {
                                currentLayer = cleanLayerName(allCandidates[candidateIndex])
                                Log.v("FpsMonitor", "Switching to next candidate: $currentLayer")
                            } else {
                                // Cycled through all, reset to find new ones
                                currentLayer = null
                                delay(1000) 
                            }
                        }
                    } else {
                        currentLayerFailureCount = 0
                        if (!layerValidated) {
                            // Success! Save this pattern
                            val fullRaw = allCandidates[candidateIndex]
                            val pattern = extractPattern(fullRaw)
                            if (pattern != null) {
                                Log.i("FpsMonitor", "Valid rendering layer found: $pattern. Saving for future use.")
                                onLayerFound(pattern)
                                layerValidated = true
                            }
                        }
                    }
                }
 else {
                    Log.w("FpsMonitor", "No valid rendering layers found for $packageName")
                    _fpsData.value = _fpsData.value.copy(currentFps = 0f)
                    delay(2000)
                }
                
                if (isBenchmarking) {
                    _fpsData.value = _fpsData.value.copy(
                        currentBenchmarkDuration = System.currentTimeMillis() - benchmarkStartTime
                    )
                }
                
                delay(1000) // update every second
            }
            metricsJob.cancel()
        }
    }

    fun stopMonitoring() {
        Log.d("FpsMonitor", "Stopping monitoring")
        if (isBenchmarking) stopBenchmarking()
        isMonitoring = false
        monitorJob?.cancel()
        monitorJob = null
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
        val avgCpuUsage: Float,
        val avgGpuUsage: Float,
        val avgTemp: Float,
        val frameTimes: List<Float>,
        val cpuUsageHistory: List<Float>,
        val gpuUsageHistory: List<Float>,
        val tempHistory: List<Float>,
        val cpuTempHistory: List<Float>,
        val gpuFreqHistory: List<Int>
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
            avgCpuUsage = if (recordedCpuUsage.isNotEmpty()) recordedCpuUsage.average().toFloat() else 0f,
            avgGpuUsage = if (recordedGpuUsage.isNotEmpty()) recordedGpuUsage.average().toFloat() else 0f,
            avgTemp = if (recordedTemp.isNotEmpty()) recordedTemp.average().toFloat() else 0f,
            frameTimes = recordedFrameTimes.toList(),
            cpuUsageHistory = recordedCpuUsage.toList(),
            gpuUsageHistory = recordedGpuUsage.toList(),
            tempHistory = recordedTemp.toList(),
            cpuTempHistory = recordedCpuTemp.toList(),
            gpuFreqHistory = recordedGpuFreq.toList()
        )
    }

    private suspend fun getRefreshRate(): Float {
        return try {
            val result = rootRepository.run("dumpsys display | grep -E \"mDefaultMode|refreshRate\" | grep -oE \"[0-9]+\\.[0-9]+\"")
            val fps = result.trim().split("\n").firstOrNull { it.toFloatOrNull() ?: 0f > 10f }?.toFloatOrNull()
            
            if (fps != null && fps < 1000f) {
                fps
            } else {
                60f
            }
        } catch (e: Exception) {
            60f
        }
    }

    private fun extractPattern(rawName: String): String? {
        var cleaned = if (rawName.startsWith("RequestedLayerState{") && rawName.endsWith("}")) {
            rawName.substring("RequestedLayerState{".length, rawName.length - 1)
        } else rawName
        
        // 1. Strip the leading HEX hash if present (e.g., "132fcce SurfaceView...")
        // Usually 7-8 chars followed by a space.
        val firstSpace = cleaned.indexOf(' ')
        if (firstSpace != -1 && firstSpace <= 10) {
            val prefix = cleaned.substring(0, firstSpace)
            // If prefix is alphanumeric and not a known keyword, strip it
            if (prefix.all { it.isLetterOrDigit() }) {
                cleaned = cleaned.substring(firstSpace + 1).trim()
            }
        }

        // 2. Split by # to get the stable part of the name
        val index = cleaned.indexOf('#')
        return if (index != -1) cleaned.substring(0, index).trim() else null
    }

    private suspend fun getCandidateLayers(packageName: String, preferredPattern: String? = null): List<String> {
        return try {
            val output = rootRepository.run("dumpsys SurfaceFlinger --list")
            val lines = output.lines().filter { it.isNotBlank() }
            
            val candidateLayers = lines.filter { it.contains(packageName, ignoreCase = true) }
            
            val excludedKeywords = listOf(
                "Snapshot", "Background for", "ActivityRecord", 
                "ActivityRecordInputSink", "Splash Screen", 
                "animation-leash", "Bounds for"
            )

            val validCandidates = candidateLayers.filter { layer ->
                excludedKeywords.none { keyword -> layer.contains(keyword, ignoreCase = true) }
            }.sortedWith(
                // 1. If we have a preferred pattern, put it at the absolute top
                compareByDescending<String> { preferredPattern != null && it.contains(preferredPattern) }
                // 2. BLAST is usually the primary renderer on Android 12+
                .thenByDescending { it.contains("BLAST", ignoreCase = true) }
                // 3. SurfaceView is standard for games
                .thenByDescending { it.contains("SurfaceView", ignoreCase = true) }
                // 4. Layers with # usually have rendering data
                .thenByDescending { it.contains("#") }
            )

            Log.d("FpsMonitor", "Found ${validCandidates.size} potential rendering layers for $packageName")
            validCandidates
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getActiveLayerName(packageName: String): String? {
        val candidates = getCandidateLayers(packageName)
        return if (candidates.isNotEmpty()) cleanLayerName(candidates.first()) else null
    }

    private fun cleanLayerName(rawName: String): String {
        var name = rawName.trim()
        
        // Handle "RequestedLayerState{Name#ID parentId=...}"
        if (name.startsWith("RequestedLayerState{") && name.endsWith("}")) {
            name = name.substring("RequestedLayerState{".length, name.length - 1)
        }
        
        // Extract only the part before spaces (strips parentId, z-order, etc.)
        val metadataKeywords = listOf("parentId=", "relativeParentId=", "z=", "layerStack=")
        var firstMetadataIndex = name.length
        for (keyword in metadataKeywords) {
            val index = name.indexOf(keyword)
            if (index != -1 && index < firstMetadataIndex) {
                firstMetadataIndex = index
            }
        }
        
        return name.substring(0, firstMetadataIndex).trim()
    }

    private suspend fun getSurfaceFlingerLatency(layerName: String): String {
        return try {
            val data = rootRepository.run("dumpsys SurfaceFlinger --latency \"$layerName\"")
            if (data.isBlank()) {
                Log.w("FpsMonitor", "Empty latency data for layer: $layerName")
            }
            data
        } catch (e: Exception) {
            ""
        }
    }

    private fun processLatencyData(rawData: String): Boolean {
        if (rawData.isBlank()) return false

        val lines = rawData.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return false

        try {
            val firstLine = lines.firstOrNull()
            val refreshPeriodNs = firstLine?.toLongOrNull() ?: 0L
            
            if (refreshPeriodNs <= 0) {
                Log.w("FpsMonitor", "Invalid refresh period in latency data: $firstLine")
                return false
            }

            // VRR Compensation: Calculate dynamic refresh rate from SurfaceFlinger's actual refresh period
            val dynamicRefreshRate = 1_000_000_000f / refreshPeriodNs.toFloat()
            val expectedFrameTimeMs = refreshPeriodNs / 1_000_000f

            val frameIntervals = mutableListOf<Float>()
            var lastEndNs = 0L
            var janks = 0

            // SurfaceFlinger output typically starts with the refresh period, then 128 lines of timestamps
            for (i in 1 until lines.size) {
                val parts = lines[i].trim().split("\\s+".toRegex())
                if (parts.size >= 3) {
                    val start = parts[0].toLongOrNull() ?: 0L
                    val end = parts[2].toLongOrNull() ?: 0L

                    // Ignore pending frames (Long.MAX_VALUE)
                    if (start == 0L || end == 0L || end == Long.MAX_VALUE) continue

                    // Calculate FPS based on interval between consecutive frames
                    if (lastEndNs != 0L) {
                        val intervalMs = (end - lastEndNs) / 1_000_000f
                        if (intervalMs > 0 && intervalMs < 500) { // sanity check
                            frameIntervals.add(intervalMs)
                            if (isBenchmarking) {
                                recordedFrameTimes.add(intervalMs)
                            }
                            
                            // Jank detection based on interval
                            if (intervalMs > expectedFrameTimeMs * 1.5f) {
                                janks++
                                if (isBenchmarking) totalJankCount++
                                
                                if (intervalMs > expectedFrameTimeMs * 3f) {
                                    if (isBenchmarking) totalBigJankCount++
                                }
                            }
                        }
                    }
                    lastEndNs = end
                }
            }

            if (frameIntervals.isNotEmpty()) {
                val avgInterval = frameIntervals.average().toFloat()
                val fps = 1000f / avgInterval
                
                val sorted = frameIntervals.sortedDescending()
                val top1PercentIndex = (sorted.size * 0.01).toInt().coerceAtMost(sorted.size - 1)
                val top01PercentIndex = (sorted.size * 0.001).toInt().coerceAtMost(sorted.size - 1)
                
                val fps1Low = 1000f / sorted[top1PercentIndex].coerceAtLeast(1f)
                val fps01Low = 1000f / sorted[top01PercentIndex].coerceAtLeast(1f)

                _fpsData.value = _fpsData.value.copy(
                    currentFps = fps.coerceAtMost(dynamicRefreshRate + 1f),
                    fps1Low = fps1Low.coerceAtMost(dynamicRefreshRate),
                    fps01Low = fps01Low.coerceAtMost(dynamicRefreshRate),
                    frameTimeMs = avgInterval,
                    jankCount = janks
                )
                return true
            } else {
                Log.v("FpsMonitor", "No valid frames parsed from ${lines.size} lines")
                return false
            }
        } catch (e: Exception) {
            Log.e("FpsMonitor", "Error parsing latency data", e)
            return false
        }
    }
}
