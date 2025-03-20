package com.accidentalproductions.tetristats.ui.stats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.accidentalproductions.tetristats.data.Score
import com.accidentalproductions.tetristats.databinding.ItemScoreBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ScoreAdapter : ListAdapter<Score, ScoreAdapter.ScoreViewHolder>(ScoreDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val binding = ItemScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScoreViewHolder(private val binding: ItemScoreBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        
        fun bind(score: Score) {
            binding.textViewScore.text = "${score.scoreValue}"
            binding.textViewDate.text = score.dateRecorded?.let { dateFormat.format(it) } ?: "Unknown"
            
            val levelInfo = when {
                score.startLevel != null && score.endLevel != null -> 
                    "Levels ${score.startLevel} → ${score.endLevel}"
                score.endLevel != null -> 
                    "End Level: ${score.endLevel}"
                else -> 
                    ""
            }
            binding.textViewLevelInfo.text = levelInfo
            
            binding.textViewLinesCleared.text = score.linesCleared?.let { "Lines: $it" } ?: ""
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