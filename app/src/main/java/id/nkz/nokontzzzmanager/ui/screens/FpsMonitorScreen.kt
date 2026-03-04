package id.nkz.nokontzzzmanager.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.GameEntity
import id.nkz.nokontzzzmanager.ui.components.IndeterminateExpressiveLoadingIndicator
import id.nkz.nokontzzzmanager.viewmodel.FpsMonitorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FpsMonitorScreen(
    navController: NavController,
    viewModel: FpsMonitorViewModel = hiltViewModel()
) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    val sheetState = rememberModalBottomSheetState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.loadInstalledApps()
                    showAddDialog = true
                },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Game")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(
                    bottom = padding.calculateBottomPadding(),
                    start = padding.calculateStartPadding(LocalLayoutDirection.current),
                    end = padding.calculateEndPadding(LocalLayoutDirection.current),
                    top = 0.dp
                )
                .fillMaxSize()
        ) {
            if (games.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No games registered. Tap + to add one.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(games) { index, game ->
                        val shape = when {
                            games.size == 1 -> RoundedCornerShape(24.dp)
                            index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                            index == games.lastIndex -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                            else -> RoundedCornerShape(8.dp)
                        }
                        GameItem(
                            game = game,
                            onDelete = { viewModel.removeGame(it) },
                            onToggleBenchmark = { g, enabled -> viewModel.toggleBenchmark(g, enabled) },
                            cardShape = shape
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            ModalBottomSheet(
                onDismissRequest = {
                    showAddDialog = false
                    viewModel.onSearchQueryChanged("")
                },
                sheetState = sheetState
            ) {
                AppPickerSheet(
                    filteredApps = filteredApps,
                    isLoading = isLoadingApps,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onDismiss = {
                        showAddDialog = false
                        viewModel.onSearchQueryChanged("")
                    },
                    onAppSelected = { appInfo ->
                        viewModel.addGame(appInfo)
                        showAddDialog = false
                        viewModel.onSearchQueryChanged("")
                    }
                )
            }
        }
    }
}

@Composable
fun GameItem(
    game: GameEntity,
    onDelete: (GameEntity) -> Unit,
    onToggleBenchmark: (GameEntity, Boolean) -> Unit,
    cardShape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    val context = LocalContext.current
    val icon = remember(game.packageName) {
        try {
            context.packageManager.getApplicationIcon(game.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = cardShape
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
                Box(modifier = Modifier.size(48.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = game.appName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = game.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = game.isBenchmarkEnabled,
                onCheckedChange = { onToggleBenchmark(game, it) }
            )

            IconButton(onClick = { onDelete(game) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
