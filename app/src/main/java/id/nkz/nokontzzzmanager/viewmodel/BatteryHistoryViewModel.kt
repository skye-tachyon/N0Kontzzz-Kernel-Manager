package id.nkz.nokontzzzmanager.viewmodel

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.BatteryGraphEntry
import id.nkz.nokontzzzmanager.data.model.AppUsageInfo
import id.nkz.nokontzzzmanager.data.repository.BatteryGraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.graphics.scale

@HiltViewModel
class BatteryHistoryViewModel @Inject constructor(
    private val repository: BatteryGraphRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter.LAST_24_HOURS)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    private val _appUsageList = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val appUsageList: StateFlow<List<AppUsageInfo>> = _appUsageList.asStateFlow()

    init {
        viewModelScope.launch {
            filter.collect {
                loadAppUsageStats()
            }
        }
    }

    fun checkUsagePermission() {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        _hasUsagePermission.value = mode == AppOpsManager.MODE_ALLOWED
        if (_hasUsagePermission.value) {
            loadAppUsageStats()
        }
    }

    private fun loadAppUsageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            checkUsagePermission()
            if (!_hasUsagePermission.value) return@launch

            val now = System.currentTimeMillis()
            val startTime = when (_filter.value) {
                HistoryFilter.LAST_24_HOURS, HistoryFilter.PER_CYCLE -> now - 24 * 60 * 60 * 1000
                HistoryFilter.SINCE_UNPLUGGED -> {
                    val allEntries = repository.getAllEntries().firstOrNull() ?: emptyList()
                    val lastChargeIndex = allEntries.indexOfLast { it.isCharging }
                    if (lastChargeIndex != -1 && lastChargeIndex < allEntries.size - 1) {
                        allEntries[lastChargeIndex + 1].timestamp
                    } else {
                        now - 24 * 60 * 60 * 1000
                    }
                }
            }

            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, now)

            if (stats.isNullOrEmpty()) {
                _appUsageList.value = emptyList()
                return@launch
            }

            val aggregatedStats = stats.groupBy { it.packageName }
                .mapValues { (_, entries) -> entries.sumOf { it.totalTimeInForeground } }
                .toList()
                .sortedByDescending { it.second }
            
            val totalUsageTime = aggregatedStats.sumOf { it.second }.coerceAtLeast(1L)
            
            val topApps = aggregatedStats.take(10)

            val pm = context.packageManager
            val iconSizePx = (32 * context.resources.displayMetrics.density).toInt() // Target 32dp

            val appList = topApps.mapNotNull { (packageName, totalTime) ->
                if (totalTime == 0L) return@mapNotNull null
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    
                    // Load and resize icon
                    val originalDrawable = pm.getApplicationIcon(appInfo)
                    val originalBitmap = originalDrawable.toBitmap()
                    val resizedBitmap = if (originalBitmap.width > iconSizePx || originalBitmap.height > iconSizePx) {
                        originalBitmap.scale(iconSizePx, iconSizePx)
                    } else {
                        originalBitmap
                    }

                    // Format time
                    val hours = totalTime / (1000 * 60 * 60)
                    val minutes = (totalTime / (1000 * 60)) % 60
                    val formattedTime = if (hours > 0) {
                        context.getString(R.string.usage_time_format, hours, minutes)
                    } else {
                        context.getString(R.string.usage_time_format_min, minutes)
                    }
                    
                    val percentage = ((totalTime.toDouble() / totalUsageTime.toDouble()) * 100).toInt()

                    AppUsageInfo(packageName, appName, totalTime, formattedTime, percentage, resizedBitmap)
                } catch (e: Exception) {
                    null
                }
            }
            _appUsageList.value = appList
        }
    }

    // Get history with filter applied
    val historyData: StateFlow<List<BatteryGraphEntry>> = combine(
        repository.getAllEntries(),
        _filter
    ) { entries, currentFilter ->
        val now = System.currentTimeMillis()
        when (currentFilter) {
            HistoryFilter.LAST_24_HOURS -> {
                val twentyFourHoursAgo = now - (24 * 60 * 60 * 1000)
                entries.filter { it.timestamp >= twentyFourHoursAgo }
            }
            HistoryFilter.SINCE_UNPLUGGED -> {
                // Find the last time it was charging
                val lastChargeIndex = entries.indexOfLast { it.isCharging }
                if (lastChargeIndex != -1) {
                    // Return entries starting from the point it stopped charging (or the last charge point)
                    // Usually "since unplugged" means from the moment it was unplugged.
                    // So we take entries AFTER the last charge entry.
                    // If currently charging (last entry is charging), it might show nothing or just the current charging session?
                    // Let's include the transition point.
                    entries.subList(lastChargeIndex, entries.size)
                } else {
                    // Never charged in history? Show all.
                    entries
                }
            }
            HistoryFilter.PER_CYCLE -> {
                entries
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun setFilter(newFilter: HistoryFilter) {
        _filter.value = newFilter
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.deleteAllEntries()
        }
    }
}

enum class HistoryFilter {
    LAST_24_HOURS,
    SINCE_UNPLUGGED,
    PER_CYCLE
}
