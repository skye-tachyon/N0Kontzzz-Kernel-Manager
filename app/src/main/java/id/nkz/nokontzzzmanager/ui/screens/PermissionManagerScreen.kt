package id.nkz.nokontzzzmanager.ui.screens

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import id.nkz.nokontzzzmanager.R

import android.Manifest
import android.app.AppOpsManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import id.nkz.nokontzzzmanager.data.repository.RootRepository
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PermissionManagerViewModel @Inject constructor(
    private val rootRepository: RootRepository
) : ViewModel() {
    fun checkRoot(): Boolean = rootRepository.checkRootFresh()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionManagerScreen(
    navController: NavController,
    viewModel: PermissionManagerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val permissionsList = remember(context) { getRelevantPermissions(context, viewModel) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(permissionsList) { index, permissionInfo ->
            val shape = when {
                permissionsList.size == 1 -> RoundedCornerShape(24.dp)
                index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                index == permissionsList.size - 1 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                else -> RoundedCornerShape(8.dp)
            }
            PermissionItem(permissionInfo, shape)
        }
    }
}

@Composable
fun PermissionItem(permissionInfo: AppPermissionInfo, shape: RoundedCornerShape) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(permissionInfo.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(permissionInfo.descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            if (permissionInfo.isGranted) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.permission_granted),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(4.dp).size(24.dp)
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = stringResource(R.string.permission_denied),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(4.dp).size(24.dp)
                    )
                }
            }
        }
    }
}

data class AppPermissionInfo(
    val titleRes: Int,
    val descRes: Int,
    val isGranted: Boolean
)

private fun getRelevantPermissions(context: Context, viewModel: PermissionManagerViewModel): List<AppPermissionInfo> {
    val list = mutableListOf<AppPermissionInfo>()

    // 1. Root Access
    list.add(AppPermissionInfo(R.string.perm_root_title, R.string.perm_root_desc, viewModel.checkRoot()))

    // 2. Notifications (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        list.add(AppPermissionInfo(R.string.perm_notif_title, R.string.perm_notif_desc, granted))
    }

    // 3. Usage Stats
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    list.add(AppPermissionInfo(R.string.perm_usage_title, R.string.perm_usage_desc, mode == AppOpsManager.MODE_ALLOWED))

    // 4. File Storage (Android 11+ MANAGE_EXTERNAL_STORAGE or Legacy)
    val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        android.os.Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
    list.add(AppPermissionInfo(R.string.perm_storage_title, R.string.perm_storage_desc, storageGranted))

    // 5. Battery Optimization
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    list.add(AppPermissionInfo(R.string.perm_battery_opt_title, R.string.perm_battery_opt_desc, pm.isIgnoringBatteryOptimizations(context.packageName)))

    return list
}
