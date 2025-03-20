package com.accidentalproductions.tetristats.ui.stats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.accidentalproductions.tetristats.data.Score
import com.accidentalproductions.tetristats.databinding.ItemScoreBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
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
            
            // Always hide media container in stats view
            binding.mediaContainer.visibility = View.GONE
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