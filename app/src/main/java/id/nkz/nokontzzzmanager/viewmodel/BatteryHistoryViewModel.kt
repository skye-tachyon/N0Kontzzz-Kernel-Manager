package id.nkz.nokontzzzmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nkz.nokontzzzmanager.data.database.BatteryGraphEntry
import id.nkz.nokontzzzmanager.data.repository.BatteryGraphRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatteryHistoryViewModel @Inject constructor(
    private val repository: BatteryGraphRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter.LAST_24_HOURS)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

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
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
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
    SINCE_UNPLUGGED
}