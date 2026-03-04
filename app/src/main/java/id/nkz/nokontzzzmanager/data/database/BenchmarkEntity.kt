package id.nkz.nokontzzzmanager.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benchmarks")
data class BenchmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val durationMs: Long,
    val avgFps: Float,
    val fps1Low: Float,
    val fps01Low: Float,
    val jankCount: Int,
    val bigJankCount: Int,
    val frameTimeDataJson: String? = null // For storing frame time graph data
)
