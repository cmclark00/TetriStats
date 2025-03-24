package com.accidentalproductions.tetristats

import android.app.Application
import androidx.room.Room
import com.accidentalproductions.tetristats.data.ScalingFactorsManager
import com.accidentalproductions.tetristats.data.ScoreDatabase

class TetriStatsApplication : Application() {
    lateinit var database: ScoreDatabase
        private set
        
    lateinit var scalingFactorsManager: ScalingFactorsManager
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            // First try with migration
            database = Room.databaseBuilder(
                applicationContext,
                ScoreDatabase::class.java,
                "score_database"
            )
            .addMigrations(ScoreDatabase.MIGRATION_1_2)
            .build()
        } catch (e: Exception) {
            // Fallback: If migration fails, recreate the database
            // This is not ideal for production as it loses data,
            // but helps during development or if migration fails
            database = Room.databaseBuilder(
                applicationContext,
                ScoreDatabase::class.java,
                "score_database"
            )
            .fallbackToDestructiveMigration()
            .build()
        }
        
        // Initialize the ScalingFactorsManager
        scalingFactorsManager = ScalingFactorsManager(applicationContext)
    }
} 