package com.accidentalproductions.tetristats.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.accidentalproductions.tetristats.TetriStatsApplication
import com.accidentalproductions.tetristats.util.Converters

@Database(entities = [Score::class], version = 2)
@TypeConverters(Converters::class)
abstract class ScoreDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    companion object {
        // Migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the mediaUri column to the scores table
                database.execSQL("ALTER TABLE scores ADD COLUMN mediaUri TEXT")
            }
        }

        fun getDatabase(context: Context): ScoreDatabase {
            return (context.applicationContext as TetriStatsApplication).database
        }
    }
} 