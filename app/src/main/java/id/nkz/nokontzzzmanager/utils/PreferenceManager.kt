package id.nkz.nokontzzzmanager.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import android.os.Build

@Singleton
class PreferenceManager @Inject constructor(
    @field:ApplicationContext private val context: Context,
) {
    companion object {
        private const val KEY_TARGET_GAME_PACKAGES = "target_game_packages"
        private const val KEY_KGSL_SKIP_ZEROING = "kgsl_skip_zeroing"
        private const val KEY_AVOID_DIRTY_PTE = "avoid_dirty_pte"
        private const val KEY_BYPASS_CHARGING = "bypass_charging"
        private const val KEY_FORCE_FAST_CHARGE = "force_fast_charge"
        private const val KEY_BATTERY_MONITOR_ENABLED = "battery_monitor_enabled"

        private const val KEY_CHARGING_CONTROL_ENABLED = "charging_control_enabled"
        private const val KEY_CHARGING_CONTROL_STOP_LEVEL = "charging_control_stop_level"
        private const val KEY_CHARGING_CONTROL_RESUME_LEVEL = "charging_control_resume_level"

        private const val KEY_AUTO_RESET_ON_REBOOT = "auto_reset_on_reboot"
        private const val KEY_AUTO_RESET_ON_CHARGING = "auto_reset_on_charging"
        private const val KEY_AUTO_RESET_AT_LEVEL = "auto_reset_at_level"
        private const val KEY_AUTO_RESET_TARGET_LEVEL = "auto_reset_target_level"

        private const val KEY_MONITOR_AUTO_RESET_ON_REBOOT = "monitor_auto_reset_on_reboot"
        private const val KEY_MONITOR_AUTO_RESET_ON_CHARGING = "monitor_auto_reset_on_charging"
        private const val KEY_MONITOR_AUTO_RESET_AT_LEVEL = "monitor_auto_reset_at_level"
        private const val KEY_MONITOR_AUTO_RESET_TARGET_LEVEL = "monitor_auto_reset_target_level"

        private const val KEY_NOTIFICATION_ICON_STYLE = "notification_icon_style"

        const val ICON_STYLE_BATTERY_PERCENT = 0
        const val ICON_STYLE_APP_LOGO = 1
        const val ICON_STYLE_TRANSPARENT = 2

        private const val KEY_TCP_CONGESTION_ALGORITHM = "tcp_congestion_algorithm"
        private const val KEY_IO_SCHEDULER = "io_scheduler"

        // GPU
        private const val KEY_GPU_GOVERNOR = "gpu_governor"
        private const val KEY_GPU_MIN_FREQ = "gpu_min_freq"
        private const val KEY_GPU_MAX_FREQ = "gpu_max_freq"
        private const val KEY_GPU_POWER_LEVEL = "gpu_power_level"
        private const val KEY_GPU_THROTTLING = "gpu_throttling"

        // RAM
        private const val KEY_ZRAM_DISKSIZE = "zram_disksize"
        private const val KEY_ZRAM_COMPRESSION = "zram_compression"
        private const val KEY_SWAPPINESS = "swappiness"
        private const val KEY_DIRTY_RATIO = "dirty_ratio"
        private const val KEY_DIRTY_BACKGROUND_RATIO = "dirty_background_ratio"
        private const val KEY_DIRTY_WRITEBACK = "dirty_writeback"
        private const val KEY_DIRTY_EXPIRE = "dirty_expire"
        private const val KEY_MIN_FREE_MEMORY = "min_free_memory"

        // CPU Per-cluster
        private const val KEY_CPU_GOV_PREFIX = "cpu_gov_"
        private const val KEY_CPU_MIN_FREQ_PREFIX = "cpu_min_freq_"
        private const val KEY_CPU_MAX_FREQ_PREFIX = "cpu_max_freq_"
    }

    private fun credentialPrefs(): SharedPreferences? {
        return try {
            context.getSharedPreferences("nkm_preferences", Context.MODE_PRIVATE)
        } catch (_: IllegalStateException) {
            null
        }
    }

    private fun deviceProtectedPrefs(): SharedPreferences? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val deviceContext = context.createDeviceProtectedStorageContext()
            deviceContext.getSharedPreferences("nkm_preferences", Context.MODE_PRIVATE)
        } else {
            null
        }
    }

    fun setTargetGamePackages(packages: Set<String>) {
        credentialPrefs()?.edit {
            putStringSet(KEY_TARGET_GAME_PACKAGES, packages)
        }
    }

    fun getTargetGamePackages(): Set<String> {
        credentialPrefs()?.getStringSet(KEY_TARGET_GAME_PACKAGES, null)?.let { return it }
        return deviceProtectedPrefs()?.getStringSet(KEY_TARGET_GAME_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setKgslSkipZeroing(enabled: Boolean) {
        credentialPrefs()?.edit {
            putBoolean(KEY_KGSL_SKIP_ZEROING, enabled)
        }
    }

    fun getKgslSkipZeroing(): Boolean {
        credentialPrefs()?.getBoolean(KEY_KGSL_SKIP_ZEROING, false)?.let { return it }
        return deviceProtectedPrefs()?.getBoolean(KEY_KGSL_SKIP_ZEROING, false) ?: false
    }

    fun setAvoidDirtyPte(enabled: Boolean) {
        credentialPrefs()?.edit {
            putBoolean(KEY_AVOID_DIRTY_PTE, enabled)
        }
    }

    fun getAvoidDirtyPte(): Boolean {
        credentialPrefs()?.getBoolean(KEY_AVOID_DIRTY_PTE, false)?.let { return it }
        return deviceProtectedPrefs()?.getBoolean(KEY_AVOID_DIRTY_PTE, false) ?: false
    }

    fun setBypassCharging(enabled: Boolean) {
        credentialPrefs()?.edit {
            putBoolean(KEY_BYPASS_CHARGING, enabled)
        }
    }

    fun getBypassCharging(): Boolean {
        credentialPrefs()?.getBoolean(KEY_BYPASS_CHARGING, false)?.let { return it }
        return deviceProtectedPrefs()?.getBoolean(KEY_BYPASS_CHARGING, false) ?: false
    }

    fun setForceFastCharge(enabled: Boolean) {
        credentialPrefs()?.edit {
            putBoolean(KEY_FORCE_FAST_CHARGE, enabled)
        }
    }

    fun getForceFastCharge(): Boolean {
        credentialPrefs()?.getBoolean(KEY_FORCE_FAST_CHARGE, false)?.let { return it }
        return deviceProtectedPrefs()?.getBoolean(KEY_FORCE_FAST_CHARGE, false) ?: false
    }

    fun setBatteryMonitorEnabled(enabled: Boolean) {
        credentialPrefs()?.edit {
            putBoolean(KEY_BATTERY_MONITOR_ENABLED, enabled)
        }
        deviceProtectedPrefs()?.edit {
            putBoolean(KEY_BATTERY_MONITOR_ENABLED, enabled)
        }
    }

    fun isBatteryMonitorEnabled(): Boolean {
        val primary = credentialPrefs()?.getBoolean(KEY_BATTERY_MONITOR_ENABLED, false)
        if (primary == true) return true
        val fallback = deviceProtectedPrefs()?.getBoolean(KEY_BATTERY_MONITOR_ENABLED, false)
        return fallback ?: false
    }

    fun setChargingControlEnabled(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_CHARGING_CONTROL_ENABLED, enabled) }
        deviceProtectedPrefs()?.edit { putBoolean(KEY_CHARGING_CONTROL_ENABLED, enabled) }
    }

    fun isChargingControlEnabled(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_CHARGING_CONTROL_ENABLED, false)
            ?: deviceProtectedPrefs()?.getBoolean(KEY_CHARGING_CONTROL_ENABLED, false)
            ?: false
    }

    fun setChargingControlStopLevel(level: Int) {
        credentialPrefs()?.edit { putInt(KEY_CHARGING_CONTROL_STOP_LEVEL, level) }
        deviceProtectedPrefs()?.edit { putInt(KEY_CHARGING_CONTROL_STOP_LEVEL, level) }
    }

    fun getChargingControlStopLevel(): Int {
        return credentialPrefs()?.getInt(KEY_CHARGING_CONTROL_STOP_LEVEL, 80)
            ?: deviceProtectedPrefs()?.getInt(KEY_CHARGING_CONTROL_STOP_LEVEL, 80)
            ?: 80
    }

    fun setChargingControlResumeLevel(level: Int) {
        credentialPrefs()?.edit { putInt(KEY_CHARGING_CONTROL_RESUME_LEVEL, level) }
        deviceProtectedPrefs()?.edit { putInt(KEY_CHARGING_CONTROL_RESUME_LEVEL, level) }
    }

    fun getChargingControlResumeLevel(): Int {
        return credentialPrefs()?.getInt(KEY_CHARGING_CONTROL_RESUME_LEVEL, 70)
            ?: deviceProtectedPrefs()?.getInt(KEY_CHARGING_CONTROL_RESUME_LEVEL, 70)
            ?: 70
    }

    fun setAutoResetOnReboot(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_AUTO_RESET_ON_REBOOT, enabled) }
        deviceProtectedPrefs()?.edit { putBoolean(KEY_AUTO_RESET_ON_REBOOT, enabled) }
    }

    fun isAutoResetOnReboot(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_AUTO_RESET_ON_REBOOT, false)
            ?: deviceProtectedPrefs()?.getBoolean(KEY_AUTO_RESET_ON_REBOOT, false)
            ?: false
    }

    fun setAutoResetOnCharging(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_AUTO_RESET_ON_CHARGING, enabled) }
        deviceProtectedPrefs()?.edit { putBoolean(KEY_AUTO_RESET_ON_CHARGING, enabled) }
    }

    fun isAutoResetOnCharging(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_AUTO_RESET_ON_CHARGING, false)
            ?: deviceProtectedPrefs()?.getBoolean(KEY_AUTO_RESET_ON_CHARGING, false)
            ?: false
    }

    fun setAutoResetAtLevel(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_AUTO_RESET_AT_LEVEL, enabled) }
        deviceProtectedPrefs()?.edit { putBoolean(KEY_AUTO_RESET_AT_LEVEL, enabled) }
    }

    fun isAutoResetAtLevel(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_AUTO_RESET_AT_LEVEL, false)
            ?: deviceProtectedPrefs()?.getBoolean(KEY_AUTO_RESET_AT_LEVEL, false)
            ?: false
    }

    fun setAutoResetTargetLevel(level: Int) {
        credentialPrefs()?.edit { putInt(KEY_AUTO_RESET_TARGET_LEVEL, level) }
        deviceProtectedPrefs()?.edit { putInt(KEY_AUTO_RESET_TARGET_LEVEL, level) }
    }

    fun getAutoResetTargetLevel(): Int {
        return credentialPrefs()?.getInt(KEY_AUTO_RESET_TARGET_LEVEL, 90)
            ?: deviceProtectedPrefs()?.getInt(KEY_AUTO_RESET_TARGET_LEVEL, 90)
            ?: 90
    }

    fun setMonitorAutoResetOnReboot(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_MONITOR_AUTO_RESET_ON_REBOOT, enabled) }
        deviceProtectedPrefs()?.edit { putBoolean(KEY_MONITOR_AUTO_RESET_ON_REBOOT, enabled) }
    }

    fun isMonitorAutoResetOnReboot(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_MONITOR_AUTO_RESET_ON_REBOOT, false)
            ?: deviceProtectedPrefs()?.getBoolean(KEY_MONITOR_AUTO_RESET_ON_REBOOT, false)
            ?: false
    }

    fun setMonitorAutoResetOnCharging(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_MONITOR_AUTO_RESET_ON_CHARGING, enabled) }
        deviceProtectedPrefs()?.edit { putBoolean(KEY_MONITOR_AUTO_RESET_ON_CHARGING, enabled) }
    }

    fun isMonitorAutoResetOnCharging(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_MONITOR_AUTO_RESET_ON_CHARGING, false)
            ?: deviceProtectedPrefs()?.getBoolean(KEY_MONITOR_AUTO_RESET_ON_CHARGING, false)
            ?: false
    }

    fun setMonitorAutoResetAtLevel(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_MONITOR_AUTO_RESET_AT_LEVEL, enabled) }
        deviceProtectedPrefs()?.edit { putBoolean(KEY_MONITOR_AUTO_RESET_AT_LEVEL, enabled) }
    }

    fun isMonitorAutoResetAtLevel(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_MONITOR_AUTO_RESET_AT_LEVEL, false)
            ?: deviceProtectedPrefs()?.getBoolean(KEY_MONITOR_AUTO_RESET_AT_LEVEL, false)
            ?: false
    }

    fun setMonitorAutoResetTargetLevel(level: Int) {
        credentialPrefs()?.edit { putInt(KEY_MONITOR_AUTO_RESET_TARGET_LEVEL, level) }
        deviceProtectedPrefs()?.edit { putInt(KEY_MONITOR_AUTO_RESET_TARGET_LEVEL, level) }
    }

    fun getMonitorAutoResetTargetLevel(): Int {
        return credentialPrefs()?.getInt(KEY_MONITOR_AUTO_RESET_TARGET_LEVEL, 90)
            ?: deviceProtectedPrefs()?.getInt(KEY_MONITOR_AUTO_RESET_TARGET_LEVEL, 90)
            ?: 90
    }

    fun setNotificationIconStyle(style: Int) {
        credentialPrefs()?.edit { putInt(KEY_NOTIFICATION_ICON_STYLE, style) }
        deviceProtectedPrefs()?.edit { putInt(KEY_NOTIFICATION_ICON_STYLE, style) }
    }

    fun getNotificationIconStyle(): Int {
        return credentialPrefs()?.getInt(KEY_NOTIFICATION_ICON_STYLE, ICON_STYLE_APP_LOGO)
            ?: deviceProtectedPrefs()?.getInt(KEY_NOTIFICATION_ICON_STYLE, ICON_STYLE_APP_LOGO)
            ?: ICON_STYLE_APP_LOGO
    }

    fun setTcpCongestionAlgorithm(algorithm: String?) {
        credentialPrefs()?.edit {
            if (algorithm == null) remove(KEY_TCP_CONGESTION_ALGORITHM)
            else putString(KEY_TCP_CONGESTION_ALGORITHM, algorithm)
        }
        deviceProtectedPrefs()?.edit {
            if (algorithm == null) remove(KEY_TCP_CONGESTION_ALGORITHM)
            else putString(KEY_TCP_CONGESTION_ALGORITHM, algorithm)
        }
    }

    fun getTcpCongestionAlgorithm(): String? {
        return credentialPrefs()?.getString(KEY_TCP_CONGESTION_ALGORITHM, null)
            ?: deviceProtectedPrefs()?.getString(KEY_TCP_CONGESTION_ALGORITHM, null)
    }

    fun setIoScheduler(scheduler: String?) {
        credentialPrefs()?.edit {
            if (scheduler == null) remove(KEY_IO_SCHEDULER)
            else putString(KEY_IO_SCHEDULER, scheduler)
        }
        deviceProtectedPrefs()?.edit {
            if (scheduler == null) remove(KEY_IO_SCHEDULER)
            else putString(KEY_IO_SCHEDULER, scheduler)
        }
    }

    fun getIoScheduler(): String? {
        return credentialPrefs()?.getString(KEY_IO_SCHEDULER, null)
            ?: deviceProtectedPrefs()?.getString(KEY_IO_SCHEDULER, null)
    }

    // GPU
    fun setGpuGovernor(governor: String?) {
        credentialPrefs()?.edit {
            if (governor == null) remove(KEY_GPU_GOVERNOR)
            else putString(KEY_GPU_GOVERNOR, governor)
        }
    }

    fun getGpuGovernor(): String? {
        return credentialPrefs()?.getString(KEY_GPU_GOVERNOR, null)
    }

    fun setGpuMinFreq(freq: Int) {
        credentialPrefs()?.edit { putInt(KEY_GPU_MIN_FREQ, freq) }
    }

    fun getGpuMinFreq(): Int {
        return credentialPrefs()?.getInt(KEY_GPU_MIN_FREQ, -1) ?: -1
    }

    fun setGpuMaxFreq(freq: Int) {
        credentialPrefs()?.edit { putInt(KEY_GPU_MAX_FREQ, freq) }
    }

    fun getGpuMaxFreq(): Int {
        return credentialPrefs()?.getInt(KEY_GPU_MAX_FREQ, -1) ?: -1
    }

    fun setGpuPowerLevel(level: Int) {
        credentialPrefs()?.edit { putInt(KEY_GPU_POWER_LEVEL, level) }
    }

    fun getGpuPowerLevel(): Int {
        return credentialPrefs()?.getInt(KEY_GPU_POWER_LEVEL, -1) ?: -1
    }

    fun setGpuThrottling(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_GPU_THROTTLING, enabled) }
    }

    fun getGpuThrottling(): Boolean? {
        if (credentialPrefs()?.contains(KEY_GPU_THROTTLING) == true) {
            return credentialPrefs()?.getBoolean(KEY_GPU_THROTTLING, false)
        }
        return null
    }

    // RAM
    fun setZramDisksize(size: Long) {
        credentialPrefs()?.edit { putLong(KEY_ZRAM_DISKSIZE, size) }
    }

    fun getZramDisksize(): Long {
        return credentialPrefs()?.getLong(KEY_ZRAM_DISKSIZE, -1L) ?: -1L
    }

    fun setZramCompression(algo: String?) {
        credentialPrefs()?.edit {
            if (algo == null) remove(KEY_ZRAM_COMPRESSION)
            else putString(KEY_ZRAM_COMPRESSION, algo)
        }
    }

    fun getZramCompression(): String? {
        return credentialPrefs()?.getString(KEY_ZRAM_COMPRESSION, null)
    }

    fun setSwappiness(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_SWAPPINESS, value) }
    }

    fun getSwappiness(): Int {
        return credentialPrefs()?.getInt(KEY_SWAPPINESS, -1) ?: -1
    }

    fun setDirtyRatio(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_DIRTY_RATIO, value) }
    }

    fun getDirtyRatio(): Int {
        return credentialPrefs()?.getInt(KEY_DIRTY_RATIO, -1) ?: -1
    }

    fun setDirtyBackgroundRatio(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_DIRTY_BACKGROUND_RATIO, value) }
    }

    fun getDirtyBackgroundRatio(): Int {
        return credentialPrefs()?.getInt(KEY_DIRTY_BACKGROUND_RATIO, -1) ?: -1
    }

    fun setDirtyWriteback(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_DIRTY_WRITEBACK, value) }
    }

    fun getDirtyWriteback(): Int {
        return credentialPrefs()?.getInt(KEY_DIRTY_WRITEBACK, -1) ?: -1
    }

    fun setDirtyExpire(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_DIRTY_EXPIRE, value) }
    }

    fun getDirtyExpire(): Int {
        return credentialPrefs()?.getInt(KEY_DIRTY_EXPIRE, -1) ?: -1
    }

    fun setMinFreeMemory(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_MIN_FREE_MEMORY, value) }
    }

    fun getMinFreeMemory(): Int {
        return credentialPrefs()?.getInt(KEY_MIN_FREE_MEMORY, -1) ?: -1
    }

    // CPU Per-cluster
    fun setCpuGov(cluster: String, gov: String) {
        credentialPrefs()?.edit { putString(KEY_CPU_GOV_PREFIX + cluster, gov) }
    }

    fun getCpuGov(cluster: String): String? {
        return credentialPrefs()?.getString(KEY_CPU_GOV_PREFIX + cluster, null)
    }

    fun setCpuMinFreq(cluster: String, freq: Int) {
        credentialPrefs()?.edit { putInt(KEY_CPU_MIN_FREQ_PREFIX + cluster, freq) }
    }

    fun getCpuMinFreq(cluster: String): Int {
        return credentialPrefs()?.getInt(KEY_CPU_MIN_FREQ_PREFIX + cluster, -1) ?: -1
    }

    fun setCpuMaxFreq(cluster: String, freq: Int) {
        credentialPrefs()?.edit { putInt(KEY_CPU_MAX_FREQ_PREFIX + cluster, freq) }
    }

    fun getCpuMaxFreq(cluster: String): Int {
        return credentialPrefs()?.getInt(KEY_CPU_MAX_FREQ_PREFIX + cluster, -1) ?: -1
    }
}
