package com.accidentalproductions.tetristats.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.accidentalproductions.tetristats.databinding.ActivityScalingFactorTestBinding
import com.accidentalproductions.tetristats.util.GameScoreSample
import com.accidentalproductions.tetristats.util.ScalingFactorAnalyzer

class ScalingFactorTestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScalingFactorTestBinding
    private val analyzer = ScalingFactorAnalyzer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScalingFactorTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGameDropdown()
        setupSkillLevelDropdown()
        setupButtons()
    }

    private fun setupGameDropdown() {
        val games = listOf(
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
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, games)
        binding.spinnerGame.setAdapter(adapter)
    }

    private fun setupSkillLevelDropdown() {
        val skillLevels = listOf("beginner", "intermediate", "advanced")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, skillLevels)
        binding.spinnerSkillLevel.setAdapter(adapter)
    }

    private fun setupButtons() {
        binding.buttonAddSample.setOnClickListener {
            val game = binding.spinnerGame.text.toString()
            val score = binding.editTextScore.text.toString().toIntOrNull()
            val level = binding.editTextLevel.text.toString().toIntOrNull()
            val skillLevel = binding.spinnerSkillLevel.text.toString()
            val notes = binding.editTextNotes.text.toString()

            if (score != null && level != null) {
                val sample = GameScoreSample(game, score, level, skillLevel, notes)
                analyzer.addSample(sample)
                clearInputs()
                updateSampleCount()
            }
        }

        binding.buttonAnalyze.setOnClickListener {
            analyzer.printAnalysisReport()
            binding.textViewReport.text = analyzer.generateScalingFactorCode()
        }

        binding.buttonClear.setOnClickListener {
            analyzer.clearSamples()
            updateSampleCount()
            binding.textViewReport.text = ""
        }
    }

    private fun updateSampleCount() {
        binding.textViewSampleCount.text = "Samples: ${analyzer.sampleCount}"
    }

    private fun clearInputs() {
        binding.editTextScore.text?.clear()
        binding.editTextLevel.text?.clear()
        binding.editTextNotes.text?.clear()
    }
} 