package id.nkz.nokontzzzmanager.service

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import id.nkz.nokontzzzmanager.data.database.AppProfileEntity
import id.nkz.nokontzzzmanager.data.repository.AppProfileRepository
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

import id.nkz.nokontzzzmanager.data.repository.TuningRepository

import id.nkz.nokontzzzmanager.data.repository.ThermalRepository

@AndroidEntryPoint
class AppMonitorService : Service() {

    @Inject
    lateinit var appProfileRepository: AppProfileRepository

    @Inject
    lateinit var systemRepository: SystemRepository

    @Inject
    lateinit var tuningRepository: TuningRepository

    @Inject
    lateinit var thermalRepository: ThermalRepository

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var gameRepository: id.nkz.nokontzzzmanager.data.repository.GameRepository

    @Inject
    lateinit var fpsMonitorManager: id.nkz.nokontzzzmanager.manager.FpsMonitorManager
    
    // Raw shared prefs removed as we now use PreferenceManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null
    private var lastPackageName: String? = null
    private var isProfileApplied = false

    private val usageStatsManager by lazy {
        getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val powerManager by lazy {
        getSystemService(POWER_SERVICE) as android.os.PowerManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitorJob == null || !monitorJob!!.isActive) {
            startMonitoring()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                if (powerManager.isInteractive) {
                    checkForegroundApp()
                    delay(1000) // Check every 1 second
                } else {
                    // Screen is off, pause monitoring
                    delay(5000)
                }
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
    }

    private suspend fun checkForegroundApp() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 2000 // Look back 2 seconds

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentApp = ""

        // Iterate to find the latest ACTIVITY_RESUMED event
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentApp = event.packageName
            }
        }

        // If no new event found, currentApp will be empty, so we skip
        if (currentApp.isNotEmpty() && currentApp != lastPackageName) {
            lastPackageName = currentApp
            handleAppChange(currentApp)
        }
    }

    private suspend fun handleAppChange(packageName: String) {
        val profile = appProfileRepository.getProfile(packageName)

        if (profile != null && profile.isEnabled) {
            applyProfile(profile)
            isProfileApplied = true
        } else {
            // Only revert if we previously applied a profile
            if (isProfileApplied) {
                applyGlobalSettings()
                isProfileApplied = false
            }
        }

        // Handle FPS Overlay for games
        val game = gameRepository.getGameByPackageName(packageName).first()
        if (game != null && game.isBenchmarkEnabled) {
            startFpsOverlay(packageName)
        } else {
            stopFpsOverlay()
        }
    }

    private fun startFpsOverlay(packageName: String) {
        serviceScope.launch {
            fpsMonitorManager.startMonitoring(packageName)
        }
        val intent = Intent(this, FpsOverlayService::class.java)
        startService(intent)
    }

    private fun stopFpsOverlay() {
        fpsMonitorManager.stopMonitoring()
        val intent = Intent(this, FpsOverlayService::class.java)
        stopService(intent)
    }

    private suspend fun applyProfile(profile: AppProfileEntity) {
        Log.d("AppMonitorService", "Applying profile for ${profile.appName}: ${profile.performanceMode}")
        
        // 1. Performance Mode
        applyPerformanceMode(profile.performanceMode)

        // 2. Misc Settings (KGSL, Bypass, Dirty PTE)
        systemRepository.setKgslSkipZeroing(profile.kgslSkipZeroing)
        systemRepository.setBypassCharging(profile.bypassCharging)
        systemRepository.setAvoidDirtyPte(profile.allowDirtyPte)

        // 3. CPU Tuning
        applyCpuConfig(profile.getCpuConfig())

        // 4. GPU Tuning
        applyGpuConfig(profile.getGpuConfig())

        // 5. Thermal Profile
        if (profile.thermalProfile != null) {
            applyThermalProfile(profile.thermalProfile)
        }
    }

    private suspend fun applyGlobalSettings() {
        Log.d("AppMonitorService", "Reverting to global settings")

        // 1. Performance Mode
        val globalMode = preferenceManager.getPerformanceMode()
        applyPerformanceMode(globalMode)

        // 2. Misc Settings (KGSL, Bypass, Dirty PTE)
        systemRepository.setKgslSkipZeroing(preferenceManager.getKgslSkipZeroing())
        systemRepository.setBypassCharging(preferenceManager.getBypassCharging())
        systemRepository.setAvoidDirtyPte(preferenceManager.getAvoidDirtyPte())

        // 3. Revert CPU Tuning to Global Prefs
        revertCpuConfig()

        // 4. Revert GPU Tuning to Global Prefs
        revertGpuConfig()

        // 5. Revert Thermal Profile
        revertThermalProfile()
    }

    private fun applyThermalProfile(profileIndex: Int) {
        serviceScope.launch {
            thermalRepository.setThermalModeIndex(profileIndex).collect {}
        }
    }

    private fun revertThermalProfile() {
        serviceScope.launch {
            val savedMode = thermalRepository.getSavedThermalMode()
            thermalRepository.setThermalModeIndex(savedMode).collect {}
        }
    }

    private suspend fun applyGpuConfig(config: id.nkz.nokontzzzmanager.data.model.GpuProfileConfig) {
        if (!config.governor.isNullOrBlank()) {
            tuningRepository.setGpuGov(config.governor)
        }
        if (config.minFreq != null) {
            tuningRepository.setGpuMinFreq(config.minFreq)
        }
        if (config.maxFreq != null) {
            tuningRepository.setGpuMaxFreq(config.maxFreq)
        }
        if (config.powerLevel != null) {
            tuningRepository.setGpuPowerLevel(config.powerLevel.toFloat())
        }
        if (config.throttlingEnabled != null) {
            systemRepository.setGpuThrottling(config.throttlingEnabled)
        }
    }

    private suspend fun revertGpuConfig() {
        val globalGov = preferenceManager.getGpuGovernor()
        if (globalGov != null) tuningRepository.setGpuGov(globalGov)

        val globalMin = preferenceManager.getGpuMinFreq()
        val globalMax = preferenceManager.getGpuMaxFreq()

        if (globalMin != -1 || globalMax != -1) {
            if (globalMin != -1) tuningRepository.setGpuMinFreq(globalMin)
            if (globalMax != -1) tuningRepository.setGpuMaxFreq(globalMax)
        } else {
            tuningRepository.resetGpuFreq()
        }

        val globalPwr = preferenceManager.getGpuPowerLevel()
        if (globalPwr != -1) tuningRepository.setGpuPowerLevel(globalPwr.toFloat())

        val globalThrottling = preferenceManager.getGpuThrottling()
        // If null (not set), we might assume default is enabled (true), or just leave it. 
        // Safer to revert to preference if set.
        if (globalThrottling != null) {
            systemRepository.setGpuThrottling(globalThrottling)
        }
    }

    private fun applyCpuConfig(config: id.nkz.nokontzzzmanager.data.model.CpuProfileConfig) {
        serviceScope.launch {
            // Apply Cluster Configs
            config.clusterConfigs.forEach { (cluster, clusterConfig) ->
                if (!clusterConfig.governor.isNullOrBlank()) {
                    tuningRepository.setCpuGov(cluster, clusterConfig.governor)
                }
                
                if (clusterConfig.minFreq != null || clusterConfig.maxFreq != null) {
                    val currentFreqs = tuningRepository.getCpuFreq(cluster).first()
                    val targetMin = clusterConfig.minFreq ?: currentFreqs.first
                    val targetMax = clusterConfig.maxFreq ?: currentFreqs.second
                    tuningRepository.setCpuFreq(cluster, targetMin, targetMax)
                }
            }

            // Apply Core Online Status
            config.coreOnlineStatus.forEach { (coreId, online) ->
                tuningRepository.setCoreOnline(coreId, online)
            }
        }
    }

    private fun revertCpuConfig() {
        serviceScope.launch {
            // Revert clusters to global preferences
            val clusterNodes = tuningRepository.getClusterLeaders()
            clusterNodes.forEach { cluster ->
                val globalGov = preferenceManager.getCpuGov(cluster)
                if (globalGov != null) tuningRepository.setCpuGov(cluster, globalGov)

                val globalMin = preferenceManager.getCpuMinFreq(cluster)
                val globalMax = preferenceManager.getCpuMaxFreq(cluster)
                
                if (globalMin != -1 || globalMax != -1) {
                    val currentFreqs = tuningRepository.getCpuFreq(cluster).first()
                    val targetMin = if (globalMin != -1) globalMin else currentFreqs.first
                    val targetMax = if (globalMax != -1) globalMax else currentFreqs.second
                    tuningRepository.setCpuFreq(cluster, targetMin, targetMax)
                } else {
                    tuningRepository.resetCpuFreq(cluster)
                }
            }

            // Revert cores
            // IMPORTANT: Use getNumberOfCores() to detect all physical cores, 
            // even if they were disabled by a profile.
            val cores = tuningRepository.getNumberOfCores()
            for (i in 0 until cores) {
                val globalOnline = preferenceManager.getCpuCoreOnline(i)
                // If explicit pref exists, use it. Otherwise assume online (true) or just leave it?
                // Safer to default to true if we don't know, to avoid stuck offline cores.
                if (globalOnline != null) {
                    tuningRepository.setCoreOnline(i, globalOnline)
                } else {
                    // If no global pref, ensure it's online to be safe
                    tuningRepository.setCoreOnline(i, true)
                }
            }
        }
    }

    private suspend fun applyPerformanceMode(mode: String) {
        val targetGovernor = when (mode) {
            "Performance" -> "performance"
            "Powersave" -> "powersave"
            else -> "schedutil"
        }
        
        // Dynamically get cluster leaders instead of hardcoding
        val clusterNodes = tuningRepository.getClusterLeaders()
        clusterNodes.forEach { cluster ->
             val available = tuningRepository.getAvailableCpuGovernors(cluster).first()
             if (available.contains(targetGovernor)) {
                 tuningRepository.setCpuGov(cluster, targetGovernor)
             } else if (mode == "Balanced") {
                 // Fallback for Balanced if schedutil is missing
                 when {
                     available.contains("walt") -> tuningRepository.setCpuGov(cluster, "walt")
                     available.contains("interactive") -> tuningRepository.setCpuGov(cluster, "interactive")
                     available.contains("pixutil") -> tuningRepository.setCpuGov(cluster, "pixutil")
                     // If none match, we simply don't touch it, or we could leave it as is.
                     // But we should try to move away from performance/powersave if that was the previous state.
                 }
             }
        }
    }
}
