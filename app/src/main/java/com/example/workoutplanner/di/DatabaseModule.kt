package com.example.workoutplanner.di

import android.content.Context
import com.example.workoutplanner.data.WorkoutDao
import com.example.workoutplanner.data.WorkoutDatabase
import com.example.workoutplanner.data.WorkoutRepository
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
}