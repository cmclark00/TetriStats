package com.accidentalproductions.tetristats.ui.entry

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.accidentalproductions.tetristats.TetriStatsApplication
import com.accidentalproductions.tetristats.data.Score
import com.accidentalproductions.tetristats.data.ScoreDatabase
import com.accidentalproductions.tetristats.data.ScalingFactors
import com.accidentalproductions.tetristats.data.ScalingFactorsManager
import kotlinx.coroutines.launch

class EntryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ScoreDatabase.getDatabase(application)
    private val scoreDao = database.scoreDao()
    private val scalingFactorsManager = (application as TetriStatsApplication).scalingFactorsManager

    // All games list for reference
    private val allGames = listOf(
        "NES Tetris",
        "Game Boy Tetris",
        "Tetris DX",
        "Tetris DS",
        "Tetris Effect",
        "Rosy Retrospection DX",
        "Apotris",
        "Modretro Tetris",
        "Tetris Mobile"
    )
    
    // Track user played games and score counts
    val gamesWithScores = scoreDao.getGamesWithScores()
    val totalScoreCount = scoreDao.getTotalScoreCount()
    
    // For auto-analysis
    private val _lastSubmittedGame = MutableLiveData<String>()
    private val _lastSubmittedScore = MutableLiveData<Int>()
    private val _equivalentScores = MutableLiveData<List<EquivalentScore>>()
    private val _showConversion = MutableLiveData<Boolean>(false)
    
    // Current game selection for learning
    private val _selectedEquivalentGame = MutableLiveData<String>()
    
    val equivalentScores: LiveData<List<EquivalentScore>> = _equivalentScores
    val lastSubmittedGame: LiveData<String> = _lastSubmittedGame
    val lastSubmittedScore: LiveData<Int> = _lastSubmittedScore
    val showConversion: LiveData<Boolean> = _showConversion
    
    init {
        // Set up observers to update conversion criteria whenever relevant data changes
        gamesWithScores.observeForever { 
            checkConversionCriteria() 
        }
        
        totalScoreCount.observeForever {
            checkConversionCriteria()
        }
    }

    fun getScoresForGame(gameVersion: String): LiveData<List<Score>> {
        return scoreDao.getScoresForGame(gameVersion)
    }
    
    fun setSelectedEquivalentGame(game: String) {
        _selectedEquivalentGame.value = game
    }

    /**
     * Check if we should show conversions based on score count and game count
     */
    fun checkConversionCriteria() {
        val scoreCount = totalScoreCount.value ?: 0
        val gameCount = gamesWithScores.value?.size ?: 0
        
        // Only show conversions if there are at least 3 scores across at least 2 games
        val shouldShow = scoreCount >= 3 && gameCount >= 2
        
        // For debugging
        Log.d("TetriStats", "Checking conversion criteria: scores=$scoreCount, games=$gameCount, shouldShow=$shouldShow")
        
        _showConversion.postValue(shouldShow)
    }
    
    /**
     * Force refresh the equivalent scores - use this to ensure UI has latest values
     */
    fun refreshEquivalentScores(fromGame: String, score: Int) {
        // Only refresh if conversions should be showing
        if (_showConversion.value == true) {
            Log.d("TetriStats", "Refreshing equivalent scores for $fromGame score $score")
            generateEquivalentScores(fromGame, score)
        }
    }

    fun insertScore(
        gameVersion: String,
        score: Int,
        startLevel: Int?,
        endLevel: Int?,
        linesCleared: Int?
    ) {
        viewModelScope.launch {
            val newScore = Score(
                gameVersion = gameVersion,
                scoreValue = score,
                startLevel = startLevel,
                endLevel = endLevel,
                linesCleared = linesCleared
            )
            scoreDao.insert(newScore)
            
            // After inserting, update the last submitted values
            _lastSubmittedGame.value = gameVersion  // Use immediate value change instead of postValue
            _lastSubmittedScore.value = score      // Use immediate value change instead of postValue
            
            // Immediately check conversion criteria with current values
            checkConversionCriteria()
            
            // Immediate refresh regardless if we just reached the criteria threshold
            if (totalScoreCount.value ?: 0 >= 3 && (gamesWithScores.value?.size ?: 0) >= 2) {
                generateEquivalentScores(gameVersion, score)
            }
        }
    }
    
    /**
     * Generates equivalent scores in played games based on the submitted score
     */
    private fun generateEquivalentScores(fromGame: String, score: Int) {
        val playedGames = gamesWithScores.value ?: listOf()
        val equivalents = mutableListOf<EquivalentScore>()
        
        // Generate equivalent scores for played games except the source game
        for (game in playedGames) {
            if (game != fromGame) {
                // Get the learned scaling factor
                val factor = scalingFactorsManager.getLearnedScalingFactor(fromGame, game, score)
                val equivalentScore = (score * factor).toInt()
                val sampleCount = scalingFactorsManager.getSampleCount(fromGame, game)
                
                equivalents.add(
                    EquivalentScore(
                        gameName = game,
                        score = equivalentScore,
                        sampleCount = sampleCount,
                        usesDynamicFactor = sampleCount > 0
                    )
                )
            }
        }
        
        // Use setValue for immediate update on main thread rather than postValue
        _equivalentScores.value = equivalents
    }
    
    /**
     * Add a learning sample with an equivalent score in another game
     */
    fun addEquivalentScore(equivalentScore: Int) {
        val fromGame = _lastSubmittedGame.value
        val originalScore = _lastSubmittedScore.value
        val toGame = _selectedEquivalentGame.value

        if (fromGame != null && originalScore != null && toGame != null) {
            viewModelScope.launch {
                scalingFactorsManager.updateScalingFactor(fromGame, toGame, originalScore, equivalentScore)
                
                // Regenerate the equivalent scores to update the UI
                generateEquivalentScores(fromGame, originalScore)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Remove our observers to prevent leaks
        gamesWithScores.removeObserver { checkConversionCriteria() }
        totalScoreCount.removeObserver { checkConversionCriteria() }
    }
}

class EntryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EntryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EntryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 