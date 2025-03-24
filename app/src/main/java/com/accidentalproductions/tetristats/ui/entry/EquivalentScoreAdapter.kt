package com.accidentalproductions.tetristats.ui.entry

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.accidentalproductions.tetristats.databinding.ItemEquivalentScoreBinding

class EquivalentScoreAdapter : ListAdapter<EquivalentScore, EquivalentScoreAdapter.ViewHolder>(EquivalentScoreDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEquivalentScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemEquivalentScoreBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: EquivalentScore) {
            binding.textViewGameName.text = item.gameName
            binding.textViewEquivalentScore.text = "%,d".format(item.score)
            
            // Show sample count if any samples exist
            if (item.sampleCount > 0) {
                binding.textViewSampleCount.visibility = android.view.View.VISIBLE
                binding.textViewSampleCount.text = "Based on ${item.sampleCount} learning ${if (item.sampleCount == 1) "sample" else "samples"}"
            } else {
                binding.textViewSampleCount.visibility = android.view.View.GONE
            }
        }
    }
}

class EquivalentScoreDiffCallback : DiffUtil.ItemCallback<EquivalentScore>() {
    override fun areItemsTheSame(oldItem: EquivalentScore, newItem: EquivalentScore): Boolean {
        return oldItem.gameName == newItem.gameName
    }

    override fun areContentsTheSame(oldItem: EquivalentScore, newItem: EquivalentScore): Boolean {
        return oldItem == newItem
    }
}

/**
 * Data class representing an equivalent score in another game
 */
data class EquivalentScore(
    val gameName: String,
    val score: Int,
    val sampleCount: Int = 0,
    val usesDynamicFactor: Boolean = false
) 