package com.accidentalproductions.tetristats.ui.history

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.accidentalproductions.tetristats.data.Score
import com.accidentalproductions.tetristats.databinding.ItemScoreBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScoreAdapter(private val mediaListener: MediaAttachmentListener? = null) : 
    ListAdapter<Score, ScoreAdapter.ScoreViewHolder>(ScoreDiffCallback()) {

    interface MediaAttachmentListener {
        fun onAddMediaClicked(scoreId: Long)
        fun onMediaClicked(mediaUri: Uri, isVideo: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val binding = ItemScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScoreViewHolder(binding, mediaListener)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScoreViewHolder(
        private val binding: ItemScoreBinding, 
        private val mediaListener: MediaAttachmentListener?
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        
        fun bind(score: Score) {
            // Game version
            binding.textViewGameVersion.text = score.gameVersion
            
            // Score value with formatting
            binding.textViewScore.text = numberFormat.format(score.scoreValue)
            
            // Date
            binding.textViewDate.text = score.dateRecorded.let { 
                dateFormat.format(Date(it)) 
            }
            
            // Level info
            val hasStartLevel = score.startLevel != null
            val hasEndLevel = score.endLevel != null
            
            when {
                hasStartLevel && hasEndLevel -> 
                    binding.textViewLevelInfo.text = "${score.startLevel} → ${score.endLevel}"
                hasStartLevel && !hasEndLevel ->
                    binding.textViewLevelInfo.text = "${score.startLevel}"
                !hasStartLevel && hasEndLevel ->
                    binding.textViewLevelInfo.text = "${score.endLevel}"
                else -> {
                    // Hide the levels section if no level data
                    (binding.textViewLevelInfo.parent as ViewGroup).visibility = View.GONE
                }
            }
            
            // Lines cleared
            if (score.linesCleared != null) {
                binding.layoutLinesCleared.visibility = View.VISIBLE
                binding.textViewLinesCleared.text = "${score.linesCleared}"
            } else {
                binding.layoutLinesCleared.visibility = View.GONE
            }
            
            // Handle media display
            setupMedia(score)
        }
        
        private fun setupMedia(score: Score) {
            if (score.mediaUri != null) {
                // We have media to display
                binding.cardViewMedia.visibility = View.VISIBLE
                binding.buttonAddMedia.visibility = View.GONE
                
                val uri = score.mediaUri.toUri()
                val isVideo = score.mediaUri.endsWith(".mp4", ignoreCase = true) || 
                              score.mediaUri.endsWith(".3gp", ignoreCase = true) ||
                              score.mediaUri.endsWith(".mkv", ignoreCase = true) ||
                              score.mediaUri.endsWith(".webm", ignoreCase = true)
                
                if (isVideo) {
                    // Display video thumbnail
                    binding.videoContainer.visibility = View.VISIBLE
                    binding.imageViewMedia.visibility = View.GONE
                    
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(binding.root.context, uri)
                        val bitmap = retriever.frameAtTime
                        binding.imageViewVideoThumbnail.setImageBitmap(bitmap)
                        
                        // Set click listener to play video
                        binding.videoContainer.setOnClickListener {
                            mediaListener?.onMediaClicked(uri, true)
                        }
                        
                        retriever.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        binding.videoContainer.visibility = View.GONE
                    }
                } else {
                    // Display image
                    binding.videoContainer.visibility = View.GONE
                    binding.imageViewMedia.visibility = View.VISIBLE
                    
                    try {
                        binding.imageViewMedia.setImageURI(uri)
                        
                        // Set click listener to view full image
                        binding.imageViewMedia.setOnClickListener {
                            mediaListener?.onMediaClicked(uri, false)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        binding.cardViewMedia.visibility = View.GONE
                        binding.buttonAddMedia.visibility = View.VISIBLE
                    }
                }
            } else {
                // No media, show the add button
                binding.cardViewMedia.visibility = View.GONE
                binding.buttonAddMedia.visibility = View.VISIBLE
                
                // Set click listener to add media
                binding.buttonAddMedia.setOnClickListener {
                    mediaListener?.onAddMediaClicked(score.id)
                }
            }
        }
    }
}

class ScoreDiffCallback : DiffUtil.ItemCallback<Score>() {
    override fun areItemsTheSame(oldItem: Score, newItem: Score): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Score, newItem: Score): Boolean {
        return oldItem == newItem
    }
} 