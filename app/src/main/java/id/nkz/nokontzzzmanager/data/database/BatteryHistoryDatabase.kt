package id.nkz.nokontzzzmanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BatteryHistoryEntity::class, BatteryGraphEntry::class, AppProfileEntity::class, CustomTunableEntity::class], version = 10, exportSchema = false)
abstract class BatteryHistoryDatabase : RoomDatabase() {
    abstract fun batteryHistoryDao(): BatteryHistoryDao
    abstract fun batteryGraphDao(): BatteryGraphDao
    abstract fun appProfileDao(): AppProfileDao
    abstract fun customTunableDao(): CustomTunableDao
}
