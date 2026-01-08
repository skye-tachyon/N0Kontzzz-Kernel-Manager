package id.nkz.nokontzzzmanager.ui.screens

import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import id.nkz.nokontzzzmanager.ui.components.IndeterminateExpressiveLoadingIndicator
import id.nkz.nokontzzzmanager.ui.components.CpuGovernorCard
import id.nkz.nokontzzzmanager.ui.components.GpuControlCard
import id.nkz.nokontzzzmanager.ui.components.SwappinessCard
import id.nkz.nokontzzzmanager.ui.components.ThermalCard
import id.nkz.nokontzzzmanager.viewmodel.TuningViewModel
import kotlinx.coroutines.launch


import id.nkz.nokontzzzmanager.R

// Daftar fitur dengan terjemahannya
val tuningFeatures = listOf(
    R.string.tuning_feature_performance_mode_title to R.string.tuning_feature_performance_mode_desc,
    R.string.tuning_feature_cpu_governor_title to R.string.tuning_feature_cpu_governor_desc,
    R.string.tuning_feature_gpu_control_title to R.string.tuning_feature_gpu_control_desc,
    R.string.tuning_feature_thermal_title to R.string.tuning_feature_thermal_desc,
    R.string.tuning_feature_swappiness_title to R.string.tuning_feature_swappiness_desc
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuningScreen(
    navController: NavController? = null,
    viewModel: TuningViewModel = hiltViewModel()
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()

    // Data is now loaded lazily. The LaunchedEffect triggers data loading
    // after the UI is composed, making the screen appear instantly.
    LaunchedEffect(Unit) {
        delay(150) // Allow navigation animation to finish
        viewModel.loadAllData()
    }

    // Listen for destination changes to reset scroll state
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route == "tuning") {
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

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            IndeterminateExpressiveLoadingIndicator()
        }
    } else {
        var showResetDialog by remember { mutableStateOf(false) }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.size(72.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = stringResource(id = R.string.reset_to_default_title)
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp + innerPadding.calculateTopPadding(),
                    bottom = 16.dp + innerPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Hero Header
                item {
                    HeroHeader(
                        onClick = { showInfoDialog = true }
                    )
                }

                item {
                    PerformanceModeCard(viewModel = viewModel)
                }

                item {
                    CpuGovernorCard(vm = viewModel)
                }

                item {
                    GpuControlCard(tuningViewModel = viewModel)
                }

                item {
                    ThermalCard(viewModel = viewModel)
                }

                item {
                    SwappinessCard(vm = viewModel)
                }
            }
        }

        if (showInfoDialog) {
            FeatureInfoDialog(
                onDismissRequest = { showInfoDialog = false },
                features = tuningFeatures
            )
        }

        if (showResetDialog) {
            ResetSettingsDialog(
                onDismiss = { showResetDialog = false },
                onReset = { cpu, gpu, thermal, ram ->
                    viewModel.resetSettings(cpu, gpu, thermal, ram)
                    showResetDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureInfoDialog(
    onDismissRequest: () -> Unit,
    features: List<Pair<Int, Int>>
) {
    // Full-screen dialog implementation according to MD3 guidelines with animation
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(initialScale = 0.95f),
            exit = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    // Top app bar for full-screen dialog
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(id = R.string.tuning_feature_info_title),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismissRequest) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.close)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    // Content with tabs and feature descriptions
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Feature descriptions
                        features.forEachIndexed { index, feature ->
                            FeatureDescription(
                                title = stringResource(id = feature.first),
                                description = stringResource(id = feature.second)
                            )
                            if (index < features.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    thickness = DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                        
                        // Add some bottom padding
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureDescription(title: String, description: String) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PerformanceModeCard(
    viewModel: TuningViewModel,
    blur: Boolean = true
) {
    // Collect the ACTIVE performance mode which reflects the real system state
    // If user changes governor manually or system uses default, this will be null
    val activePerformanceMode by viewModel.activePerformanceMode.collectAsState()
    val availableGovernors by viewModel.generalAvailableCpuGovernors.collectAsState()

    val isPowersaveAvailable = remember(availableGovernors) {
        availableGovernors.contains("powersave")
    }
    
    // Performance modes - Always show all modes to maintain layout stability
    val performanceModes = remember {
        listOf("Powersave", "Balanced", "Performance")
    }
    
    // Governor mappings
    val governorMappings = remember {
        mapOf(
            "Powersave" to "powersave",
            "Balanced" to "schedutil",
            "Performance" to "performance"
        )
    }

    // Custom color themes for each mode
    val balancedGreen = MaterialTheme.colorScheme.primary // Green for balanced
    val performanceRed = MaterialTheme.colorScheme.error // Red for performance
    val powersaveBlue = MaterialTheme.colorScheme.tertiary // Blue/Tertiary for powersave

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = stringResource(id = R.string.tuning_feature_performance_mode_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = stringResource(id = R.string.quick_presets_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                performanceModes.forEachIndexed { index, mode ->
                    val isFirst = index == 0
                    val isLast = index == performanceModes.lastIndex
                    val isEnabled = mode != "Powersave" || isPowersaveAvailable
                    
                    val shape = when {
                        isFirst -> RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        )
                        isLast -> RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        )
                        else -> RoundedCornerShape(4.dp)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isEnabled) {
                                // Let the ViewModel handle the logic
                                viewModel.onPerformanceModeChange(mode)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = when (mode) {
                                "Powersave" -> if (activePerformanceMode == mode) powersaveBlue.copy(alpha = 0.15f) else powersaveBlue.copy(alpha = 0.05f)
                                "Balanced" -> if (activePerformanceMode == mode) balancedGreen.copy(alpha = 0.15f) else balancedGreen.copy(alpha = 0.05f)
                                "Performance" -> if (activePerformanceMode == mode) performanceRed.copy(alpha = 0.15f) else performanceRed.copy(alpha = 0.05f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ).run {
                            if (!isEnabled) {
                                copy(containerColor = containerColor.copy(alpha = 0.02f))
                            } else this
                        },
                        shape = shape
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val contentAlpha = if (isEnabled) 1f else 0.38f
                            val baseColor = when (mode) {
                                "Powersave" -> powersaveBlue
                                "Balanced" -> balancedGreen
                                "Performance" -> performanceRed
                                else -> MaterialTheme.colorScheme.primary
                            }

                            // Icon with themed background
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        color = baseColor.copy(alpha = if (isEnabled) 0.2f else 0.05f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (mode) {
                                        "Powersave" -> Icons.Default.BatterySaver
                                        "Balanced" -> Icons.Default.Balance
                                        "Performance" -> Icons.Default.FlashOn
                                        else -> Icons.Default.Speed
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = baseColor.copy(alpha = contentAlpha)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (activePerformanceMode == mode) FontWeight.Bold else FontWeight.Medium,
                                    color = when (mode) {
                                        "Powersave" -> if (activePerformanceMode == mode) powersaveBlue else MaterialTheme.colorScheme.onSurface
                                        "Balanced" -> if (activePerformanceMode == mode) balancedGreen else MaterialTheme.colorScheme.onSurface
                                        "Performance" -> if (activePerformanceMode == mode) performanceRed else MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }.copy(alpha = contentAlpha)
                                )
                                Text(
                                    text = when (mode) {
                                        "Powersave" -> {
                                            val gov = governorMappings[mode] ?: "powersave"
                                            stringResource(id = R.string.powersave_performance_desc, gov)
                                        }
                                        "Balanced" -> {
                                            val gov = governorMappings[mode] ?: "schedutil"
                                            stringResource(id = R.string.balanced_performance_desc, gov)
                                        }
                                        "Performance" -> {
                                            val gov = governorMappings[mode] ?: "performance"
                                            stringResource(id = R.string.maximum_speed_desc, gov)
                                        }
                                        else -> {
                                            val gov = governorMappings[mode] ?: "default"
                                            stringResource(id = R.string.default_governor_desc, gov)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                                )
                            }

                            // Checkmark for selected mode
                            if (activePerformanceMode == mode) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(id = R.string.common_selected),
                                    modifier = Modifier.size(20.dp),
                                    tint = when (mode) {
                                        "Powersave" -> powersaveBlue
                                        "Balanced" -> balancedGreen
                                        "Performance" -> performanceRed
                                        else -> MaterialTheme.colorScheme.primary
                                    }.copy(alpha = contentAlpha)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = activePerformanceMode != null && activePerformanceMode != "Balanced",
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
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
                                text = stringResource(id = R.string.performance_mode_applied),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.changed_cpu_governor_desc, governorMappings[activePerformanceMode] ?: "schedutil"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroHeader(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Title
            Text(
                text = stringResource(id = R.string.system_tuning),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Description
            Text(
                text = stringResource(id = R.string.system_tuning_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            // Info text
            Text(
                text = stringResource(id = R.string.tap_for_more_info),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetSettingsDialog(
    onDismiss: () -> Unit,
    onReset: (Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var resetCpu by remember { mutableStateOf(true) }
    var resetGpu by remember { mutableStateOf(true) }
    var resetThermal by remember { mutableStateOf(true) }
    var resetRam by remember { mutableStateOf(true) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(id = R.string.reset_to_default_title),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(id = R.string.reset_to_default_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    // Selection List
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.reset_selection_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        ResetOptionItem(
                            title = stringResource(id = R.string.reset_cpu),
                            checked = resetCpu,
                            onCheckedChange = { resetCpu = it }
                        )
                        ResetOptionItem(
                            title = stringResource(id = R.string.reset_gpu),
                            checked = resetGpu,
                            onCheckedChange = { resetGpu = it }
                        )
                        ResetOptionItem(
                            title = stringResource(id = R.string.reset_thermal),
                            checked = resetThermal,
                            onCheckedChange = { resetThermal = it }
                        )
                        ResetOptionItem(
                            title = stringResource(id = R.string.reset_ram),
                            checked = resetRam,
                            onCheckedChange = { resetRam = it }
                        )
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(id = R.string.close))
                        }
                        Button(
                            onClick = { onReset(resetCpu, resetGpu, resetThermal, resetRam) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(id = R.string.reset_confirmation))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetOptionItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.error,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}