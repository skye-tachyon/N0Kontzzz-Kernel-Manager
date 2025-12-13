package id.nkz.nokontzzzmanager.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import id.nkz.nokontzzzmanager.data.database.AppProfileEntity
import id.nkz.nokontzzzmanager.viewmodel.AppInfo
import id.nkz.nokontzzzmanager.viewmodel.AppProfilesViewModel
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfilesScreen(
    navController: NavController,
    viewModel: AppProfilesViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<AppProfileEntity?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        if (!viewModel.hasUsageStatsPermission()) {
            showPermissionDialog = true
        }
        // Ensure service is running if permissions are granted
        if (viewModel.hasUsageStatsPermission()) {
            viewModel.toggleService(true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                viewModel.loadInstalledApps()
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add App Profile")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (showPermissionDialog) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.padding(16.dp).clickable {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            "Usage Stats permission required for App Profiles to work. Tap to enable.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (profiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No profiles configured. Tap + to add one.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(profiles) { profile ->
                        AppProfileItem(
                            profile = profile,
                            onEdit = { profileToEdit = it },
                            onDelete = { viewModel.deleteProfile(it) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AppPickerSheet(
            installedApps = installedApps,
            isLoading = isLoadingApps,
            onDismiss = { showAddDialog = false },
            onAppSelected = { appInfo ->
                viewModel.addProfile(appInfo)
                showAddDialog = false
                // Automatically open edit dialog after adding
                // We need to fetch the newly added profile or just create a temp one
                // For simplicity, we just add it with defaults. The user can edit it from the list.
            }
        )
    }

    if (profileToEdit != null) {
        AppProfileConfigDialog(
            profile = profileToEdit!!,
            onDismiss = { profileToEdit = null },
            onSave = { updatedProfile ->
                viewModel.updateProfile(updatedProfile)
                profileToEdit = null
            }
        )
    }
}

@Composable
fun AppProfileItem(
    profile: AppProfileEntity,
    onEdit: (AppProfileEntity) -> Unit,
    onDelete: (AppProfileEntity) -> Unit
) {
    val context = LocalContext.current
    // Load icon helper
    val icon = remember(profile.packageName) {
        try {
            context.packageManager.getApplicationIcon(profile.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(profile) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (icon != null) {
                Image(
                    painter = rememberDrawablePainter(icon),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(modifier = Modifier.size(48.dp)) // Placeholder
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.appName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${profile.performanceMode} • KGSL: ${if(profile.kgslSkipZeroing) "On" else "Off"} • Bypass: ${if(profile.bypassCharging) "On" else "Off"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = { onDelete(profile) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    installedApps: List<AppInfo>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAppSelected: (AppInfo) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Select App", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(installedApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             // Icon for picker
                            val context = LocalContext.current
                            val icon = remember(app.packageName) {
                                try {
                                    context.packageManager.getApplicationIcon(app.packageName)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (icon != null) {
                                Image(
                                    painter = rememberDrawablePainter(icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(app.appName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppProfileConfigDialog(
    profile: AppProfileEntity,
    onDismiss: () -> Unit,
    onSave: (AppProfileEntity) -> Unit
) {
    var performanceMode by remember { mutableStateOf(profile.performanceMode) }
    var kgslSkipZeroing by remember { mutableStateOf(profile.kgslSkipZeroing) }
    var bypassCharging by remember { mutableStateOf(profile.bypassCharging) }
    var isEnabled by remember { mutableStateOf(profile.isEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(profile.appName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Enable/Disable Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Profile")
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                }
                
                HorizontalDivider()

                // Performance Mode
                Text("Performance Mode", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = performanceMode == "Balanced",
                        onClick = { performanceMode = "Balanced" }
                    )
                    Text("Balanced")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = performanceMode == "Performance",
                        onClick = { performanceMode = "Performance" }
                    )
                    Text("Performance")
                }

                // KGSL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("KGSL Skip Pool Zeroing")
                    Switch(checked = kgslSkipZeroing, onCheckedChange = { kgslSkipZeroing = it })
                }

                // Bypass Charging
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bypass Charging")
                    Switch(checked = bypassCharging, onCheckedChange = { bypassCharging = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    profile.copy(
                        performanceMode = performanceMode,
                        kgslSkipZeroing = kgslSkipZeroing,
                        bypassCharging = bypassCharging,
                        isEnabled = isEnabled
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
