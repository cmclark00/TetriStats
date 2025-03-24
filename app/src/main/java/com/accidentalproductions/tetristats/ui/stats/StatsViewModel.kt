package com.accidentalproductions.tetristats.ui.stats

import android.app.Application
import androidx.lifecycle.*
import com.accidentalproductions.tetristats.data.Score
import com.accidentalproductions.tetristats.data.ScoreDatabase

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ScoreDatabase.getDatabase(application)
    private val scoreDao = database.scoreDao()

    val gamesWithScores = scoreDao.getGamesWithScores()
    
    private val _selectedGame = MutableLiveData<String>()
    val selectedGame: LiveData<String> = _selectedGame

    val filteredScores: LiveData<List<Score>> = _selectedGame.switchMap { game ->
        scoreDao.getScoresForGame(game)
    }

    val scoresByDate: LiveData<List<Score>> = _selectedGame.switchMap { game ->
        scoreDao.getScoresForGameByDate(game)
    }

    val averageScore: LiveData<Double> = _selectedGame.switchMap { game ->
        scoreDao.getAverageScore(game)
    }

    val highScore: LiveData<Int> = _selectedGame.switchMap { game ->
        scoreDao.getHighScore(game)
    }

    fun setSelectedGame(game: String) {
        _selectedGame.value = game
    }
}

class StatsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 