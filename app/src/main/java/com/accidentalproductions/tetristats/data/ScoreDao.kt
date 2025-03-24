package com.accidentalproductions.tetristats.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM scores")
    fun getAllScores(): LiveData<List<Score>>

    @Query("SELECT * FROM scores WHERE gameVersion = :gameVersion")
    fun getScoresForGame(gameVersion: String): LiveData<List<Score>>

    @Query("SELECT * FROM scores WHERE gameVersion = :gameVersion ORDER BY dateRecorded ASC")
    fun getScoresForGameByDate(gameVersion: String): LiveData<List<Score>>

    @Query("SELECT DISTINCT gameVersion FROM scores")
    fun getGamesWithScores(): LiveData<List<String>>

    @Query("SELECT COUNT(*) FROM scores")
    fun getTotalScoreCount(): LiveData<Int>

    @Query("SELECT AVG(scoreValue) FROM scores WHERE gameVersion = :gameVersion")
    fun getAverageScore(gameVersion: String): LiveData<Double>

    @Query("SELECT MAX(scoreValue) FROM scores WHERE gameVersion = :gameVersion")
    fun getHighScore(gameVersion: String): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: Score)

    @Delete
    suspend fun delete(score: Score)

    @Query("DELETE FROM scores")
    suspend fun deleteAllScores()
} 