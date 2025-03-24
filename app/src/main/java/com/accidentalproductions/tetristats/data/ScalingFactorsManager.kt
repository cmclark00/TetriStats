package com.accidentalproductions.tetristats.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages the learning and persistence of scaling factors based on user input
 */
class ScalingFactorsManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // In-memory cache of learned factors
    private var learnedFactors: MutableMap<String, MutableMap<String, LearningData>> = loadLearnedFactors()
    
    /**
     * Updates the scaling factor based on a new sample provided by the user
     * @param fromGame The source game
     * @param toGame The target game
     * @param fromScore The original score
     * @param toScore The user-provided equivalent score
     */
    suspend fun updateScalingFactor(fromGame: String, toGame: String, fromScore: Int, toScore: Int) {
        withContext(Dispatchers.IO) {
            // Calculate the actual scaling factor from this sample
            val actualFactor = toScore.toDouble() / fromScore.toDouble()
            
            // Get or create learning data for this game conversion
            val gameFactors = learnedFactors.getOrPut(fromGame) { mutableMapOf() }
            val learningData = gameFactors.getOrPut(toGame) { LearningData() }
            
            // Determine which bucket this score falls into
            val bucket = when {
                fromScore < 100000 -> ScoreBucket.LOW
                fromScore < 500000 -> ScoreBucket.MID
                else -> ScoreBucket.HIGH
            }
            
            // Update the appropriate bucket
            when (bucket) {
                ScoreBucket.LOW -> {
                    learningData.lowScoreSamples++
                    learningData.lowScoreTotal += actualFactor
                }
                ScoreBucket.MID -> {
                    learningData.midScoreSamples++
                    learningData.midScoreTotal += actualFactor
                }
                ScoreBucket.HIGH -> {
                    learningData.highScoreSamples++
                    learningData.highScoreTotal += actualFactor
                }
            }
            
            // Save the updated data
            saveLearnedFactors()
        }
    }
    
    /**
     * Gets the learned scaling factor for a conversion, falling back to default if no samples
     */
    fun getLearnedScalingFactor(fromGame: String, toGame: String, score: Int): Double {
        // Check if we have learned data for this conversion
        val learningData = learnedFactors[fromGame]?.get(toGame)
        
        // If no learning data, fall back to default
        if (learningData == null) {
            return ScalingFactors.getScalingFactor(fromGame, toGame, score)
        }
        
        // Determine which bucket this score falls into
        return when {
            score < 100000 -> {
                if (learningData.lowScoreSamples > 0) {
                    learningData.lowScoreTotal / learningData.lowScoreSamples
                } else {
                    ScalingFactors.getScalingFactor(fromGame, toGame, score)
                }
            }
            score < 500000 -> {
                if (learningData.midScoreSamples > 0) {
                    learningData.midScoreTotal / learningData.midScoreSamples
                } else {
                    ScalingFactors.getScalingFactor(fromGame, toGame, score)
                }
            }
            else -> {
                if (learningData.highScoreSamples > 0) {
                    learningData.highScoreTotal / learningData.highScoreSamples
                } else {
                    ScalingFactors.getScalingFactor(fromGame, toGame, score)
                }
            }
        }
    }
    
    /**
     * Gets the number of samples collected for this game conversion
     */
    fun getSampleCount(fromGame: String, toGame: String): Int {
        val learningData = learnedFactors[fromGame]?.get(toGame) ?: return 0
        return learningData.lowScoreSamples + learningData.midScoreSamples + learningData.highScoreSamples
    }
    
    /**
     * Resets all learned scaling factors
     */
    suspend fun resetAllFactors() {
        withContext(Dispatchers.IO) {
            learnedFactors.clear()
            saveLearnedFactors()
        }
    }
    
    /**
     * Resets learned scaling factors for a specific game conversion
     */
    suspend fun resetFactors(fromGame: String, toGame: String) {
        withContext(Dispatchers.IO) {
            learnedFactors[fromGame]?.remove(toGame)
            saveLearnedFactors()
        }
    }
    
    private fun loadLearnedFactors(): MutableMap<String, MutableMap<String, LearningData>> {
        val json = prefs.getString(KEY_LEARNED_FACTORS, null) ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<String, MutableMap<String, LearningData>>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            mutableMapOf()
        }
    }
    
    private fun saveLearnedFactors() {
        val json = gson.toJson(learnedFactors)
        prefs.edit().putString(KEY_LEARNED_FACTORS, json).apply()
    }
    
    /**
     * Data class to track learning progress for a specific game conversion
     */
    data class LearningData(
        var lowScoreSamples: Int = 0,
        var lowScoreTotal: Double = 0.0,
        var midScoreSamples: Int = 0,
        var midScoreTotal: Double = 0.0,
        var highScoreSamples: Int = 0,
        var highScoreTotal: Double = 0.0
    )
    
    enum class ScoreBucket {
        LOW, MID, HIGH
    }
    
    companion object {
        private const val PREFS_NAME = "ScalingFactorsPrefs"
        private const val KEY_LEARNED_FACTORS = "learned_factors"
    }
} 