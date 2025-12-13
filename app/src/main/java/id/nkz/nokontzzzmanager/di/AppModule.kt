package id.nkz.nokontzzzmanager.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.nkz.nokontzzzmanager.data.repository.RootRepository
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import id.nkz.nokontzzzmanager.data.repository.TuningRepository
import javax.inject.Singleton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import id.nkz.nokontzzzmanager.data.repository.ThermalRepository

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideTuningRepository(@ApplicationContext context: Context): TuningRepository = TuningRepository(context)

    @Provides
    @Singleton
    fun provideThermalRepository(@ApplicationContext context: Context, rootRepository: RootRepository): ThermalRepository =
        ThermalRepository(context, rootRepository)

    @Provides
    @Singleton
    fun provideRootRepository(): RootRepository = RootRepository()

    @Provides
    @Singleton
    fun provideSystemRepository(@ApplicationContext context: Context, tuningRepository: TuningRepository, rootRepository: RootRepository): SystemRepository =
        SystemRepository(context, tuningRepository, rootRepository)

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("settings") })
    }

    @Provides
    @Singleton
    fun provideBatteryHistoryDatabase(@ApplicationContext context: Context): id.nkz.nokontzzzmanager.data.database.BatteryHistoryDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            id.nkz.nokontzzzmanager.data.database.BatteryHistoryDatabase::class.java,
            "battery_history_db"
        )
        .fallbackToDestructiveMigration(true)
        .build()
    }

    @Provides
    @Singleton
    fun provideBatteryGraphDao(database: id.nkz.nokontzzzmanager.data.database.BatteryHistoryDatabase): id.nkz.nokontzzzmanager.data.database.BatteryGraphDao {
        return database.batteryGraphDao()
    }

    @Provides
    @Singleton
    fun provideBatteryGraphRepository(dao: id.nkz.nokontzzzmanager.data.database.BatteryGraphDao): id.nkz.nokontzzzmanager.data.repository.BatteryGraphRepository {
        return id.nkz.nokontzzzmanager.data.repository.BatteryGraphRepository(dao)
    }

    @Provides
    @Singleton
    fun provideAppProfileDao(database: id.nkz.nokontzzzmanager.data.database.BatteryHistoryDatabase): id.nkz.nokontzzzmanager.data.database.AppProfileDao {
        return database.appProfileDao()
    }

    @Provides
    @Singleton
    fun provideAppProfileRepository(dao: id.nkz.nokontzzzmanager.data.database.AppProfileDao): id.nkz.nokontzzzmanager.data.repository.AppProfileRepository {
        return id.nkz.nokontzzzmanager.data.repository.AppProfileRepository(dao)
    }
}