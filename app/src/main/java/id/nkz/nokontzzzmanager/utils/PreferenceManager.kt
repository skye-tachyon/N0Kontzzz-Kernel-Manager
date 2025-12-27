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
}
