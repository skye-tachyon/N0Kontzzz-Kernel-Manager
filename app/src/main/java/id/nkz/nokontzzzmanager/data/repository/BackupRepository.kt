package id.nkz.nokontzzzmanager.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import id.nkz.nokontzzzmanager.data.model.*
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val context: Context,
    private val preferenceManager: PreferenceManager,
    private val persistentSettingsManager: PersistentSettingsManager
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun createBackup(
        uri: Uri,
        includeTuning: Boolean,
        includeNetwork: Boolean,
        includeBattery: Boolean,
        includeOther: Boolean
    ): Result<String> {
        return try {
            val tuningSettings = if (includeTuning) {
                val (gov, freq) = persistentSettingsManager.getCpu7Settings()
                TuningSettings(
                    thermalMode = persistentSettingsManager.getLastThermalMode(),
                    cpuGovernor = gov,
                    cpuMaxFreq = freq
                )
            } else null

            val networkSettings = if (includeNetwork) {
                NetworkStorageSettings(
                    tcpCongestion = preferenceManager.getTcpCongestionAlgorithm(),
                    ioScheduler = preferenceManager.getIoScheduler()
                )
            } else null

            val batterySettings = if (includeBattery) {
                BatterySettings(
                    bypassCharging = preferenceManager.getBypassCharging(),
                    forceFastCharge = preferenceManager.getForceFastCharge(),
                    chargingControlEnabled = preferenceManager.isChargingControlEnabled(),
                    stopLevel = preferenceManager.getChargingControlStopLevel(),
                    resumeLevel = preferenceManager.getChargingControlResumeLevel(),
                    batteryMonitorEnabled = preferenceManager.isBatteryMonitorEnabled()
                )
            } else null

            val otherSettings = if (includeOther) {
                OtherSettings(
                    kgslSkipZeroing = preferenceManager.getKgslSkipZeroing(),
                    notificationIconStyle = preferenceManager.getNotificationIconStyle()
                )
            } else null

            val backupData = BackupData(
                tuning = tuningSettings,
                networkStorage = networkSettings,
                battery = batterySettings,
                other = otherSettings
            )

            val jsonString = json.encodeToString(backupData)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: return Result.failure(Exception("Failed to open output stream"))

            Result.success("Backup saved successfully")
        } catch (e: Exception) {
            Log.e("BackupRepository", "Backup failed", e)
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(
        uri: Uri,
        restoreTuning: Boolean,
        restoreNetwork: Boolean,
        restoreBattery: Boolean,
        restoreOther: Boolean
    ): Result<Boolean> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return Result.failure(Exception("Failed to open input stream"))

            val backupData = json.decodeFromString<BackupData>(jsonString)

            if (restoreTuning && backupData.tuning != null) {
                backupData.tuning.thermalMode?.let { persistentSettingsManager.saveThermalMode(it) }
                if (backupData.tuning.cpuGovernor != null && backupData.tuning.cpuMaxFreq != null) {
                    persistentSettingsManager.saveCpu7Settings(backupData.tuning.cpuGovernor, backupData.tuning.cpuMaxFreq)
                }
                persistentSettingsManager.applyLastKnownSettings()
            }

            if (restoreNetwork && backupData.networkStorage != null) {
                backupData.networkStorage.tcpCongestion?.let { preferenceManager.setTcpCongestionAlgorithm(it) }
                backupData.networkStorage.ioScheduler?.let { preferenceManager.setIoScheduler(it) }
            }

            if (restoreBattery && backupData.battery != null) {
                backupData.battery.bypassCharging?.let { preferenceManager.setBypassCharging(it) }
                backupData.battery.forceFastCharge?.let { preferenceManager.setForceFastCharge(it) }
                backupData.battery.chargingControlEnabled?.let { preferenceManager.setChargingControlEnabled(it) }
                backupData.battery.stopLevel?.let { preferenceManager.setChargingControlStopLevel(it) }
                backupData.battery.resumeLevel?.let { preferenceManager.setChargingControlResumeLevel(it) }
                backupData.battery.batteryMonitorEnabled?.let { preferenceManager.setBatteryMonitorEnabled(it) }
            }

            if (restoreOther && backupData.other != null) {
                backupData.other.kgslSkipZeroing?.let { preferenceManager.setKgslSkipZeroing(it) }
                backupData.other.notificationIconStyle?.let { preferenceManager.setNotificationIconStyle(it) }
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e("BackupRepository", "Restore failed", e)
            Result.failure(e)
        }
    }

    suspend fun getBackupPreview(uri: Uri): Result<BackupPreview> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return Result.failure(Exception("Failed to open input stream"))

            val data = json.decodeFromString<BackupData>(jsonString)
            Result.success(
                BackupPreview(
                    hasTuning = data.tuning != null,
                    hasNetwork = data.networkStorage != null,
                    hasBattery = data.battery != null,
                    hasOther = data.other != null,
                    timestamp = data.timestamp
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
