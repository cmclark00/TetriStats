package com.accidentalproductions.tetristats

import android.app.Application
import androidx.room.Room
import com.accidentalproductions.tetristats.data.ScoreDatabase

class TetriStatsApplication : Application() {
    lateinit var database: ScoreDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            ScoreDatabase::class.java,
            "score_database"
        ).build()
    }
} 