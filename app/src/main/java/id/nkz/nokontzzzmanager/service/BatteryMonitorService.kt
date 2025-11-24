package id.nkz.nokontzzzmanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

class BatteryMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private var batteryManager: BatteryManager? = null
    private var serviceStartedAtMs: Long = 0L

    private val channelId = "battery_monitor_channel"
    private val notificationId = 1001

    private var lastUpdate = System.currentTimeMillis()
    private var lastCurrent = 0f
    private var designCapacityUah = 0L // autodetected; no hardcoded fallback
    private var realtimeUsedMah = 0.0
    // Legacy baseline fields (no longer used for drain; kept previously for transitions) removed
    // Cumulative consumption tracking
    private var lastSampleElapsedMs: Long = 0L
    private var lastSampleChargeUah: Long = 0L
    private var consumedOnUah: Long = 0L
    private var consumedOffUah: Long = 0L
    private var prevInteractiveForSample: Boolean? = null
    private var lastSamplePercent: Double = Double.NaN
    private var onPercentDrop: Double = 0.0
    private var offPercentDrop: Double = 0.0

    private var isRunning = false

    // Screen on-time tracking
    private var screenReceiver: BroadcastReceiver? = null
    private var screenOnAccumMs: Long = 0L
    private var screenOnStartAtElapsed: Long? = null
    private var powerReceiver: BroadcastReceiver? = null
    private var windowStartElapsed: Long = -1L
    private var windowStartUptime: Long = -1L
    @Volatile private var nextDelayOverrideMs: Long? = null

    // Persistence
    private val prefs by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isDeviceProtectedStorage) {
            createDeviceProtectedStorageContext().getSharedPreferences("battery_monitor_prefs", Context.MODE_PRIVATE)
        } else {
            getSharedPreferences("battery_monitor_prefs", Context.MODE_PRIVATE)
        }
    }
    private val keyScreenAccum = "screen_accum_ms"
    private val keyLastElapsed = "last_elapsed_ms"

    override fun onCreate() {
        super.onCreate()
        serviceStartedAtMs = System.currentTimeMillis()
        batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        val initialNotification = createNotification("Starting...", "Collecting battery data...")
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(notificationId, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(notificationId, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(notificationId, initialNotification)
            }
        } catch (e: Exception) {
            // Prevent crash if FGS is blocked
            stopSelf()
        }
        designCapacityUah = getDesignCapacityUah()
        restoreStateIfAny()
        initScreenTracking()
        initPowerTracking()
        startMonitoring()
    }

    private fun computeCurrentBatteryPercent(stickyLevel: Int, chargeUah: Long): Double {
        // Prefer OS-reported percentage to avoid noisy coulomb counter coupling to current draw
        if (stickyLevel >= 0) return stickyLevel.toDouble()
        if (chargeUah > 0L && designCapacityUah > 0L) {
            return (chargeUah.toDouble() * 100.0) / designCapacityUah.toDouble()
        }
        return 0.0
    }

    private fun formatPercent(percent: Double, hasData: Boolean): String {
        if (!hasData) return String.format(Locale.US, "%.1f", 0.0)
        return if (percent >= 99.995) String.format(Locale.US, "%.1f", 100.0)
        else String.format(Locale.US, "%.2f", percent)
    }

    private fun readFirstLong(paths: List<String>): Long? {
        for (p in paths) {
            try {
                val s = File(p).readText().trim()
                if (s.isNotEmpty()) {
                    val v = s.toLong()
                    return v
                }
            } catch (_: Exception) { }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        isRunning = false
        try {
            screenReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) { }
        try {
            powerReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) { }
        persistState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESET) {
            try {
                manualReset()
                val stats = collectSystemStats()
                updateNotification(stats)
            } catch (_: Exception) { }
        }
        return START_STICKY
    }

    // === MAIN LOOP ===
    private fun startMonitoring() {
        if (isRunning) return
        isRunning = true

        scope.launch {
            while (isRunning) {
                val stats = collectSystemStats()
                updateNotification(stats)
                val d = nextDelayOverrideMs ?: 5_000L
                nextDelayOverrideMs = null
                delay(d)
            }
        }
    }

    // === BATTERY LOGIC ===
    private fun collectSystemStats(): BatteryData {
        val bm = batteryManager ?: return BatteryData()

        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged > 0

        // Prefer sysfs for current/voltage; fallback to BatteryManager
        val currentUaSys = readFirstLong(listOf(
            "/sys/class/power_supply/battery/current_now",
            "/sys/class/power_supply/battery/Battery_Current",
            "/sys/class/power_supply/battery/batt_current_now"
        ))
        val voltageUvSys = readFirstLong(listOf(
            "/sys/class/power_supply/battery/voltage_now",
            "/sys/class/power_supply/battery/batt_voltage_now"
        ))

        val currentUa = currentUaSys ?: bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toLong()
        val voltageV = (voltageUvSys?.toDouble()?.div(1_000_000.0))
            ?: ((intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0).toDouble() / 1000.0)
        val powerUwSys = readFirstLong(listOf(
            "/sys/class/power_supply/battery/power_avg",
            "/sys/class/power_supply/battery/power_now"
        ))

        // Map sign so: charging -> positive mA, discharging -> negative mA
        val rawMa = currentUa.toFloat() / 1000f
        var currentMa = rawMa
        if (charging && rawMa < 0f) currentMa = -rawMa
        if (!charging && rawMa > 0f) currentMa = -rawMa

        if (lastCurrent != 0f)
            currentMa = (currentMa * 0.6f) + (lastCurrent * 0.4f)
        lastCurrent = currentMa

        val computedWatt = if (voltageV != null && !currentMa.isNaN()) {
            kotlin.math.abs((voltageV * (currentMa.toDouble() / 1000.0)).toFloat())
        } else 0f
        val powerWatt = when {
            powerUwSys != null && powerUwSys != 0L -> kotlin.math.abs(powerUwSys.toFloat() / 1_000_000f)
            else -> computedWatt
        }

        updateRealtimeUsed(currentMa)

        val nowElapsed = SystemClock.elapsedRealtime()
        val nowUptime = SystemClock.uptimeMillis()

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val interactive = pm.isInteractive

        val currentCharge = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        // Attribute consumption since last sample to previous interactive state
        if (lastSampleElapsedMs != 0L) {
            val dt = nowElapsed - lastSampleElapsedMs
            if (dt > 0) {
                val deltaUah = (lastSampleChargeUah - currentCharge).coerceAtLeast(0L)
                val prev = prevInteractiveForSample ?: interactive
                if (prev) consumedOnUah += deltaUah else consumedOffUah += deltaUah
                // Also accumulate percent drop
                val currentPercent = computeCurrentBatteryPercent(level, currentCharge)
                if (!lastSamplePercent.isNaN()) {
                    val dPct = (lastSamplePercent - currentPercent).coerceAtLeast(0.0)
                    if (prev) onPercentDrop += dPct else offPercentDrop += dPct
                }
                lastSamplePercent = currentPercent
            }
        }
        lastSampleElapsedMs = nowElapsed
        lastSampleChargeUah = currentCharge
        prevInteractiveForSample = interactive
        if (lastSamplePercent.isNaN()) {
            lastSamplePercent = computeCurrentBatteryPercent(level, currentCharge)
        }

        // Screen state transitions are handled via prevInteractiveForSample for attribution; no baseline resets needed

        // Drain rates based on cumulative consumption and cumulative time in each state (since unplug window)
        // Durations are computed later as currentScreenOnMs and screenOffMs; compute strings after those values are ready
        // Charging behavior: reset and pause counting until unplugged
        if (charging) {
            screenOnAccumMs = 0L
            screenOnStartAtElapsed = null
            windowStartElapsed = -1L
            windowStartUptime = -1L
            // Reset cumulative consumption while charging
            consumedOnUah = 0L
            consumedOffUah = 0L
            onPercentDrop = 0.0
            offPercentDrop = 0.0
            lastSampleElapsedMs = nowElapsed
            lastSampleChargeUah = currentCharge
            prevInteractiveForSample = interactive
            lastSamplePercent = computeCurrentBatteryPercent(level, currentCharge)
        } else if (windowStartElapsed < 0L || windowStartUptime < 0L) {
            // start window on first non-charging tick
            windowStartElapsed = nowElapsed
            windowStartUptime = nowUptime
            // if screen currently on, start tracking from now
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            screenOnStartAtElapsed = if (pm.isInteractive) nowElapsed else null
            // also initialize sampling baselines
            lastSampleElapsedMs = nowElapsed
            lastSampleChargeUah = currentCharge
            prevInteractiveForSample = interactive
            lastSamplePercent = computeCurrentBatteryPercent(level, currentCharge)
        }

        val currentScreenOnMs = screenOnAccumMs + (screenOnStartAtElapsed?.let { nowElapsed - it } ?: 0L)
        val windowMs = if (windowStartElapsed >= 0L) (nowElapsed - windowStartElapsed) else 0L
        val screenOffMs = (windowMs - currentScreenOnMs).coerceAtLeast(0L)

        val awakeMs = if (windowStartUptime >= 0L) (nowUptime - windowStartUptime) else 0L
        val deepSleepMs = (windowMs - awakeMs).coerceAtLeast(0L)
        val awakeOffMs = (awakeMs - currentScreenOnMs).coerceAtLeast(0L)
        // Deep Sleep % of screen-off time, gunakan ms agar akurat; hindari lonjakan jika total < 1s
        val offTotalMsForPct = deepSleepMs + awakeOffMs
        val hasOffWindow = offTotalMsForPct >= 1000L
        val deepSleepPct = if (hasOffWindow) (deepSleepMs * 100.0 / offTotalMsForPct) else 0.0
        val deepSleepPctStr = formatPercent(deepSleepPct, hasOffWindow)
        val deepSleepText = String.format(Locale.US, "%s (%s%%)", formatDurationAdaptive(deepSleepMs), deepSleepPctStr)
        val awakePct = if (hasOffWindow) (awakeOffMs * 100.0 / offTotalMsForPct) else 0.0
        val awakePctStr = formatPercent(awakePct, hasOffWindow)
        val awakeText = String.format(Locale.US, "%s (%s%%)", formatDurationAdaptive(awakeOffMs), awakePctStr)

        // Compute drain strings from cumulative consumption/time
        val onHours = currentScreenOnMs / 3_600_000.0
        val offHours = screenOffMs / 3_600_000.0
        val activeRate = if (onHours > 0) (onPercentDrop / onHours) else 0.0
        val idleRate = if (offHours > 0) (offPercentDrop / offHours) else 0.0
        val activeDrainStr = if (onPercentDrop <= 0.0 || onHours <= 0.0) {
            "0% /hr"
        } else {
            String.format(Locale.US, "%.2f%% /hr", activeRate)
        }
        val idleDrainStr = if (offPercentDrop <= 0.0 || offHours <= 0.0) {
            "0% /hr"
        } else {
            String.format(Locale.US, "%.2f%% /hr", idleRate)
        }

        return BatteryData(
            level = level,
            temperatureC = temp,
            statusText = getChargingStatus(status, plugged),
            currentMa = currentMa,
            powerWatt = powerWatt,
            activeDrain = activeDrainStr,
            idleDrain = idleDrainStr,
            screenOnTime = formatDurationAdaptive(currentScreenOnMs),
            elapsed = formatDurationAdaptive(screenOffMs),
            uptime = awakeText,
            deepSleep = deepSleepText
        )
    }

    private fun initScreenTracking() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isInteractive) {
            screenOnStartAtElapsed = SystemClock.elapsedRealtime()
            // Instant refresh when service starts and screen is already on
            try {
                scope.launch {
                    val stats = collectSystemStats()
                    updateNotification(stats)
                }
                nextDelayOverrideMs = 5_000L
            } catch (_: Exception) { }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        if (screenOnStartAtElapsed == null) {
                            screenOnStartAtElapsed = SystemClock.elapsedRealtime()
                        }
                        // Instant refresh on screen ON
                        try {
                            scope.launch {
                                val stats = collectSystemStats()
                                updateNotification(stats)
                            }
                            nextDelayOverrideMs = 5_000L
                        } catch (_: Exception) { }
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        val start = screenOnStartAtElapsed
                        if (start != null) {
                            val now = SystemClock.elapsedRealtime()
                            screenOnAccumMs += (now - start)
                            screenOnStartAtElapsed = null
                        }
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        // User unlocked: ensure latest numbers shown immediately
                        try {
                            scope.launch {
                                val stats = collectSystemStats()
                                updateNotification(stats)
                            }
                            nextDelayOverrideMs = 5_000L
                        } catch (_: Exception) { }
                    }
                }
            }
        }
        screenReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(receiver, filter)
    }

    private fun initPowerTracking() {
        // Set initial window depending on current charging state
        val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged > 0
        if (isCharging) {
            onPowerConnected()
        } else {
            onPowerDisconnected()
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED -> onPowerConnected()
                    Intent.ACTION_POWER_DISCONNECTED -> onPowerDisconnected()
                }
            }
        }
        powerReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(receiver, filter)
    }

    private fun onPowerConnected() {
        screenOnAccumMs = 0L
        screenOnStartAtElapsed = null
        windowStartElapsed = -1L
        windowStartUptime = -1L
        prevInteractiveForSample = null
        consumedOnUah = 0L
        consumedOffUah = 0L
        lastSampleElapsedMs = 0L
        lastSampleChargeUah = 0L
        prevInteractiveForSample = null
        lastSamplePercent = Double.NaN
        onPercentDrop = 0.0
        offPercentDrop = 0.0
        persistState()
        // Tampilkan status charging seketika tanpa menunggu interval berikutnya
        try {
            lastCurrent = 0f
            scope.launch {
                val stats = collectSystemStats()
                updateNotification(stats)
            }
            nextDelayOverrideMs = 5_000L
        } catch (_: Exception) { }
    }

    private fun onPowerDisconnected() {
        val nowEl = SystemClock.elapsedRealtime()
        val nowUp = SystemClock.uptimeMillis()
        windowStartElapsed = nowEl
        windowStartUptime = nowUp
        screenOnAccumMs = 0L
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        screenOnStartAtElapsed = if (pm.isInteractive) nowEl else null
        // Reset cumulative consumption and set sampling baseline
        consumedOnUah = 0L
        consumedOffUah = 0L
        val bm = batteryManager
        val charge = bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0L
        lastSampleElapsedMs = nowEl
        lastSampleChargeUah = charge
        prevInteractiveForSample = pm.isInteractive
        val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        lastSamplePercent = computeCurrentBatteryPercent(level, charge)
        persistState()
        // Instant refresh to reflect discharging state without waiting for the next tick
        try {
            scope.launch {
                val stats = collectSystemStats()
                updateNotification(stats)
            }
            nextDelayOverrideMs = 5_000L
        } catch (_: Exception) { }
    }

    private fun manualReset() {
        val nowEl = SystemClock.elapsedRealtime()
        val nowUp = SystemClock.uptimeMillis()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        windowStartElapsed = nowEl
        windowStartUptime = nowUp
        screenOnAccumMs = 0L
        screenOnStartAtElapsed = if (pm.isInteractive) nowEl else null
        consumedOnUah = 0L
        consumedOffUah = 0L
        onPercentDrop = 0.0
        offPercentDrop = 0.0
        val charge = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0L
        lastSampleElapsedMs = nowEl
        lastSampleChargeUah = charge
        prevInteractiveForSample = pm.isInteractive
        val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        lastSamplePercent = computeCurrentBatteryPercent(level, charge)
        persistState()
    }

    private fun formatDrainFrom(
        startTimeMs: Long,
        startChargeUah: Long,
        startLevel: Int,
        nowElapsed: Long,
        currentChargeUah: Long,
        currentLevel: Int
    ): String {
        val elapsedMs = nowElapsed - startTimeMs
        if (elapsedMs <= 0L) return "0.0%/h"
        val hours = elapsedMs / 3_600_000.0
        if (hours <= 0.0) return "0.0%/h"

        val percentDrop = when {
            startLevel >= 0 && currentLevel >= 0 -> (startLevel - currentLevel).toDouble()
            else -> ((startChargeUah - currentChargeUah).toDouble() * 100.0) / designCapacityUah.toDouble()
        }
        if (percentDrop <= 0.0) return "0.0%/h"

        val percentPerHour = percentDrop / hours
        return String.format(Locale.US, "%.2f%%/h", percentPerHour)
    }

    private fun updateRealtimeUsed(currentMa: Float) {
        val now = System.currentTimeMillis()
        val diffSec = (now - lastUpdate) / 1000.0
        lastUpdate = now

        if (currentMa > 0f) {
            val add = (diffSec / 3600.0) * currentMa
            realtimeUsedMah += add
        }
    }

    private fun getDesignCapacityUah(): Long {
        // Try multiple sysfs paths; normalize unit to µAh
        val candidates = listOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/battery/charge_full",
            "/sys/class/power_supply/max170xx_battery/charge_full_design",
            "/sys/class/power_supply/max170xx_battery/charge_full"
        )
        for (p in candidates) {
            try {
                val raw = File(p).readText().trim().toLong()
                if (raw > 0) return normalizeToUah(raw)
            } catch (_: Exception) { }
        }
        // Fallback to derive from charge counter and level (no hardcoded value)
        val bm = batteryManager ?: return 0L
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charge = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        return if (level > 0 && charge > 0) (charge * 100) / level else 0L
    }

    private fun normalizeToUah(raw: Long): Long {
        // Heuristic: some kernels expose in mAh or nAh; normalize to µAh
        return when {
            raw in 1..100_000L -> raw * 1_000L // likely mAh
            raw >= 100_000_000L -> raw / 1_000L // likely nAh
            else -> raw // assume µAh
        }
    }

    // legacy stub removed; baseline is handled via sampling attribution

    private fun getChargingStatus(status: Int, plugged: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            else -> if (plugged > 0) "Plugged" else "Idle"
        }
    }

    // === NOTIFICATION ===
    private fun createNotification(title: String, bigText: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(null)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(pendingIntent)
            .setShowWhen(true)
            .setWhen(if (serviceStartedAtMs != 0L) serviceStartedAtMs else System.currentTimeMillis())
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Battery Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Menampilkan status baterai realtime"
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(stats: BatteryData) {
        val isChargingLike = stats.statusText == "Charging" || stats.statusText == "Plugged" || stats.statusText == "Full"
        val isFull = stats.statusText == "Full"
        val currentAbsMa = kotlin.math.abs(stats.currentMa)
        val currentDisplayMa = kotlin.math.abs(stats.currentMa.roundToInt())
        val statusLabel = if (isChargingLike && !isFull) {
            when {
                currentAbsMa > 2000f -> "Charging rapidly"
                currentAbsMa in 0.1f..999.9f -> "Charging slowly"
                else -> "Charging"
            }
        } else stats.statusText

        val title = if (isFull) {
            "${stats.level}% · ${stats.temperatureC.roundToInt()}°C · Fully Charged"
        } else {
            val baseTitle = "${stats.level}% · ${stats.temperatureC.roundToInt()}°C · ${statusLabel} · ${currentDisplayMa}mA"
            if (isChargingLike) {
                val wattStr = String.format(Locale.US, "%.1fW", kotlin.math.abs(stats.powerWatt))
                "$baseTitle · $wattStr"
            } else baseTitle
        }
        val onUsedPctStr = "${kotlin.math.max(0, onPercentDrop.roundToInt())}%"
        val offUsedPctStr = "${kotlin.math.max(0, offPercentDrop.roundToInt())}%"
        val bigText = """
            Active drain : ${stats.activeDrain} · idle drain   : ${stats.idleDrain}
            Screen on    : ${stats.screenOnTime} (${onUsedPctStr})
            Screen off   : ${stats.elapsed} (${offUsedPctStr})
            Deep sleep   : ${stats.deepSleep}
            Awake        : ${stats.uptime}
        """.trimIndent()
        notificationManager.notify(notificationId, createNotification(title, bigText))
    }

    private fun formatDurationAdaptive(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val h = (totalSec / 3600).toInt()
        val m = ((totalSec % 3600) / 60).toInt()
        val s = (totalSec % 60).toInt()
        return when {
            totalSec < 60 -> String.format(Locale.US, "%ds", s)
            totalSec < 3600 -> String.format(Locale.US, "%dm %ds", m, s)
            else -> String.format(Locale.US, "%dh %dm %ds", h, m, s)
        }
    }

    private fun persistState() {
        val now = SystemClock.elapsedRealtime()
        val updatedAccum = screenOnAccumMs + (screenOnStartAtElapsed?.let { now - it } ?: 0L)
        prefs.edit()
            .putLong(keyScreenAccum, updatedAccum)
            .putLong(keyLastElapsed, now)
            .putLong("window_start_elapsed", windowStartElapsed)
            .putLong("window_start_uptime", windowStartUptime)
            .apply()
    }

    private fun restoreStateIfAny() {
        val lastElapsed = prefs.getLong(keyLastElapsed, 0L)
        val savedAccum = prefs.getLong(keyScreenAccum, 0L)
        windowStartElapsed = prefs.getLong("window_start_elapsed", -1L)
        windowStartUptime = prefs.getLong("window_start_uptime", -1L)
        val now = SystemClock.elapsedRealtime()
        // Reset if device rebooted (elapsedRealtime wrapped/reset)
        screenOnAccumMs = if (lastElapsed == 0L || now < lastElapsed) 0L else savedAccum
        if (lastElapsed == 0L || now < lastElapsed) {
            windowStartElapsed = -1L
            windowStartUptime = -1L
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatteryMonitorService::class.java))
        }

        private const val ACTION_RESET = "id.nkz.nokontzzzmanager.action.RESET_BATTERY_MONITOR"

        fun reset(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java).apply { action = ACTION_RESET }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else context.startService(intent)
        }
    }

    data class BatteryData(
        val level: Int = -1,
        val temperatureC: Float = 0f,
        val statusText: String = "Unknown",
        val currentMa: Float = 0f,
        val powerWatt: Float = 0f,
        val activeDrain: String = "-",
        val idleDrain: String = "-",
        val screenOnTime: String = "-",
        val elapsed: String = "-",
        val uptime: String = "-",
        val deepSleep: String = "-"
    )
}