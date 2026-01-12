package id.nkz.nokontzzzmanager.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import id.nkz.nokontzzzmanager.data.repository.TuningRepository
import id.nkz.nokontzzzmanager.data.repository.ThermalRepository
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import android.content.Intent
import androidx.core.content.ContextCompat
import id.nkz.nokontzzzmanager.service.ThermalService

@HiltWorker
class RestoreSettingsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val systemRepository: SystemRepository,
    private val tuningRepository: TuningRepository,
    private val thermalRepository: ThermalRepository,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RestoreSettingsWorker", "Restoring system settings...")

        restoreNetworkAndIoSettings()
        restoreMiscSettings()
        restorePerformanceMode()
        restoreCpuSettings() // New method for per-cluster restore
        restoreGpuSettings()
        restoreRamSettings()
        restoreThermalSettings()

        return Result.success()
    }

    private fun restoreCpuSettings() {
        val clusters = tuningRepository.getClusterLeaders()
        clusters.forEach { cluster ->
            // Restore Governor
            preferenceManager.getCpuGov(cluster)?.let { gov ->
                tuningRepository.setCpuGov(cluster, gov)
                Log.d("RestoreSettingsWorker", "Restored CPU Governor for $cluster: $gov")
            }

            // Restore Frequencies
            val minFreq = preferenceManager.getCpuMinFreq(cluster)
            val maxFreq = preferenceManager.getCpuMaxFreq(cluster)
            if (minFreq != -1 && maxFreq != -1) {
                tuningRepository.setCpuFreq(cluster, minFreq, maxFreq)
                Log.d("RestoreSettingsWorker", "Restored CPU Frequencies for $cluster: $minFreq - $maxFreq")
            }
        }
    }

    private fun restoreNetworkAndIoSettings() {
        // Restore TCP Congestion Algorithm
        val savedTcpAlgo = preferenceManager.getTcpCongestionAlgorithm()
        if (!savedTcpAlgo.isNullOrEmpty()) {
            val success = systemRepository.setTcpCongestionAlgorithm(savedTcpAlgo)
            Log.d("RestoreSettingsWorker", "Restored TCP Congestion to $savedTcpAlgo: $success")
        }

        // Restore I/O Scheduler
        val savedIoScheduler = preferenceManager.getIoScheduler()
        if (!savedIoScheduler.isNullOrEmpty()) {
            val success = systemRepository.setIoScheduler(savedIoScheduler)
            Log.d("RestoreSettingsWorker", "Restored I/O Scheduler to $savedIoScheduler: $success")
        }
    }

    private fun restoreMiscSettings() {
        // Restore Avoid Dirty PTE
        if (preferenceManager.getAvoidDirtyPte()) {
            val success = systemRepository.setAvoidDirtyPte(true)
            Log.d("RestoreSettingsWorker", "Restored Avoid Dirty PTE: $success")
        }

        // Restore KGSL Skip Zeroing
        if (preferenceManager.getKgslSkipZeroing()) {
            val success = systemRepository.setKgslSkipZeroing(true)
            Log.d("RestoreSettingsWorker", "Restored KGSL Skip Zeroing: $success")
        }

        // Restore Bypass Charging
        if (preferenceManager.getBypassCharging()) {
            val success = systemRepository.setBypassCharging(true)
            Log.d("RestoreSettingsWorker", "Restored Bypass Charging: $success")
        }

        // Restore Force Fast Charge
        if (preferenceManager.getForceFastCharge()) {
            val success = systemRepository.setForceFastCharge(true)
            Log.d("RestoreSettingsWorker", "Restored Force Fast Charge: $success")
        }
    }

    private fun restorePerformanceMode() {
        val mode = preferenceManager.getPerformanceMode()
        
        val governor = when (mode) {
            "Performance" -> "performance"
            "Powersave" -> "powersave"
            else -> "schedutil"
        }

        val clusters = tuningRepository.getClusterLeaders()
        var successCount = 0
        
        clusters.forEach { cluster ->
            if (tuningRepository.setCpuGov(cluster, governor)) {
                successCount++
            }
        }
        
        Log.d("RestoreSettingsWorker", "Restored Performance Mode ($mode -> $governor) on $successCount clusters")
    }

    private fun restoreGpuSettings() {
        // Governor
        preferenceManager.getGpuGovernor()?.let { gov ->
            if (tuningRepository.setGpuGov(gov)) {
                Log.d("RestoreSettingsWorker", "Restored GPU Governor: $gov")
            }
        }

        // Frequencies
        val minFreq = preferenceManager.getGpuMinFreq()
        if (minFreq != -1) {
            tuningRepository.setGpuMinFreq(minFreq)
        }
        
        val maxFreq = preferenceManager.getGpuMaxFreq()
        if (maxFreq != -1) {
            tuningRepository.setGpuMaxFreq(maxFreq)
        }

        // Power Level
        val powerLevel = preferenceManager.getGpuPowerLevel()
        if (powerLevel != -1) {
            tuningRepository.setGpuPowerLevel(powerLevel.toFloat())
        }

        // Throttling
        preferenceManager.getGpuThrottling()?.let { enabled ->
            systemRepository.setGpuThrottling(enabled)
        }
    }

    private fun restoreRamSettings() {
        // ZRAM Size
        val zramSize = preferenceManager.getZramDisksize()
        if (zramSize != -1L) {
             tuningRepository.setZramDisksize(zramSize)
        }

        // Compression Algo
        preferenceManager.getZramCompression()?.let { algo ->
            tuningRepository.setCompressionAlgorithm(algo)
        }

        // Swappiness
        val swappiness = preferenceManager.getSwappiness()
        if (swappiness != -1) {
            tuningRepository.setSwappiness(swappiness)
        }

        // Dirty Ratio
        val dirtyRatio = preferenceManager.getDirtyRatio()
        if (dirtyRatio != -1) {
            tuningRepository.setDirtyRatio(dirtyRatio)
        }

        // Dirty Background Ratio
        val dirtyBgRatio = preferenceManager.getDirtyBackgroundRatio()
        if (dirtyBgRatio != -1) {
            tuningRepository.setDirtyBackgroundRatio(dirtyBgRatio)
        }

        // Dirty Writeback
        val dirtyWriteback = preferenceManager.getDirtyWriteback()
        if (dirtyWriteback != -1) {
            tuningRepository.setDirtyWriteback(dirtyWriteback * 100)
        }

        // Dirty Expire
        val dirtyExpire = preferenceManager.getDirtyExpire()
        if (dirtyExpire != -1) {
            tuningRepository.setDirtyExpireCentisecs(dirtyExpire)
        }

        // Min Free Memory
        val minFree = preferenceManager.getMinFreeMemory()
        if (minFree != -1) {
            tuningRepository.setMinFreeMemory(minFree * 1024)
        }
    }

    private suspend fun restoreThermalSettings() {
        val thermalPrefs = applicationContext.getSharedPreferences("thermal_settings_prefs", Context.MODE_PRIVATE)
        val lastSavedIndex = thermalPrefs.getInt("last_applied_thermal_index", -2)

        if (lastSavedIndex != -2) {
             thermalRepository.setThermalModeIndex(lastSavedIndex).collect { success ->
                 if (success) {
                     Log.d("RestoreSettingsWorker", "Restored Thermal Profile Index: $lastSavedIndex")
                     
                     // Handle Dynamic Mode Service
                     if (lastSavedIndex == 10) {
                         val intent = Intent(applicationContext, ThermalService::class.java)
                         intent.putExtra("thermal_mode", lastSavedIndex)
                         try {
                             ContextCompat.startForegroundService(applicationContext, intent)
                         } catch (e: Exception) {
                             Log.e("RestoreSettingsWorker", "Failed to start ThermalService", e)
                         }
                     }
                 }
             }
        }
    }
}
