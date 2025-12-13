package id.nkz.nokontzzzmanager.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nkz.nokontzzzmanager.data.database.AppProfileEntity
import id.nkz.nokontzzzmanager.data.repository.AppProfileRepository
import id.nkz.nokontzzzmanager.service.AppMonitorService
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AppProfilesViewModel @Inject constructor(
    private val application: Application,
    private val appProfileRepository: AppProfileRepository,
    private val preferenceManager: PreferenceManager
) : AndroidViewModel(application) {

    val profiles = appProfileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()
    
    // Check if service is running or permissions granted? 
    // We can check usage stats permission here.
    
    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = application.packageManager
                val packages = pm.getInstalledPackages(0)
                packages.mapNotNull { packageInfo ->
                    val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                    // val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 // isSystem not used currently
                    
                    val label = appInfo.loadLabel(pm).toString()

                    AppInfo(
                        packageName = packageInfo.packageName,
                        appName = label
                    )
                }.sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun addProfile(appInfo: AppInfo) {
        viewModelScope.launch {
            val profile = AppProfileEntity(
                packageName = appInfo.packageName,
                appName = appInfo.appName,
                // Default settings
                performanceMode = "Balanced",
                kgslSkipZeroing = false,
                bypassCharging = false,
                isEnabled = true
            )
            appProfileRepository.insertProfile(profile)
        }
    }

    fun updateProfile(profile: AppProfileEntity) {
        viewModelScope.launch {
            appProfileRepository.insertProfile(profile)
        }
    }

    fun deleteProfile(profile: AppProfileEntity) {
        viewModelScope.launch {
            appProfileRepository.deleteProfile(profile)
        }
    }

    fun toggleService(enabled: Boolean) {
        // We can use PreferenceManager to store a "Service Enabled" toggle if we want the user to easily disable the whole feature.
        // For now, let's just start/stop service.
        val intent = Intent(application, AppMonitorService::class.java)
        if (enabled) {
            application.startService(intent)
        } else {
            application.stopService(intent)
        }
    }
    
    fun hasUsageStatsPermission(): Boolean {
        val appOps = application.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            application.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String
)
