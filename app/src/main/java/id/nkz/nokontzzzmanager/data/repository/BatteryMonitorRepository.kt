package id.nkz.nokontzzzmanager.data.repository

import android.content.Context
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import id.nkz.nokontzzzmanager.data.model.BatteryMonitorStats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryMonitorRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs by lazy {
        // Must match the name used in BatteryMonitorService
        context.createDeviceProtectedStorageContext().getSharedPreferences("battery_monitor_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Reads the current accumulated stats from BatteryMonitorService's shared preferences.
     * Note: These values are updated every 60 seconds or on power events.
     * Logic mimics BatteryMonitorService's restoration logic to present a consistent view.
     */
    fun getMonitorStats(): BatteryMonitorStats {
        val lastElapsed = prefs.getLong("last_elapsed_ms", 0L)
        val savedAccum = prefs.getLong("screen_accum_ms", 0L)
        val savedWindowStart = prefs.getLong("window_start_elapsed", -1L)
        val savedWindowAccum = prefs.getLong("window_accum_ms", 0L)
        val savedWindowStartUptime = prefs.getLong("window_start_uptime", -1L)

        val savedConsumedOn = prefs.getLong("consumed_on_uah", 0L) // Not used for now, using calculated drops
        val savedConsumedOff = prefs.getLong("consumed_off_uah", 0L)
        
        val savedOnDrop = java.lang.Double.longBitsToDouble(prefs.getLong("on_percent_drop_bits", 0L))
        val savedOffDrop = java.lang.Double.longBitsToDouble(prefs.getLong("off_percent_drop_bits", 0L))

        // We use SystemClock.elapsedRealtime() to estimate the "live" window if possible, 
        // but since we don't know if the service is actually running or if the device rebooted 
        // since last save without us knowing (though lastElapsed helps), we will stick to 
        // the persisted values + extrapolation if reasonable.
        
        val now = SystemClock.elapsedRealtime()
        val nowUptime = SystemClock.uptimeMillis()
        
        // Simple extrapolation: if last save was recent (< 2 mins), assume service is running 
        // and we can extrapolate window duration.
        // However, we don't know the *current* screen state (On/Off) perfectly without a Receiver.
        // So we will just use the "Persisted" values which represent the state as of 'lastElapsed'.
        // If we want to be closer to "Live", we can add (now - lastElapsed) to the Total Window.
        // But we can't easily attribute it to Screen On vs Off without knowing current state.
        
        // Let's stick to the persisted snapshot logic to ensure consistency with what the Service *saved*.
        // The service updates every 1 min, so it's fresh enough.

        // Reconstruct Total Window
        // Logic from Service: val currentSessionWindowMs = if (windowStartElapsed >= 0L) (nowElapsed - windowStartElapsed) else 0L
        // val windowMs = windowAccumMs + currentSessionWindowMs
        
        // To show "Live-ish" data, we calculate the window ending at 'lastElapsed' (which is when the data was saved).
        // If we use 'now', we might overshoot if the service stopped.
        // But if the service is running, 'lastElapsed' is at most 60s ago.
        
        val effectiveNow = if (lastElapsed > 0) lastElapsed else now

        val currentSessionWindowMs = if (savedWindowStart >= 0) (effectiveNow - savedWindowStart).coerceAtLeast(0L) else 0L
        val totalWindowMs = savedWindowAccum + currentSessionWindowMs
        
        // Reconstruct Screen On
        // Service: val currentScreenOnMs = screenOnAccumMs + (screenOnStartAtElapsed?.let { nowElapsed - it } ?: 0L)
        // We DON'T have screenOnStartAtElapsed (memory only).
        // BUT 'screen_accum_ms' in prefs is updated *before* writing.
        // In persistState(): updatedAccum = screenOnAccumMs + (screenOnStartAtElapsed?.let { now - it } ?: 0L)
        // So 'screen_accum_ms' ALREADY includes the time up to 'lastElapsed'.
        val totalScreenOnMs = savedAccum

        val totalScreenOffMs = (totalWindowMs - totalScreenOnMs).coerceAtLeast(0L)

        // Reconstruct Awake/Deep Sleep
        // Service: val awakeMs = if (windowStartUptime >= 0L) (nowUptime - windowStartUptime) else 0L
        // We need 'windowStartUptime' relative to 'lastElapsed'.
        // Wait, uptime and elapsed drift. 
        // We can just rely on the fact that awake + deep_sleep = screen_off_time + screen_on_time ?? No.
        // Service: 
        // val awakeMs = if (windowStartUptime >= 0L) (nowUptime - windowStartUptime) else 0L (This is session awake)
        // Service DOES NOT persist 'accumulated awake'. It persists 'window_accum_ms'.
        // Actually, looking at Service code:
        // Service DOES NOT persist 'awake' or 'deep sleep' accumulators! 
        // It calculates them from: `val awakeMs = if (windowStartUptime >= 0L) (nowUptime - windowStartUptime) else 0L`
        // AND `val deepSleepMs = (windowMs - awakeMs).coerceAtLeast(0L)` ?? 
        // Wait, `windowMs` (Total elapsed) - `awakeMs` (Total Uptime)? 
        // Yes, typically Deep Sleep = Realtime - Uptime.
        // But `awakeMs` in Service is just `nowUptime - windowStartUptime`.
        // If there was a previous session (`windowAccumMs`), does it include deep sleep?
        // Service logic:
        // `val windowMs = windowAccumMs + currentSessionWindowMs`
        // `val awakeMs = if (windowStartUptime >= 0L) (nowUptime - windowStartUptime) else 0L`
        // THIS LOOKS WRONG in Service if `windowAccumMs` > 0 (rebooted). 
        // `awakeMs` only counts CURRENT session. `windowMs` counts TOTAL.
        // So `deepSleepMs` = TOTAL - CURRENT_AWAKE. 
        // If `windowAccumMs` is large (previous session), it all gets counted as Deep Sleep?
        // That seems like a bug in the Service if `windowAccumMs` persists across reboots but `awakeMs` accumulator is missing.
        // Let's check Service `restoreStateIfAny`.
        // It does NOT restore any "awake" accumulator.
        // So `awakeMs` starts at 0 after reboot.
        // But `windowAccumMs` starts at `savedWindowAccum` (from prev session).
        // So `deepSleepMs` = `(saved + current) - current_uptime`.
        // This effectively treats ALL previous session time as Deep Sleep?
        // OR `windowAccumMs` is not what I think it is.
        // Service: `val windowMs = windowAccumMs + currentSessionWindowMs`.
        // Yes, it seems so. 
        // If `BatteryMonitorService` has this logic, I must replicate it to match the notification, 
        // even if it seems slightly off across reboots.
        
        // Calculate Uptime (Awake) for current session ending at 'lastElapsed'
        // We need 'lastUptime' corresponding to 'lastElapsed'.
        // We don't have it saved. But we have 'window_start_uptime'.
        // If we assume the device didn't reboot between 'window_start_elapsed' and 'lastElapsed' (which is the definition of a session),
        // Then `deltaElapsed = lastElapsed - window_start_elapsed`
        // `deltaUptime` should be similar IF no deep sleep.
        // We can't know deep sleep duration without Uptime.
        // Wait, `window_start_uptime` is the Uptime at the start of the session.
        // The service calculates `awakeMs` using `nowUptime - windowStartUptime`.
        // But `nowUptime` is not saved.
        // However, `lastElapsed` is saved.
        // We can approximate `currentSessionUptime`? No.
        // We are stuck here. The Service creates the notification with *live* uptime.
        // It doesn't persist the *uptime* at the moment of save.
        // BUT, Deep Sleep = (Realtime - Uptime).
        // Total Deep Sleep = Total Realtime - Total Uptime.
        // If I want to calculate Total Deep Sleep, I need Total Uptime.
        // I can calculate Total Deep Sleep if I know Total Uptime.
        // Since I can't know Total Uptime from prefs (missing `uptime_accum` or `last_uptime`), 
        // I cannot perfectly replicate "Deep Sleep" from prefs alone if across reboots.
        // However, if `windowAccumMs` is 0 (fresh boot), then `window_start_uptime` is valid for current session.
        // `uptime` now = `SystemClock.uptimeMillis()`.
        // `awake` = `uptime - window_start_uptime`.
        // `deepSleep` = `(now - window_start_elapsed) - (uptime - window_start_uptime)`.
        
        // So I will calculate the values *as of now*, assuming the service is running (or stopped recently).
        // Use `SystemClock.uptimeMillis()` and `SystemClock.elapsedRealtime()`.
        
        // Drain Rates
        val onHours = totalScreenOnMs / 3_600_000.0
        val offHours = totalScreenOffMs / 3_600_000.0
        
        val activeRate = if (onHours > 0) (savedOnDrop / onHours).toFloat() else 0f
        val idleRate = if (offHours > 0) (savedOffDrop / offHours).toFloat() else 0f
        
        // Recalculate Awake/Deep Sleep based on current device state
        // If `window_start_uptime` is -1, we have no session.
        val sessionAwakeMs = if (savedWindowStartUptime >= 0) (nowUptime - savedWindowStartUptime).coerceAtLeast(0L) else 0L
        val sessionWindowMs = if (savedWindowStart >= 0) (now - savedWindowStart).coerceAtLeast(0L) else 0L
        
        // We ignore `windowAccumMs` for Awake calculation because we can't reconstruct previous session's awake time.
        // If the service has the "Deep Sleep = Total - Awake" bug, we replicate it?
        // If `windowAccumMs` > 0, the service notification shows `deepSleep = (windowAccum + sessionWindow) - sessionAwake`.
        // Yes, I will replicate exactly that.
        
        val totalWindowLive = savedWindowAccum + sessionWindowMs
        val totalAwakeLive = sessionAwakeMs // This ignores previous session's awake time, just like Service seems to.
        val totalDeepSleepLive = (totalWindowLive - totalAwakeLive).coerceAtLeast(0L)
        
        return BatteryMonitorStats(
            screenOnMs = totalScreenOnMs, // This is "at last save". We could add (now - lastElapsed) if we knew screen was on. Safe to use stored.
            screenOffMs = totalScreenOffMs, // Derived from window at last save.
            awakeMs = totalAwakeLive, // Calculated live
            deepSleepMs = totalDeepSleepLive, // Calculated live
            activeDrainRate = activeRate,
            idleDrainRate = idleRate,
            activeDrainPct = savedOnDrop.toFloat(),
            idleDrainPct = savedOffDrop.toFloat(),
            lastUpdateTime = lastElapsed
        )
    }
}
