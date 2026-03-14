package com.example.workoutplanner

import android.app.Application
import com.example.workoutplanner.data.WorkoutDatabase

class WorkoutApplication : Application() {
    val database: WorkoutDatabase by lazy { WorkoutDatabase.getDatabase(this) }
}
