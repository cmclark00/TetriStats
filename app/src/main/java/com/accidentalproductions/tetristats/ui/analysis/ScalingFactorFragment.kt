package com.accidentalproductions.tetristats.ui.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.accidentalproductions.tetristats.databinding.FragmentScalingFactorBinding
import com.accidentalproductions.tetristats.util.GameScoreSample
import com.accidentalproductions.tetristats.util.ScalingFactorAnalyzer

class ScalingFactorFragment : Fragment() {
    private var _binding: FragmentScalingFactorBinding? = null
    private val binding get() = _binding!!
    private val analyzer = ScalingFactorAnalyzer()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScalingFactorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, games)
        binding.spinnerGame.setAdapter(adapter)
    }

    private fun setupSkillLevelDropdown() {
        val skillLevels = listOf("beginner", "intermediate", "advanced")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, skillLevels)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 