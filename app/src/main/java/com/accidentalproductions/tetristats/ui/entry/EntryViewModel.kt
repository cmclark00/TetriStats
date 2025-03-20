package com.accidentalproductions.tetristats.ui.entry

import android.app.Application
import androidx.lifecycle.*
import com.accidentalproductions.tetristats.data.Score
import com.accidentalproductions.tetristats.data.ScoreDatabase
import com.accidentalproductions.tetristats.data.ScalingFactors
import kotlinx.coroutines.launch

class EntryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ScoreDatabase.getDatabase(application)
    private val scoreDao = database.scoreDao()

    val gamesWithScores = scoreDao.getGamesWithScores()
    
    private val _selectedFromGame = MutableLiveData<String>()
    private val _selectedScore = MutableLiveData<Score>()
    private val _selectedToGame = MutableLiveData<String>()
    private val _convertedScore = MutableLiveData<Int>()

    val convertedScore: LiveData<Int> = _convertedScore

    fun getScoresForGame(gameVersion: String): LiveData<List<Score>> {
        return scoreDao.getScoresForGame(gameVersion)
    }

    fun setSelectedFromGame(game: String) {
        _selectedFromGame.value = game
    }

    fun setSelectedScore(score: Score) {
        _selectedScore.value = score
    }

    fun setSelectedToGame(game: String) {
        _selectedToGame.value = game
    }

    fun convertScore() {
        val fromGame = _selectedFromGame.value
        val score = _selectedScore.value
        val toGame = _selectedToGame.value

        if (fromGame != null && score != null && toGame != null) {
            val convertedScore = ScalingFactors.convertScore(fromGame, toGame, score.scoreValue)
            _convertedScore.value = convertedScore
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