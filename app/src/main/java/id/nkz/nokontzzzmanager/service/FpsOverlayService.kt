package id.nkz.nokontzzzmanager.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import id.nkz.nokontzzzmanager.data.database.BenchmarkEntity
import id.nkz.nokontzzzmanager.data.repository.BenchmarkRepository
import id.nkz.nokontzzzmanager.data.repository.GameRepository
import id.nkz.nokontzzzmanager.manager.FpsMonitorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class FpsOverlayService : Service(), androidx.lifecycle.LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    @Inject
    lateinit var fpsMonitorManager: FpsMonitorManager

    @Inject
    lateinit var benchmarkRepository: BenchmarkRepository

    @Inject
    lateinit var gameRepository: GameRepository

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val CHANNEL_ID = "fps_overlay_service_channel"
    private val NOTIFICATION_ID = 1003

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        createNotificationChannel()
        startForegroundService()
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showOverlay()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "FPS Meter"
            val descriptionText = "Shows FPS overlay during gameplay"
            val importance = android.app.NotificationManager.IMPORTANCE_LOW
            val channel = android.app.NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FPS Meter Active")
            .setContentText("Overlay is being displayed")
            .setSmallIcon(id.nkz.nokontzzzmanager.R.drawable.ic_notification)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // For FPS overlay, we might use SPECIAL_USE or just omit type if not strictly required by policy, 
                // but Android 14+ requires it for foreground services.
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("FpsOverlay", "Failed to start foreground service", e)
        }
    }

    private fun showOverlay() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FpsOverlayService)
            setViewTreeViewModelStoreOwner(this@FpsOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FpsOverlayService)
            
            setContent {
                MaterialTheme {
                    FpsOverlayContent(
                        fpsMonitorManager = fpsMonitorManager,
                        onToggleBenchmark = {
                            toggleBenchmarking()
                        }
                    )
                }
            }
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        try {
            windowManager?.addView(composeView, params)
        } catch (e: Exception) {
            android.util.Log.e("FpsOverlay", "Error adding overlay view", e)
            stopSelf()
        }
    }

    private fun toggleBenchmarking() {
        val currentData = fpsMonitorManager.fpsData.value
        if (currentData.isBenchmarking) {
            val result = fpsMonitorManager.stopBenchmarking()
            if (result != null) {
                saveBenchmarkResult(result)
            }
        } else {
            fpsMonitorManager.startBenchmarking()
        }
    }

    private fun saveBenchmarkResult(result: FpsMonitorManager.BenchmarkResult) {
        serviceScope.launch(Dispatchers.IO) {
            val game = gameRepository.getGameByPackageName(result.packageName).first()
            val appName = game?.appName ?: result.packageName
            
            val entity = BenchmarkEntity(
                packageName = result.packageName,
                appName = appName,
                timestamp = result.startTime,
                durationMs = result.durationMs,
                avgFps = result.avgFps,
                fps1Low = result.fps1Low,
                fps01Low = result.fps01Low,
                jankCount = result.jankCount,
                bigJankCount = result.bigJankCount,
                frameTimeDataJson = Json.encodeToString(result.frameTimes)
            )
            benchmarkRepository.insertBenchmark(entity)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure the service remains in foreground if it was already running
        startForegroundService()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        composeView?.let { windowManager?.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FpsOverlayContent(
    fpsMonitorManager: FpsMonitorManager,
    onToggleBenchmark: () -> Unit
) {
    val fpsData by fpsMonitorManager.fpsData.collectAsState()

    Box(
        modifier = Modifier
            .background(Color(0xBB000000), shape = RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Benchmark Toggle Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (fpsData.isBenchmarking) Color.Red.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
                    .clickable { onToggleBenchmark() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (fpsData.isBenchmarking) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = "Benchmark",
                    tint = if (fpsData.isBenchmarking) Color.Red else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${fpsData.currentFps.toInt()}", color = Color.Green, style = MaterialTheme.typography.titleMedium)
                    Text(text = " FPS", color = Color.Green.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
                
                if (fpsData.isBenchmarking) {
                    val seconds = (fpsData.currentBenchmarkDuration / 1000) % 60
                    val minutes = (fpsData.currentBenchmarkDuration / (1000 * 60)) % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "1%: ${fpsData.fps1Low.toInt()}", color = Color.Yellow, style = MaterialTheme.typography.labelSmall)
                        Text(text = "FT: ${String.format("%.1f", fpsData.frameTimeMs)}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
