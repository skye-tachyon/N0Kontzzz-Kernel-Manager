package id.nkz.nokontzzzmanager.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_profiles")
data class AppProfileEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val performanceMode: String = "Balanced", // "Balanced" or "Performance"
    val kgslSkipZeroing: Boolean = false,
    val bypassCharging: Boolean = false,
    val allowDirtyPte: Boolean = false,
    val cpuConfigJson: String? = null,
    val isEnabled: Boolean = true
) {
    fun getCpuConfig(): id.nkz.nokontzzzmanager.data.model.CpuProfileConfig {
        if (cpuConfigJson.isNullOrBlank()) return id.nkz.nokontzzzmanager.data.model.CpuProfileConfig()
        return try {
            kotlinx.serialization.json.Json.decodeFromString(cpuConfigJson)
        } catch (e: Exception) {
            id.nkz.nokontzzzmanager.data.model.CpuProfileConfig()
        }
    }
}
