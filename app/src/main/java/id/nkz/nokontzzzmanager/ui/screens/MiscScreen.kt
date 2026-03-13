package id.nkz.nokontzzzmanager.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import id.nkz.nokontzzzmanager.ui.dialog.TcpCongestionDialog
import id.nkz.nokontzzzmanager.viewmodel.MiscViewModel
import id.nkz.nokontzzzmanager.ui.dialog.IoSchedulerDialog
import id.nkz.nokontzzzmanager.ui.dialog.BatteryHistoryConfigDialog
import id.nkz.nokontzzzmanager.ui.dialog.ChargingControlDialog

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import id.nkz.nokontzzzmanager.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.core.net.toUri

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscScreen(
    navController: NavController? = null,
    viewModel: MiscViewModel = hiltViewModel(),
) {
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    val kgslSkipZeroingEnabled by viewModel.kgslSkipZeroingEnabled.collectAsStateWithLifecycle()
    val isKgslFeatureAvailable by viewModel.isKgslFeatureAvailable.collectAsStateWithLifecycle()
    val avoidDirtyPteEnabled by viewModel.avoidDirtyPteEnabled.collectAsStateWithLifecycle()
    val isAvoidDirtyPteAvailable by viewModel.isAvoidDirtyPteAvailable.collectAsStateWithLifecycle()
    val bypassChargingEnabled by viewModel.bypassChargingEnabled.collectAsStateWithLifecycle()
    val isBypassChargingAvailable by viewModel.isBypassChargingAvailable.collectAsStateWithLifecycle()
    val forceFastChargeEnabled by viewModel.forceFastChargeEnabled.collectAsStateWithLifecycle()
    val isForceFastChargeAvailable by viewModel.isForceFastChargeAvailable.collectAsStateWithLifecycle()
    val batteryMonitorEnabled by viewModel.batteryMonitorEnabled.collectAsStateWithLifecycle()
    val tcpCongestionAlgorithm by viewModel.tcpCongestionAlgorithm.collectAsStateWithLifecycle()
    val availableTcpAlgorithms by viewModel.availableTcpCongestionAlgorithms.collectAsStateWithLifecycle()
    val ioScheduler by viewModel.ioScheduler.collectAsStateWithLifecycle()
    val availableIoSchedulers by viewModel.availableIoSchedulers.collectAsStateWithLifecycle()
    val applyNetworkStorageOnBoot by viewModel.applyNetworkStorageOnBoot.collectAsStateWithLifecycle()

    val autoResetOnReboot by viewModel.autoResetOnReboot.collectAsStateWithLifecycle()
    val autoResetOnCharging by viewModel.autoResetOnCharging.collectAsStateWithLifecycle()
    val autoResetAtLevel by viewModel.autoResetAtLevel.collectAsStateWithLifecycle()
    val autoResetTargetLevel by viewModel.autoResetTargetLevel.collectAsStateWithLifecycle()

    val monitorAutoResetOnReboot by viewModel.monitorAutoResetOnReboot.collectAsStateWithLifecycle()
    val monitorAutoResetOnCharging by viewModel.monitorAutoResetOnCharging.collectAsStateWithLifecycle()
    val monitorAutoResetAtLevel by viewModel.monitorAutoResetAtLevel.collectAsStateWithLifecycle()
    val monitorAutoResetTargetLevel by viewModel.monitorAutoResetTargetLevel.collectAsStateWithLifecycle()

    val chargingControlEnabled by viewModel.chargingControlEnabled.collectAsStateWithLifecycle()
    val chargingControlStopLevel by viewModel.chargingControlStopLevel.collectAsStateWithLifecycle()
    val chargingControlResumeLevel by viewModel.chargingControlResumeLevel.collectAsStateWithLifecycle()
    val batteryInfo by viewModel.batteryInfo.collectAsStateWithLifecycle()

    var showAutoResetDialog by remember { mutableStateOf(false) }
    var showMonitorAutoResetDialog by remember { mutableStateOf(false) }
    var showChargingControlDialog by remember { mutableStateOf(false) }

    if (showAutoResetDialog) {
        BatteryHistoryConfigDialog(
            onDismiss = { showAutoResetDialog = false },
            resetOnReboot = autoResetOnReboot,
            onResetOnRebootChange = viewModel::setAutoResetOnReboot,
            resetOnCharging = autoResetOnCharging,
            onResetOnChargingChange = viewModel::setAutoResetOnCharging,
            resetAtLevel = autoResetAtLevel,
            onResetAtLevelChange = viewModel::setAutoResetAtLevel,
            targetLevel = autoResetTargetLevel,
            onTargetLevelChange = viewModel::setAutoResetTargetLevel
        )
    }

    if (showMonitorAutoResetDialog) {
        BatteryHistoryConfigDialog(
            onDismiss = { showMonitorAutoResetDialog = false },
            title = stringResource(R.string.battery_monitor_config_title),
            description = stringResource(R.string.battery_monitor_config_desc),
            resetOnReboot = monitorAutoResetOnReboot,
            onResetOnRebootChange = viewModel::setMonitorAutoResetOnReboot,
            resetOnCharging = monitorAutoResetOnCharging,
            onResetOnChargingChange = viewModel::setMonitorAutoResetOnCharging,
            resetAtLevel = monitorAutoResetAtLevel,
            onResetAtLevelChange = viewModel::setMonitorAutoResetAtLevel,
            targetLevel = monitorAutoResetTargetLevel,
            onTargetLevelChange = viewModel::setMonitorAutoResetTargetLevel
        )
    }

    if (showChargingControlDialog) {
        ChargingControlDialog(
            onDismiss = { showChargingControlDialog = false },
            enabled = chargingControlEnabled,
            onEnabledChange = viewModel::setChargingControlEnabled,
            stopLevel = chargingControlStopLevel,
            onStopLevelChange = viewModel::setChargingControlStopLevel,
            resumeLevel = chargingControlResumeLevel,
            onResumeLevelChange = viewModel::setChargingControlResumeLevel,
            batteryInfo = batteryInfo,
            isBypassActive = bypassChargingEnabled
        )
    }

    val context = LocalContext.current

    val checkBatteryOptimizationAndEnable = {
        val pm = context.getSystemService(PowerManager::class.java)
        if (pm != null && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
            try {
                val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = ("package:" + context.packageName).toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(i)
            } catch (_: Exception) { }
        }
        viewModel.toggleBatteryMonitor(true)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkBatteryOptimizationAndEnable()
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 0.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text(stringResource(id = R.string.kernel), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            CustomTunableEntryCard(
                onClick = { navController?.navigate("custom_tunable") },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
            )
        }
        item {
            TcpCongestionControlCard(
                tcpCongestionAlgorithm = tcpCongestionAlgorithm,
                availableAlgorithms = availableTcpAlgorithms,
                onAlgorithmChange = { viewModel.updateTcpCongestionAlgorithm(it) },
                shape = RoundedCornerShape(8.dp)
            )
        }
        item {
            IoSchedulerCard(
                ioScheduler = ioScheduler,
                availableSchedulers = availableIoSchedulers,
                onSchedulerChange = { viewModel.updateIoScheduler(it) },
                shape = RoundedCornerShape(8.dp)
            )
        }
        item {
            NetworkStorageOnBootCard(
                applyOnBoot = applyNetworkStorageOnBoot,
                onToggle = { viewModel.setApplyNetworkStorageOnBoot(it) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(stringResource(id = R.string.gpu_power), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            KgslSkipZeroingCard(
                kgslSkipZeroingEnabled = kgslSkipZeroingEnabled,
                isKgslFeatureAvailable = isKgslFeatureAvailable,
                onToggleKgslSkipZeroing = { viewModel.toggleKgslSkipZeroing(it) }
            )
        }
        item {
            AvoidDirtyPteCard(
                avoidDirtyPteEnabled = avoidDirtyPteEnabled,
                isAvoidDirtyPteAvailable = isAvoidDirtyPteAvailable,
                onToggleAvoidDirtyPte = { viewModel.toggleAvoidDirtyPte(it) }
            )
        }
        item {
            BypassChargingCard(
                bypassChargingEnabled = bypassChargingEnabled,
                isBypassChargingAvailable = isBypassChargingAvailable,
                isChargingControlEnabled = chargingControlEnabled,
                onToggleBypassCharging = { viewModel.toggleBypassCharging(it) }
            )
        }
        item {
            ForceFastChargeCard(
                forceFastChargeEnabled = forceFastChargeEnabled,
                isForceFastChargeAvailable = isForceFastChargeAvailable,
                onToggleForceFastCharge = { viewModel.toggleForceFastCharge(it) }
            )
        }
        item {
            ChargingControlCard(
                enabled = chargingControlEnabled,
                isBatteryMonitorEnabled = batteryMonitorEnabled,
                isBypassChargingEnabled = bypassChargingEnabled,
                onClick = { showChargingControlDialog = true }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(stringResource(id = R.string.battery_settings), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            BatteryMonitorCard(
                enabled = batteryMonitorEnabled,
                onToggle = { enabled ->
                    if (enabled) {
                        var hasPermission = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                hasPermission = false
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        if (hasPermission) checkBatteryOptimizationAndEnable()
                    } else {
                        viewModel.toggleBatteryMonitor(false)
                    }
                }
            )
        }
        item {
            BatteryMonitorAutoResetCard(onClick = { showMonitorAutoResetDialog = true })
        }
        item {
            BatteryMonitorResetCard(onReset = { viewModel.resetBatteryMonitor() }, onEnsurePermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            })
        }
        item {
            BatteryHistoryCard(onClick = { navController?.navigate("battery_history") }, onSettingsClick = { showAutoResetDialog = true })
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(stringResource(id = R.string.automation_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item { AppProfilesCard(onClick = { navController?.navigate("app_profiles") }) }
        item { FpsMonitorCard(onClick = { navController?.navigate("fps_monitor") }) }
        item { BgBlockerCard(onClick = { navController?.navigate("bg_blocker") }) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(stringResource(id = R.string.system_stats_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item { ProcessMonitorCard(onClick = { navController?.navigate("process_monitor") }) }
        item { DexoptCard(onClick = { navController?.navigate("dexopt") }) }
        item { WakelockCard(onClick = { navController?.navigate("wakelock_monitor") }) }
        item { KernelLogCard(onClick = { navController?.navigate("kernel_log") }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkStorageOnBootCard(applyOnBoot: Boolean, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = { onToggle(!applyOnBoot) }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.apply_on_boot_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.apply_on_boot_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = applyOnBoot, onCheckedChange = null, thumbContent = if (applyOnBoot) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else { { Icon(Icons.Default.Close, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } })
            }
            if (applyOnBoot) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.apply_on_boot_active), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(stringResource(R.string.apply_on_boot_active_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KgslSkipZeroingCard(kgslSkipZeroingEnabled: Boolean, isKgslFeatureAvailable: Boolean?, onToggleKgslSkipZeroing: (Boolean) -> Unit) {
    val featureAvailable = isKgslFeatureAvailable == true
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = { if (featureAvailable) onToggleKgslSkipZeroing(!kgslSkipZeroingEnabled) }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = if (featureAvailable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.VideogameAsset, null, tint = if (featureAvailable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.kgsl_skip_pool_zeroing), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(if (featureAvailable) stringResource(R.string.kgsl_skip_pool_zeroing_desc) else stringResource(R.string.feature_not_available), style = MaterialTheme.typography.bodySmall, color = if (featureAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(checked = kgslSkipZeroingEnabled, onCheckedChange = null, enabled = featureAvailable, thumbContent = if (kgslSkipZeroingEnabled) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else { { Icon(Icons.Default.Close, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } })
            }
            if (kgslSkipZeroingEnabled && featureAvailable) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.performance_mode_active), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(stringResource(R.string.kgsl_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvoidDirtyPteCard(avoidDirtyPteEnabled: Boolean, isAvoidDirtyPteAvailable: Boolean?, onToggleAvoidDirtyPte: (Boolean) -> Unit) {
    val featureAvailable = isAvoidDirtyPteAvailable == true
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = { if (featureAvailable) onToggleAvoidDirtyPte(!avoidDirtyPteEnabled) }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = if (featureAvailable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Memory, null, tint = if (featureAvailable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.avoid_dirty_pte), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(if (featureAvailable) stringResource(R.string.avoid_dirty_pte_desc) else stringResource(R.string.feature_not_available), style = MaterialTheme.typography.bodySmall, color = if (featureAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(checked = avoidDirtyPteEnabled, onCheckedChange = null, enabled = featureAvailable, thumbContent = if (avoidDirtyPteEnabled) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else { { Icon(Icons.Default.Close, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } })
            }
            if (avoidDirtyPteEnabled && featureAvailable) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.avoid_dirty_pte_activated), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(stringResource(R.string.avoid_dirty_pte_active_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfilesCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AppSettingsAlt, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_profiles_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.app_profiles_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FpsMonitorCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.fps_monitor_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.fps_monitor_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BgBlockerCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp, 8.dp, 24.dp, 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.bg_blocker_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.bg_blocker_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryMonitorResetCard(onReset: () -> Unit, onEnsurePermission: () -> Unit) {
    val context = LocalContext.current
    val msg = stringResource(R.string.battery_stats_reset_toast)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = { onReset(); Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.battery_monitor_reset_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.battery_monitor_reset_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.Refresh, "Reset", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryMonitorAutoResetCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.battery_monitor_config_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.battery_monitor_config_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryMonitorCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = { onToggle(!enabled) }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MonitorHeart, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.battery_monitor), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.battery_monitor_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = null, thumbContent = if (enabled) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else { { Icon(Icons.Default.Close, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTunableEntryCard(onClick: () -> Unit, shape: RoundedCornerShape) {
    Card(modifier = Modifier.fillMaxWidth(), shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.custom_tunable_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.custom_tunable_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TcpCongestionControlCard(tcpCongestionAlgorithm: String?, availableAlgorithms: List<String>, onAlgorithmChange: (String) -> Unit, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)) {
    var showDialog by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = { if (availableAlgorithms.isNotEmpty()) showDialog = true }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Router, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.tcp_congestion_control), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(tcpCongestionAlgorithm ?: "...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Change", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showDialog) { TcpCongestionDialog(currentAlgorithm = tcpCongestionAlgorithm ?: "", availableAlgorithms = availableAlgorithms, onAlgorithmSelected = { onAlgorithmChange(it); showDialog = false }, onDismiss = { showDialog = false }) }
}

@Composable
fun IoSchedulerCard(ioScheduler: String?, availableSchedulers: List<String>, onSchedulerChange: (String) -> Unit, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)) {
    var showDialog by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = { if (availableSchedulers.isNotEmpty()) showDialog = true }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.io_scheduler), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(ioScheduler ?: "...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Change", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showDialog) { IoSchedulerDialog(currentScheduler = ioScheduler ?: "", availableSchedulers = availableSchedulers, onSchedulerSelected = { onSchedulerChange(it); showDialog = false }, onDismiss = { showDialog = false }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BypassChargingCard(bypassChargingEnabled: Boolean, isBypassChargingAvailable: Boolean?, isChargingControlEnabled: Boolean, onToggleBypassCharging: (Boolean) -> Unit) {
    val featureAvailable = isBypassChargingAvailable == true
    val isEnabled = featureAvailable && !isChargingControlEnabled
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = { if (isEnabled) onToggleBypassCharging(!bypassChargingEnabled) }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BatteryChargingFull, null, tint = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.bypass_charging), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(if (isChargingControlEnabled) stringResource(R.string.charging_control_active_subtitle) else if (featureAvailable) stringResource(R.string.bypass_charging_desc) else stringResource(R.string.feature_not_available), style = MaterialTheme.typography.bodySmall, color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(checked = bypassChargingEnabled, onCheckedChange = null, enabled = isEnabled, thumbContent = if (bypassChargingEnabled) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else { { Icon(Icons.Default.Close, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } })
            }
            if (bypassChargingEnabled && featureAvailable) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.bypass_charging_activated), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(stringResource(R.string.bypass_charging_active_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingControlCard(enabled: Boolean, isBatteryMonitorEnabled: Boolean, isBypassChargingEnabled: Boolean, onClick: () -> Unit) {
    val isEnabled = isBatteryMonitorEnabled && (!isBypassChargingEnabled || enabled)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp, 8.dp, 24.dp, 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = { if (isEnabled) onClick() }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BatterySaver, null, tint = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.charging_control_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(if (isBypassChargingEnabled && !enabled) stringResource(R.string.bypass_active_subtitle) else if (isBatteryMonitorEnabled) stringResource(R.string.charging_control_desc) else stringResource(R.string.requires_battery_monitor), style = MaterialTheme.typography.bodySmall, color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryHistoryCard(onClick: () -> Unit, onSettingsClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.battery_history_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.battery_history_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForceFastChargeCard(forceFastChargeEnabled: Boolean, isForceFastChargeAvailable: Boolean?, onToggleForceFastCharge: (Boolean) -> Unit) {
    val featureAvailable = isForceFastChargeAvailable == true
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = { if (featureAvailable) onToggleForceFastCharge(!forceFastChargeEnabled) }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = if (featureAvailable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ElectricBolt, null, tint = if (featureAvailable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.force_fast_charge), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(if (featureAvailable) stringResource(R.string.force_fast_charge_desc) else stringResource(R.string.feature_not_available), style = MaterialTheme.typography.bodySmall, color = if (featureAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(checked = forceFastChargeEnabled, onCheckedChange = null, enabled = featureAvailable, thumbContent = if (forceFastChargeEnabled) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else { { Icon(Icons.Default.Close, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } })
            }
            if (forceFastChargeEnabled && featureAvailable) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.force_fast_charge_activated), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(stringResource(R.string.force_fast_charge_active_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessMonitorCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.process_monitor_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.process_monitor_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexoptCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SettingsSuggest, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dexopt_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.dexopt_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakelockCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.wakelock_monitor_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.wakelock_monitor_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KernelLogCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp, 8.dp, 24.dp, 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.kernel_log_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.kernel_log_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
