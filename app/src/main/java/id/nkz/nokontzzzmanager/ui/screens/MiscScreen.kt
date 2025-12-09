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

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import id.nkz.nokontzzzmanager.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.core.net.toUri

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscScreen(
    navController: NavController? = null,
    viewModel: MiscViewModel = hiltViewModel(),
) {
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    // Listen for destination changes to reset scroll state
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route == "misc") {
                coroutineScope.launch {
                    lazyListState.scrollToItem(0)
                }
            }
        }
        navController?.addOnDestinationChangedListener(listener)
        onDispose {
            navController?.removeOnDestinationChangedListener(listener)
        }
    }

    val kgslSkipZeroingEnabled by viewModel.kgslSkipZeroingEnabled.collectAsStateWithLifecycle()
    val isKgslFeatureAvailable by viewModel.isKgslFeatureAvailable.collectAsStateWithLifecycle()
    val bypassChargingEnabled by viewModel.bypassChargingEnabled.collectAsStateWithLifecycle()
    val isBypassChargingAvailable by viewModel.isBypassChargingAvailable.collectAsStateWithLifecycle()
    val batteryMonitorEnabled by viewModel.batteryMonitorEnabled.collectAsStateWithLifecycle()
    val tcpCongestionAlgorithm by viewModel.tcpCongestionAlgorithm.collectAsStateWithLifecycle()
    val availableTcpAlgorithms by viewModel.availableTcpCongestionAlgorithms.collectAsStateWithLifecycle()
    val ioScheduler by viewModel.ioScheduler.collectAsStateWithLifecycle()
    val availableIoSchedulers by viewModel.availableIoSchedulers.collectAsStateWithLifecycle()

    val autoResetOnReboot by viewModel.autoResetOnReboot.collectAsStateWithLifecycle()
    val autoResetOnCharging by viewModel.autoResetOnCharging.collectAsStateWithLifecycle()
    val autoResetAtLevel by viewModel.autoResetAtLevel.collectAsStateWithLifecycle()
    val autoResetTargetLevel by viewModel.autoResetTargetLevel.collectAsStateWithLifecycle()

    var showAutoResetDialog by remember { mutableStateOf(false) }

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

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleBatteryMonitor(true)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Group 1: GPU & Power
        item {
            Text(stringResource(id = R.string.gpu_power), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        // KGSL Skip Pool Zeroing feature
        item {
            KgslSkipZeroingCard(
                kgslSkipZeroingEnabled = kgslSkipZeroingEnabled,
                isKgslFeatureAvailable = isKgslFeatureAvailable,
                onToggleKgslSkipZeroing = { enabled ->
                    viewModel.toggleKgslSkipZeroing(enabled)
                }
            )
        }

        // Bypass Charging feature
        item {
            BypassChargingCard(
                bypassChargingEnabled = bypassChargingEnabled,
                isBypassChargingAvailable = isBypassChargingAvailable,
                onToggleBypassCharging = { enabled ->
                    viewModel.toggleBypassCharging(enabled)
                }
            )
        }

        // Battery Monitor toggle
        item {
            BatteryMonitorCard(
                enabled = batteryMonitorEnabled,
                onToggle = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // Prompt ignore battery optimizations jika belum di-whitelist
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
                    } else {
                        viewModel.toggleBatteryMonitor(enabled)
                    }
                }
            )
        }

        // Battery History
        item {
            BatteryHistoryCard(
                onClick = { navController?.navigate("battery_history") },
                onSettingsClick = { showAutoResetDialog = true }
            )
        }

        // Battery Monitor manual reset
        item {
            BatteryMonitorResetCard(
                onReset = {
                    viewModel.resetBatteryMonitor()
                },
                onEnsurePermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            )
        }

        // Spacer between groups
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Group 2: Network & Storage
        item {
            Text(stringResource(id = R.string.network_storage), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // TCP Congestion Control Algorithm feature
        item {
            TcpCongestionControlCard(
                tcpCongestionAlgorithm = tcpCongestionAlgorithm,
                availableAlgorithms = availableTcpAlgorithms,
                onAlgorithmChange = { algorithm ->
                    viewModel.updateTcpCongestionAlgorithm(algorithm)
                }
            )
        }
        
        // I/O Scheduler feature
        item {
            IoSchedulerCard(
                ioScheduler = ioScheduler,
                availableSchedulers = availableIoSchedulers,
                onSchedulerChange = { scheduler ->
                    viewModel.updateIoScheduler(scheduler)
                }
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KgslSkipZeroingCard(
    kgslSkipZeroingEnabled: Boolean,
    isKgslFeatureAvailable: Boolean?,
    onToggleKgslSkipZeroing: (Boolean) -> Unit,
) {
    // Treat null as false for UI purposes, preventing flicker during initial load
    val featureAvailable = isKgslFeatureAvailable == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = {
            if (featureAvailable) {
                onToggleKgslSkipZeroing(!kgslSkipZeroingEnabled)
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.kgsl_skip_pool_zeroing),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                    Text(
                        text = if (featureAvailable) {
                            stringResource(id = R.string.kgsl_skip_pool_zeroing_desc)
                        } else {
                            stringResource(id = R.string.feature_not_available)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                }

                Switch(
                    checked = kgslSkipZeroingEnabled && featureAvailable,
                    onCheckedChange = null,
                    enabled = featureAvailable,
                    thumbContent = if (kgslSkipZeroingEnabled && featureAvailable) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            }

            if (kgslSkipZeroingEnabled && featureAvailable) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(id = R.string.performance_mode_active),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.kgsl_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryMonitorResetCard(
    onReset: () -> Unit,
    onEnsurePermission: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = {
            onEnsurePermission()
            onReset()
            Toast.makeText(context, context.getString(R.string.battery_stats_reset_toast), Toast.LENGTH_SHORT).show()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.battery_monitor_reset_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.battery_monitor_reset_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryMonitorCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = { onToggle(!enabled) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.battery_monitor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.battery_monitor_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = null,
                    thumbContent = if (enabled) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TcpCongestionControlCard(
    tcpCongestionAlgorithm: String?,
    availableAlgorithms: List<String>,
    onAlgorithmChange: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = { 
            if (availableAlgorithms.isNotEmpty()) {
                showDialog = true 
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.tcp_congestion_control),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tcpCongestionAlgorithm ?: "...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(id = R.string.change_algorithm),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    if (showDialog) {
        TcpCongestionDialog(
            currentAlgorithm = tcpCongestionAlgorithm ?: "",
            availableAlgorithms = availableAlgorithms,
            onAlgorithmSelected = { algorithm ->
                onAlgorithmChange(algorithm)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun IoSchedulerCard(
    ioScheduler: String?,
    availableSchedulers: List<String>,
    onSchedulerChange: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = { 
            if (availableSchedulers.isNotEmpty()) {
                showDialog = true 
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.io_scheduler),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = ioScheduler ?: "...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(id = R.string.change_scheduler),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    if (showDialog) {
        IoSchedulerDialog(
            currentScheduler = ioScheduler ?: "",
            availableSchedulers = availableSchedulers,
            onSchedulerSelected = { scheduler ->
                onSchedulerChange(scheduler)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BypassChargingCard(
    bypassChargingEnabled: Boolean,
    isBypassChargingAvailable: Boolean?,
    onToggleBypassCharging: (Boolean) -> Unit,
) {
    // Treat null as false for UI purposes, preventing flicker during initial load
    val featureAvailable = isBypassChargingAvailable == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = {
            if (featureAvailable) {
                onToggleBypassCharging(!bypassChargingEnabled)
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.bypass_charging),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                    Text(
                        text = if (featureAvailable) {
                            stringResource(id = R.string.bypass_charging_desc)
                        } else {
                            stringResource(id = R.string.feature_not_available)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                }

                Switch(
                    checked = bypassChargingEnabled && featureAvailable,
                    onCheckedChange = null,
                    enabled = featureAvailable,
                    thumbContent = if (bypassChargingEnabled && featureAvailable) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            }

            if (bypassChargingEnabled && featureAvailable) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(id = R.string.bypass_charging_activated),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.bypass_charging_active_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryHistoryCard(
    onClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.battery_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.battery_history_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Auto-Reset Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
