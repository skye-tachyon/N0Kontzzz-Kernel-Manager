package id.nkz.nokontzzzmanager.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.nkz.nokontzzzmanager.service.BatteryMonitorService
import id.nkz.nokontzzzmanager.ui.theme.ThemeMode
import id.nkz.nokontzzzmanager.util.ThemeManager
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import id.nkz.nokontzzzmanager.data.repository.BackupRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

import android.net.Uri

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    private val preferenceManager: PreferenceManager,
    private val backupRepository: BackupRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _notificationIconStyle = MutableStateFlow(preferenceManager.getNotificationIconStyle())
    val notificationIconStyle: StateFlow<Int> = _notificationIconStyle.asStateFlow()

    private val _isBatteryMonitorEnabled = MutableStateFlow(preferenceManager.isBatteryMonitorEnabled())
    val isBatteryMonitorEnabled: StateFlow<Boolean> = _isBatteryMonitorEnabled.asStateFlow()

    private val _backupRestoreEvent = MutableSharedFlow<String>()
    val backupRestoreEvent: SharedFlow<String> = _backupRestoreEvent

    val currentThemeMode: StateFlow<ThemeMode> = themeManager.currentThemeMode
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ThemeMode.SYSTEM_DEFAULT
        )

    val isAmoledMode: StateFlow<Boolean> = themeManager.isAmoledMode
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            false
        )

    val themeChanged: StateFlow<Boolean> = themeManager.themeChanged
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            false
        )

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            themeManager.setThemeMode(themeMode)
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setAmoledMode(enabled)
        }
    }

    fun setNotificationIconStyle(style: Int) {
        preferenceManager.setNotificationIconStyle(style)
        _notificationIconStyle.value = style
        try {
            BatteryMonitorService.updateIcon(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetThemeChangedSignal() {
        themeManager.resetThemeChangedSignal()
    }

    fun backupSettings(uri: Uri, includeTuning: Boolean, includeNetwork: Boolean, includeBattery: Boolean, includeOther: Boolean) {
        viewModelScope.launch {
            val result = backupRepository.createBackup(uri, includeTuning, includeNetwork, includeBattery, includeOther)
            if (result.isSuccess) {
                _backupRestoreEvent.emit("Backup created successfully")
            } else {
                _backupRestoreEvent.emit("Backup failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun restoreSettings(uri: Uri, restoreTuning: Boolean, restoreNetwork: Boolean, restoreBattery: Boolean, restoreOther: Boolean) {
        viewModelScope.launch {
            val result = backupRepository.restoreBackup(uri, restoreTuning, restoreNetwork, restoreBattery, restoreOther)
            if (result.isSuccess) {
                _backupRestoreEvent.emit("Settings restored successfully. A reboot may be required.")
                // Refresh local states if needed
                _notificationIconStyle.value = preferenceManager.getNotificationIconStyle()
                _isBatteryMonitorEnabled.value = preferenceManager.isBatteryMonitorEnabled()
            } else {
                _backupRestoreEvent.emit("Restore failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
