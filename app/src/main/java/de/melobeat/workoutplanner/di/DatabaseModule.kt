package de.melobeat.workoutplanner.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import de.melobeat.workoutplanner.data.RestTimerPreferencesRepository
import de.melobeat.workoutplanner.data.WorkoutDao
import de.melobeat.workoutplanner.data.WorkoutDatabase
import de.melobeat.workoutplanner.data.UserProfileRepository
import de.melobeat.workoutplanner.data.WorkoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RestTimerDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserProfileDataStore

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkoutDatabase {
        return WorkoutDatabase.getDatabase(context)
    }

    @Provides
    fun provideWorkoutDao(database: WorkoutDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        dao: WorkoutDao,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): WorkoutRepository = WorkoutRepository(dao, dispatcher)

    @Provides
    @Singleton
    @RestTimerDataStore
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("rest_timer_prefs") }
        )

    @Provides
    @Singleton
    @UserProfileDataStore
    fun provideUserProfileDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("user_profile_prefs") }
        )

    @Provides
    @Singleton
    fun provideRestTimerPreferencesRepository(
        @RestTimerDataStore dataStore: DataStore<Preferences>
    ): RestTimerPreferencesRepository = RestTimerPreferencesRepository(dataStore)

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        @UserProfileDataStore dataStore: DataStore<Preferences>
    ): UserProfileRepository = UserProfileRepository(dataStore)
}