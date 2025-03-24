package com.accidentalproductions.tetristats.ui.entry

import android.app.Application
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
        "Apotris"
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
        viewModelScope.launch {
            val scoreCount = totalScoreCount.value ?: 0
            val gameCount = gamesWithScores.value?.size ?: 0
            
            // Only show conversions if there are at least 3 scores across at least 2 games
            _showConversion.postValue(scoreCount >= 3 && gameCount >= 2)
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
            _lastSubmittedGame.postValue(gameVersion)
            _lastSubmittedScore.postValue(score)
            
            // Check if we should show conversions
            checkConversionCriteria()
            
            // Only generate equivalent scores if we meet the criteria
            if (_showConversion.value == true) {
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
        
        _equivalentScores.postValue(equivalents)
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