package com.accidentalproductions.tetristats.ui.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.accidentalproductions.tetristats.data.Score
import com.accidentalproductions.tetristats.databinding.FragmentEntryBinding

class EntryFragment : Fragment() {
    private var _binding: FragmentEntryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EntryViewModel by viewModels { EntryViewModelFactory(requireActivity().application) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGameVersionDropdown()
        setupScoreConverter()
        setupSubmitButton()
    }

    private fun setupGameVersionDropdown() {
        val games = listOf(
            "NES Tetris",
            "Game Boy Tetris",
            "Tetris DX",
            "Tetris DS",
            "Tetris Effect",
            "Rosy Retrospection DX",
            "Apotris"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, games)
        binding.autoCompleteGameVersion.setAdapter(adapter)
    }

    private fun setupScoreConverter() {
        // Setup "From Game" dropdown with games that have scores
        viewModel.gamesWithScores.observe(viewLifecycleOwner) { games ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, games)
            binding.autoCompleteFromGame.setAdapter(adapter)
        }

        // Update score selection when game is selected
        binding.autoCompleteFromGame.setOnItemClickListener { _, _, _, _ ->
            val selectedGame = binding.autoCompleteFromGame.text.toString()
            viewModel.setSelectedFromGame(selectedGame)
            updateScoreDropdown(selectedGame)
        }

        // Setup "To Game" dropdown
        val allGames = listOf(
            "NES Tetris",
            "Game Boy Tetris",
            "Tetris DX",
            "Tetris DS",
            "Tetris Effect",
            "Rosy Retrospection DX",
            "Apotris"
        )
        val toGameAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, allGames)
        binding.autoCompleteToGame.setAdapter(toGameAdapter)

        // Handle score conversion
        binding.buttonConvert.setOnClickListener {
            viewModel.convertScore()
        }

        // Observe converted score
        viewModel.convertedScore.observe(viewLifecycleOwner) { score ->
            binding.cardConvertedScore.visibility = View.VISIBLE
            binding.textViewConvertedScore.text = "%,d".format(score)
        }

        // Update selected games
        binding.autoCompleteToGame.setOnItemClickListener { _, _, _, _ ->
            viewModel.setSelectedToGame(binding.autoCompleteToGame.text.toString())
        }
    }

    private fun updateScoreDropdown(gameVersion: String) {
        viewModel.getScoresForGame(gameVersion).observe(viewLifecycleOwner) { scores ->
            val scoreStrings = scores.map { "${it.scoreValue} (Level ${it.endLevel ?: "?"})"}
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, scoreStrings)
            binding.spinnerScoreSelect.setAdapter(adapter)

            binding.spinnerScoreSelect.setOnItemClickListener { _, _, position, _ ->
                viewModel.setSelectedScore(scores[position])
            }
        }
    }

    private fun setupSubmitButton() {
        binding.buttonSubmit.setOnClickListener {
            val gameVersion = binding.autoCompleteGameVersion.text.toString()
            val score = binding.editTextScore.text.toString().toIntOrNull()
            val startLevel = binding.editTextStartLevel.text.toString().toIntOrNull()
            val endLevel = binding.editTextEndLevel.text.toString().toIntOrNull()
            val linesCleared = binding.editTextLinesCleared.text.toString().toIntOrNull()

            if (gameVersion.isNotEmpty() && score != null) {
                viewModel.insertScore(
                    gameVersion = gameVersion,
                    score = score,
                    startLevel = startLevel,
                    endLevel = endLevel,
                    linesCleared = linesCleared
                )
                clearInputs()
            }
        }
    }

    private fun clearInputs() {
        binding.autoCompleteGameVersion.text?.clear()
        binding.editTextScore.text?.clear()
        binding.editTextStartLevel.text?.clear()
        binding.editTextEndLevel.text?.clear()
        binding.editTextLinesCleared.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 