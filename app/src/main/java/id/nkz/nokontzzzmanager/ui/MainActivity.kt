
package id.nkz.nokontzzzmanager.ui

import id.nkz.nokontzzzmanager.ui.screens.TuningScreen
import android.content.Context
import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import id.nkz.nokontzzzmanager.data.repository.RootRepository
import id.nkz.nokontzzzmanager.data.repository.ThermalRepository
import id.nkz.nokontzzzmanager.ui.components.BottomNavBar

import id.nkz.nokontzzzmanager.ui.dialog.KernelVerificationDialog
import id.nkz.nokontzzzmanager.ui.dialog.RootRequiredDialog
import id.nkz.nokontzzzmanager.ui.dialog.BatteryOptDialog
import id.nkz.nokontzzzmanager.ui.screens.*
import id.nkz.nokontzzzmanager.ui.theme.RvKernelManagerTheme
import id.nkz.nokontzzzmanager.util.ThemeManager
import id.nkz.nokontzzzmanager.util.BatteryOptimizationChecker
import id.nkz.nokontzzzmanager.ui.components.UnifiedTopAppBar
import javax.inject.Inject
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import id.nkz.nokontzzzmanager.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import android.Manifest
import androidx.activity.viewModels
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel

import id.nkz.nokontzzzmanager.utils.LocaleHelper
import id.nkz.nokontzzzmanager.viewmodel.MainViewModel
import id.nkz.nokontzzzmanager.viewmodel.KernelLogViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    @Inject
    lateinit var rootRepo: RootRepository

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var thermalRepository: ThermalRepository

    @Inject
    lateinit var themeManager: ThemeManager

    private lateinit var batteryOptChecker: BatteryOptimizationChecker
    private var showBatteryOptDialog by mutableStateOf(false)
    private var showRootRequiredDialog by mutableStateOf(false)
    private var showKernelVerificationDialog by mutableStateOf(false)
    private var permissionDenialCount by mutableIntStateOf(0)
    private val MAX_PERMISSION_RETRIES = 2
    private var notifPermissionLauncher: ActivityResultLauncher<String>? = null

    // State for BatteryHistoryScreen FAB
    val batteryHistoryFabVisible = mutableStateOf(false)
    val batteryHistoryFabAction: MutableState<(() -> Unit)?> = mutableStateOf(null)

    // State for ProcessMonitor FAB
    val processMonitorFabVisible = mutableStateOf(false)
    val processMonitorFabAction: MutableState<(() -> Unit)?> = mutableStateOf(null)

    // State for CustomTunable FAB
    val customTunableFabVisible = mutableStateOf(false)
    val customTunableFabAction: MutableState<(() -> Unit)?> = mutableStateOf(null)

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Trigger failsafe restore for network & storage settings
        mainViewModel.runFailsafeNetworkStorageRestore()

        // Initialize batteryOptChecker regardless of root status for consistency
        batteryOptChecker = BatteryOptimizationChecker(this)
        
        // Daftarkan launcher izin notifikasi (Android 13+), lalu minta sekali di awal bila perlu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                // tandai sudah pernah prompt agar tidak berulang
                getSharedPreferences("perm_prefs", MODE_PRIVATE).edit {
                    putBoolean("notif_prompted", true)
                }
            }
            maybeRequestNotificationPermissionOnce()
        }
        
        // Initial check for root and kernel
        lifecycleScope.launch {
            mainViewModel.checkRootAndKernel()
        }

        setContent {
            val isRootAvailable by mainViewModel.isRootAvailable.collectAsStateWithLifecycle()
            val isKernelSupported by mainViewModel.isKernelSupported.collectAsStateWithLifecycle()

            // Update dialog visibility based on ViewModel state
            LaunchedEffect(isRootAvailable) {
                showRootRequiredDialog = isRootAvailable == false
            }

            LaunchedEffect(isKernelSupported) {
                if (isRootAvailable == true) {
                    showKernelVerificationDialog = isKernelSupported == false
                    if (isKernelSupported == true) {
                        checkAndHandlePermissions()
                    }
                }
            }

            RvKernelManagerTheme(themeManager = themeManager) {
                val navController = rememberNavController()


                LaunchedEffect(Unit) {
                    if (intent.getBooleanExtra("navigateToSettings", false)) {
                        navController.navigate("settings")
                    }
                }

                val items = listOf(stringResource(id = R.string.home), stringResource(id = R.string.tuning), stringResource(id = R.string.misc))
                val topAppBarState = rememberTopAppBarState()
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val isAmoledMode by themeManager.isAmoledMode.collectAsState(initial = false)

                val currentRoute = currentDestination?.route
                
                // Hoisted ViewModel for Kernel Log to drive TopAppBar actions
                var kernelLogViewModel by remember { mutableStateOf<KernelLogViewModel?>(null) }

                // Reset TopAppBar scroll state when navigating to a new screen
                LaunchedEffect(currentRoute) {
                    topAppBarState.contentOffset = 0f
                }

                val title = when (currentRoute) {
                    "settings" -> stringResource(id = R.string.settings)
                    "battery_history" -> stringResource(id = R.string.battery_history_title) // Define title for Battery History screen
                    "custom_tunable" -> stringResource(id = R.string.custom_tunable_title)
                    "app_profiles" -> "App Profiles"
                    "process_monitor" -> stringResource(id = R.string.process_monitor_title)
                    "permission_manager" -> stringResource(id = R.string.permission_manager_title)
                    "dexopt" -> "Dexopt"
                    "kernel_log" -> stringResource(id = R.string.kernel_log_title)
                    else -> stringResource(id = R.string.n0kz_kernel_manager) // Default title for home, tuning, misc
                }

                val showSettingsIcon = when (currentRoute) {
                    "home", "tuning", "misc" -> true
                    else -> false // Do not show for settings or other screens
                }
                
                // UnifiedTopAppBar logic: always show except for specific cases (currently none)
                val showUnifiedTopAppBar = true

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        if (showUnifiedTopAppBar) {
                            UnifiedTopAppBar(
                                title = title,
                                navController = navController,
                                showSettingsIcon = showSettingsIcon,
                                scrollBehavior = scrollBehavior,
                                isAmoledMode = isAmoledMode,
                                actions = {
                                    if (currentRoute == "kernel_log") {
                                        kernelLogViewModel?.let { viewModel ->
                                            val isSearchVisible by viewModel.isSearchVisible.collectAsStateWithLifecycle()
                                            val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
                                            val isMenuExpanded by viewModel.isMenuExpanded.collectAsStateWithLifecycle()

                                            if (isSearchVisible) {
                                                IconButton(onClick = { 
                                                    viewModel.setSearchVisible(false) 
                                                }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                                }
                                            } else {
                                                IconButton(onClick = { viewModel.setSearchVisible(true) }) {
                                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                                }
                                                
                                                IconButton(onClick = { viewModel.togglePause() }) {
                                                    Icon(
                                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                        contentDescription = if (isPaused) "Resume" else "Pause"
                                                    )
                                                }
                                                
                                                Box {
                                                    IconButton(onClick = { viewModel.setMenuExpanded(true) }) {
                                                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                                                    }
                                                    
                                                    DropdownMenu(
                                                        expanded = isMenuExpanded,
                                                        onDismissRequest = { viewModel.setMenuExpanded(false) },
                                                        modifier = Modifier.widthIn(min = 200.dp),
                                                        shape = RoundedCornerShape(28.dp)
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text(stringResource(R.string.kernel_log_clear)) },
                                                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                                            onClick = {
                                                                viewModel.clearLogs()
                                                                viewModel.setMenuExpanded(false)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text(stringResource(R.string.kernel_log_refresh)) },
                                                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                                            onClick = {
                                                                viewModel.loadLogs()
                                                                viewModel.setMenuExpanded(false)
                                                            }
                                                        )
                                                         DropdownMenuItem(
                                                            text = { Text(stringResource(R.string.kernel_log_export)) }, 
                                                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                                                            onClick = {
                                                                viewModel.triggerExport()
                                                                viewModel.setMenuExpanded(false)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Specific actions for other screens if needed
                                    }
                                }
                            )
                        }
                    },
                    floatingActionButton = {
                        when (currentRoute) {
                            "home" -> {
                                var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
                                BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

                                val fabMenuItems = remember {
                                    listOf(
                                        Triple("power_off", R.string.power_off, Icons.Filled.PowerSettingsNew),
                                        Triple("reboot_system", R.string.reboot_system, Icons.Filled.Refresh),
                                        Triple(
                                            "reboot_recovery",
                                            R.string.reboot_recovery,
                                            Icons.Filled.SettingsBackupRestore
                                        ),
                                        Triple("reboot_bootloader", R.string.reboot_bootloader, Icons.Filled.Build)
                                    )
                                }

                                FloatingActionButtonMenu(
                                    expanded = fabMenuExpanded,
                                    button = {
                                        ToggleFloatingActionButton(
                                            checked = fabMenuExpanded,
                                            onCheckedChange = { fabMenuExpanded = it },
                                            containerSize = { 72.dp },
                                        ) {
                                            val imageVector by remember {
                                                derivedStateOf {
                                                    if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.PowerSettingsNew
                                                }
                                            }
                                            Icon(
                                                painter = rememberVectorPainter(imageVector),
                                                contentDescription = stringResource(id = R.string.toggle_fab_menu),
                                                modifier = Modifier.animateIcon({ checkedProgress })
                                            )
                                        }
                                    }
                                ) {
                                    fabMenuItems.forEach { (action, textRes, icon) ->
                                        val command = when (action) {
                                            "power_off" -> "reboot -p"
                                            "reboot_recovery" -> "reboot recovery"
                                            "reboot_bootloader" -> "reboot bootloader"
                                            "reboot_system" -> "reboot"
                                            else -> ""
                                        }
                                        FloatingActionButtonMenuItem(
                                            onClick = {
                                                if (command.isNotEmpty()) {
                                                    lifecycleScope.launch {
                                                        try {
                                                            rootRepo.run(command)
                                                        } catch (e: Exception) {
                                                            // Handle error or log
                                                        }
                                                    }
                                                }
                                                fabMenuExpanded = false
                                            },
                                            icon = { Icon(icon, contentDescription = null) },
                                            text = { Text(text = stringResource(textRes)) },
                                        )
                                    }
                                }
                            }
                            "battery_history" if batteryHistoryFabVisible.value -> {
                                FloatingActionButton(
                                    onClick = { batteryHistoryFabAction.value?.invoke() },
                                    modifier = Modifier.size(72.dp) // Match HomeScreen FAB size
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear History")
                                }
                            }
                            "process_monitor" if processMonitorFabVisible.value -> {
                                FloatingActionButton(
                                    onClick = { processMonitorFabAction.value?.invoke() },
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            }
                            "custom_tunable" if customTunableFabVisible.value -> {
                                ExtendedFloatingActionButton(
                                    onClick = { customTunableFabAction.value?.invoke() },
                                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    text = { Text(stringResource(R.string.add_tunable)) }
                                )
                            }
                        }
                    },
                    floatingActionButtonPosition = FabPosition.End,
                    bottomBar = { BottomNavBar(navController, items, isAmoledMode = isAmoledMode) }
                ) { innerPadding ->
                    val isRootAvailable by mainViewModel.isRootAvailable.collectAsStateWithLifecycle()
                    
                    if (showKernelVerificationDialog) {
                        KernelVerificationDialog(onDismiss = { finish() })
                    }
                    if (showRootRequiredDialog) {
                        RootRequiredDialog(onDismiss = { 
                            finishAndRemoveTask()
                        })
                    }
                    // Only show permission dialog if device is rooted
                    if (showBatteryOptDialog && isRootAvailable == true) {
                        BatteryOptDialog(
                            onDismiss = {
                                // Only allow dismiss if we haven't exceeded retry limit
                                if (permissionDenialCount < MAX_PERMISSION_RETRIES) {
                                    showBatteryOptDialog = false
                                }
                            },
                            onConfirm = {
                                showBatteryOptDialog = false
                                batteryOptChecker.checkAndRequestPermissions(this@MainActivity)
                            },
                            onExit = { finish() },
                            showExitButton = permissionDenialCount >= MAX_PERMISSION_RETRIES
                        )
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(
                            "home",
                            enterTransition = { fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) },
                            exitTransition = { fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) },
                            popEnterTransition = { fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) },
                            popExitTransition = { fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) }
                        ) { HomeScreen(navController = navController) }
                        composable(
                            "tuning",
                            enterTransition = { fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) },
                            exitTransition = { fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) },
                            popEnterTransition = { fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) },
                            popExitTransition = { fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) }
                        ) { TuningScreen(navController = navController) }
                        composable(
                            "misc",
                            enterTransition = { fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) },
                            exitTransition = { fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) },
                            popEnterTransition = { fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) },
                            popExitTransition = { fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) }
                        ) { MiscScreen(navController = navController) }
                        composable(
                            "custom_tunable",
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        ) {
                             CustomTunableScreen(navController = navController)
                        }
                        composable(
                            "battery_history",
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        ) {
                            BatteryHistoryScreen(navController = navController)
                        }
                        composable(
                            "settings",
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        ) {
                            SettingsScreen(navController = navController)
                        }
                        composable(
                            "app_profiles",
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        ) {
                            AppProfilesScreen(navController = navController)
                        }
                        composable(
                            "process_monitor",
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        ) {
                            ProcessMonitorScreen(navController = navController)
                        }
                        composable(
                            "permission_manager",
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        ) {
                            PermissionManagerScreen(navController = navController)
                        }
                        composable(
                            "dexopt",
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        ) {
                            DexoptScreen(navController = navController)
                        }
                        composable(
                            "kernel_log",
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        ) {
                            val viewModel = hiltViewModel<KernelLogViewModel>()
                            // Use DisposableEffect to manage the hoisted state safely
                            DisposableEffect(viewModel) {
                                kernelLogViewModel = viewModel
                                onDispose {
                                    kernelLogViewModel = null
                                }
                            }
                            KernelLogScreen(navController = navController, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun checkAndHandlePermissions() {
        lifecycleScope.launch {
            if (rootRepo.checkRootFresh() && !batteryOptChecker.hasRequiredPermissions()) {
                showBatteryOptDialog = true
            }
        }
    }

    private fun maybeRequestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val alreadyPrompted = getSharedPreferences("perm_prefs", MODE_PRIVATE)
            .getBoolean("notif_prompted", false)
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!alreadyPrompted && !granted) {
            notifPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Check root status again in case user granted root access
        lifecycleScope.launch {
            if (!rootRepo.checkRootFresh()) {
                showRootRequiredDialog = true
            } else {
                mainViewModel.checkRootAndKernel()
                showRootRequiredDialog = false
                
                if (showKernelVerificationDialog) {
                    return@launch
                }
                
                if (!batteryOptChecker.hasRequiredPermissions()) {
                    permissionDenialCount++
                    showBatteryOptDialog = true
                } else {
                    permissionDenialCount = 0
                    showBatteryOptDialog = false
                }
            }
        }
    }
}
