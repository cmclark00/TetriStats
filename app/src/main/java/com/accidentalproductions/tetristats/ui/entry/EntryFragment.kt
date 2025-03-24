package com.accidentalproductions.tetristats.ui.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.accidentalproductions.tetristats.data.Score
import com.accidentalproductions.tetristats.databinding.FragmentEntryBinding

class EntryFragment : Fragment() {
    private var _binding: FragmentEntryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EntryViewModel by viewModels { EntryViewModelFactory(requireActivity().application) }
    private lateinit var equivalentScoreAdapter: EquivalentScoreAdapter
    
    // Flag to track if we already showed the requirements toast
    private var hasShownRequirementsToast = false

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
        setupRecyclerView()
        setupSubmitButton()
        setupAutoAnalysis()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh conversions when returning to this fragment
        refreshConversions()
    }
    
    /**
     * Force refresh the conversions using the last submitted values
     */
    private fun refreshConversions() {
        if (viewModel.showConversion.value == true) {
            // If we have last submitted values, regenerate conversions
            val game = viewModel.lastSubmittedGame.value
            val score = viewModel.lastSubmittedScore.value
            
            if (game != null && score != null) {
                viewModel.refreshEquivalentScores(game, score)
                
                // Make sure UI updates immediately by forcing an adapter refresh
                viewModel.equivalentScores.value?.let { scores ->
                    equivalentScoreAdapter.submitList(null)  // Clear first
                    equivalentScoreAdapter.submitList(scores)  // Then add new list
                }
                
                // Ensure card is visible
                updateAnalysisCard()
            }
        }
    }
    
    /**
     * Update the analysis card visibility and contents based on current state
     */
    private fun updateAnalysisCard() {
        if (viewModel.showConversion.value != true) {
            binding.cardAnalysisResults.visibility = View.GONE
            return
        }
        
        val game = viewModel.lastSubmittedGame.value
        val score = viewModel.lastSubmittedScore.value
        
        if (game != null && score != null) {
            // Get the list of games with scores
            val playedGames = viewModel.gamesWithScores.value ?: listOf()
            
            // Make sure we don't show the source game in the equivalent dropdown
            val filteredGames = playedGames.filter { it != game }
            if (filteredGames.isNotEmpty()) {
                binding.textViewOriginalScore.text = "Your $game score of ${"%,d".format(score)} is equivalent to:"
                binding.cardAnalysisResults.visibility = View.VISIBLE
                
                val filteredAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, filteredGames)
                binding.autoCompleteEquivalentGame.setAdapter(filteredAdapter)
                
                // Select first game by default
                binding.autoCompleteEquivalentGame.setText(filteredGames[0], false)
                viewModel.setSelectedEquivalentGame(filteredGames[0])
            } else {
                // If no other games to convert to, hide the card
                binding.cardAnalysisResults.visibility = View.GONE
            }
        }
    }

    private fun setupGameVersionDropdown() {
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
        binding.autoCompleteGameVersion.setAdapter(adapter)
    }
    
    private fun setupRecyclerView() {
        equivalentScoreAdapter = EquivalentScoreAdapter()
        binding.recyclerViewEquivalentScores.apply {
            adapter = equivalentScoreAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupAutoAnalysis() {
        // Hide the analysis card by default
        binding.cardAnalysisResults.visibility = View.GONE
        
        // Observe if we should show conversions
        viewModel.showConversion.observe(viewLifecycleOwner) { shouldShow ->
            // No need to show toast here - we'll do it only after score submission
            if (shouldShow) {
                // Update card when showConversion changes
                updateAnalysisCard()
            } else {
                binding.cardAnalysisResults.visibility = View.GONE
            }
        }
        
        // Only setup equivalence UI when we have scores
        viewModel.gamesWithScores.observe(viewLifecycleOwner) { games ->
            // Setup the game dropdown for adding equivalents - only with played games
            if (games.isNotEmpty()) {
                // Update card when games list changes
                updateAnalysisCard()
            }
        }
        
        // Update selected game
        binding.autoCompleteEquivalentGame.setOnItemClickListener { _, _, _, _ ->
            val selectedGame = binding.autoCompleteEquivalentGame.text.toString()
            viewModel.setSelectedEquivalentGame(selectedGame)
        }
        
        // Handle adding equivalent scores
        binding.buttonAddEquivalent.setOnClickListener {
            val equivalentScore = binding.editTextEquivalentScore.text.toString().toIntOrNull()
            if (equivalentScore != null) {
                viewModel.addEquivalentScore(equivalentScore)
                binding.editTextEquivalentScore.text?.clear()
                Toast.makeText(context, "Equivalent score added! The converter is learning.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Please enter a valid score", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe last submitted score details
        viewModel.lastSubmittedGame.observe(viewLifecycleOwner) { _ ->
            // Update the analysis card when last submitted game changes
            updateAnalysisCard()
        }
        
        // Observe last submitted score value
        viewModel.lastSubmittedScore.observe(viewLifecycleOwner) { _ ->
            // Update the analysis card when score changes
            updateAnalysisCard()
        }
        
        // Observe equivalent scores
        viewModel.equivalentScores.observe(viewLifecycleOwner) { scores ->
            if (scores.isNotEmpty()) {
                // Force a clean update by clearing first
                equivalentScoreAdapter.submitList(null)
                equivalentScoreAdapter.submitList(scores)
            } else if (viewModel.showConversion.value == true) {
                // If we should be showing conversions but have no scores, probably no other games
                binding.cardAnalysisResults.visibility = View.GONE
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
                
                // Force immediate refresh of conversions
                if (viewModel.showConversion.value == false) {
                    Toast.makeText(
                        context,
                        "Enter at least 3 scores across 2 different games to see conversions",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    refreshConversions()
                    
                    // Scroll down to show the analysis results
                    binding.root.post {
                        binding.root.fullScroll(View.FOCUS_DOWN)
                    }
                }
            } else {
                Toast.makeText(context, "Please enter a game and score", Toast.LENGTH_SHORT).show()
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