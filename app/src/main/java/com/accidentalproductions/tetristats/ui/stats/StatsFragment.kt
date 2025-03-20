package com.accidentalproductions.tetristats.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.accidentalproductions.tetristats.databinding.FragmentStatsBinding

class StatsFragment : Fragment() {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatsViewModel by viewModels { StatsViewModelFactory(requireActivity().application) }
    private lateinit var scoreAdapter: ScoreAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupGameFilter()
        observeStats()
    }

    private fun setupRecyclerView() {
        scoreAdapter = ScoreAdapter()
        binding.recyclerViewScores.apply {
            adapter = scoreAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupGameFilter() {
        viewModel.gamesWithScores.observe(viewLifecycleOwner) { games ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, games)
            binding.autoCompleteGameFilter.setAdapter(adapter)
        }

        binding.autoCompleteGameFilter.setOnItemClickListener { _, _, _, _ ->
            val selectedGame = binding.autoCompleteGameFilter.text.toString()
            viewModel.setSelectedGame(selectedGame)
        }
    }

    private fun observeStats() {
        viewModel.filteredScores.observe(viewLifecycleOwner) { scores ->
            scoreAdapter.submitList(scores)
        }

        viewModel.averageScore.observe(viewLifecycleOwner) { average ->
            binding.textViewAverageScore.text = "%.0f".format(average)
        }

        viewModel.highScore.observe(viewLifecycleOwner) { highScore ->
            binding.textViewHighScore.text = "%,d".format(highScore)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 