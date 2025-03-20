package com.accidentalproductions.tetristats.ui.history

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.accidentalproductions.tetristats.data.Score
import com.accidentalproductions.tetristats.data.ScoreDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ScoreDatabase.getDatabase(application)
    private val scoreDao = database.scoreDao()

    val allScores = scoreDao.getAllScores()
    
    private val _exportResult = MutableLiveData<ExportResult>()
    val exportResult: LiveData<ExportResult> = _exportResult
    
    private val _mediaSaveResult = MutableLiveData<MediaSaveResult?>()
    val mediaSaveResult: LiveData<MediaSaveResult?> = _mediaSaveResult
    
    // For delete and undo functionality
    private val _deleteResult = MutableLiveData<DeleteResult?>()
    val deleteResult: LiveData<DeleteResult?> = _deleteResult
    
    // Keep track of recently deleted score for undo
    private var lastDeletedScore: Score? = null
    
    // For media removal
    private val _mediaRemoveResult = MutableLiveData<MediaRemoveResult?>()
    val mediaRemoveResult: LiveData<MediaRemoveResult?> = _mediaRemoveResult
    
    fun exportScoresToCsv() {
        viewModelScope.launch {
            try {
                val scores = allScores.value ?: emptyList()
                if (scores.isEmpty()) {
                    _exportResult.postValue(ExportResult.Error("No scores to export"))
                    return@launch
                }
                
                val csvContent = generateCsvContent(scores)
                val uri = saveCsvFile(csvContent)
                _exportResult.postValue(ExportResult.Success(uri))
            } catch (e: Exception) {
                _exportResult.postValue(ExportResult.Error("Export failed: ${e.message}"))
            }
        }
    }
    
    fun saveMediaForScore(scoreId: Long, mediaUri: Uri) {
        viewModelScope.launch {
            try {
                // Copy the media to our app's internal storage to ensure it's persisted
                val savedUri = copyMediaToAppStorage(mediaUri)
                
                // Update the score with the media URI
                val score = getScoreById(scoreId)
                if (score != null) {
                    val updatedScore = score.copy(mediaUri = savedUri.toString())
                    updateScore(updatedScore)
                    _mediaSaveResult.postValue(MediaSaveResult.Success)
                } else {
                    _mediaSaveResult.postValue(MediaSaveResult.Error("Score not found"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _mediaSaveResult.postValue(MediaSaveResult.Error(e.message ?: "Unknown error"))
            }
        }
    }
    
    private suspend fun getScoreById(scoreId: Long): Score? = withContext(Dispatchers.IO) {
        val scores = allScores.value ?: emptyList()
        return@withContext scores.find { it.id == scoreId }
    }
    
    private suspend fun updateScore(score: Score) = withContext(Dispatchers.IO) {
        scoreDao.insert(score) // Using insert with REPLACE strategy
    }
    
    private suspend fun copyMediaToAppStorage(sourceUri: Uri): Uri = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val contentResolver = context.contentResolver
        
        // Determine file extension
        val mimeType = contentResolver.getType(sourceUri) ?: "application/octet-stream"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: 
                        if (mimeType.startsWith("image/")) "jpg" else "mp4"
        
        // Create a unique filename
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "media_${timestamp}_${UUID.randomUUID()}.$extension"
        
        // Create file in app's private storage
        val mediaDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "tetris_media")
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }
        
        val destFile = File(mediaDir, fileName)
        
        // Copy the content
        contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("Failed to open input stream")
        
        // Return a URI that can be used by our app
        return@withContext FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            destFile
        )
    }
    
    private fun generateCsvContent(scores: List<Score>): String {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val sb = StringBuilder()
        
        // Header
        sb.appendLine("ID,Game,Score,Start Level,End Level,Lines Cleared,Date,Media")
        
        // Data
        scores.forEach { score ->
            sb.appendLine(
                "${score.id}," +
                "\"${score.gameVersion}\"," +
                "${score.scoreValue}," +
                "${score.startLevel ?: ""}," +
                "${score.endLevel ?: ""}," +
                "${score.linesCleared ?: ""}," +
                "${score.dateRecorded.let { dateFormat.format(Date(it)) }}," +
                "${score.mediaUri ?: ""}"
            )
        }
        
        return sb.toString()
    }
    
    private suspend fun saveCsvFile(content: String): Uri = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "tetris_scores_$timestamp.csv"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                contentValues
            ) ?: throw IOException("Failed to create new MediaStore record")
            
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray())
            } ?: throw IOException("Failed to open output stream")
            
            return@withContext uri
        } else {
            // For older Android versions
            val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: throw IOException("Failed to access Documents directory")
            
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }
            
            val file = File(documentsDir, fileName)
            FileOutputStream(file).use { stream ->
                stream.write(content.toByteArray())
            }
            
            return@withContext FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        }
    }
    
    fun deleteScore(scoreId: Long) {
        viewModelScope.launch {
            try {
                val score = getScoreById(scoreId)
                if (score != null) {
                    // Store for potential undo
                    lastDeletedScore = score
                    
                    // Delete from database
                    scoreDao.delete(score)
                    
                    _deleteResult.postValue(DeleteResult.Success("Score deleted. Tap UNDO to restore."))
                } else {
                    _deleteResult.postValue(DeleteResult.Error("Score not found"))
                }
            } catch (e: Exception) {
                _deleteResult.postValue(DeleteResult.Error("Delete failed: ${e.message}"))
            }
        }
    }
    
    fun undoDelete() {
        viewModelScope.launch {
            try {
                lastDeletedScore?.let { score ->
                    // Reinsert the score
                    scoreDao.insert(score)
                    
                    _deleteResult.postValue(DeleteResult.UndoSuccess("Score restored"))
                    
                    // Clear the stored score
                    lastDeletedScore = null
                } ?: run {
                    _deleteResult.postValue(DeleteResult.Error("No score to restore"))
                }
            } catch (e: Exception) {
                _deleteResult.postValue(DeleteResult.Error("Restore failed: ${e.message}"))
            }
        }
    }
    
    fun removeMedia(scoreId: Long) {
        viewModelScope.launch {
            try {
                val score = getScoreById(scoreId)
                if (score != null && score.mediaUri != null) {
                    // Store media URI for potential cleanup
                    val mediaUri = score.mediaUri
                    
                    // Update score with null media
                    val updatedScore = score.copy(mediaUri = null)
                    updateScore(updatedScore)
                    
                    // Try to delete the physical file if it's in our app storage
                    if (mediaUri.startsWith("content://") && 
                        mediaUri.contains(getApplication<Application>().packageName)) {
                        try {
                            val uriObj = Uri.parse(mediaUri)
                            val context = getApplication<Application>()
                            val contentResolver = context.contentResolver
                            contentResolver.delete(uriObj, null, null)
                        } catch (e: Exception) {
                            // Just log, don't fail the whole operation
                            e.printStackTrace()
                        }
                    }
                    
                    _mediaRemoveResult.postValue(MediaRemoveResult.Success)
                } else {
                    _mediaRemoveResult.postValue(MediaRemoveResult.Error("Score not found or no media attached"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _mediaRemoveResult.postValue(MediaRemoveResult.Error(e.message ?: "Unknown error"))
            }
        }
    }
    
    // Methods to clear LiveData after consumption
    fun clearDeleteResult() {
        _deleteResult.value = null
    }
    
    fun clearMediaSaveResult() {
        _mediaSaveResult.value = null
    }
    
    fun clearMediaRemoveResult() {
        _mediaRemoveResult.value = null
    }
}

sealed class ExportResult {
    data class Success(val uri: Uri) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

sealed class MediaSaveResult {
    object Success : MediaSaveResult()
    data class Error(val message: String) : MediaSaveResult()
}

sealed class DeleteResult {
    data class Success(val message: String) : DeleteResult()
    data class UndoSuccess(val message: String) : DeleteResult()
    data class Error(val message: String) : DeleteResult()
}

sealed class MediaRemoveResult {
    object Success : MediaRemoveResult()
    data class Error(val message: String) : MediaRemoveResult()
}

class HistoryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 