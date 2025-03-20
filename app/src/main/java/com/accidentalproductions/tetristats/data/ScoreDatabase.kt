package com.accidentalproductions.tetristats.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.accidentalproductions.tetristats.TetriStatsApplication
import com.accidentalproductions.tetristats.util.Converters

@Database(entities = [Score::class], version = 1)
@TypeConverters(Converters::class)
abstract class ScoreDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    companion object {
        fun getDatabase(context: Context): ScoreDatabase {
            return (context.applicationContext as TetriStatsApplication).database
        }
    }
} 