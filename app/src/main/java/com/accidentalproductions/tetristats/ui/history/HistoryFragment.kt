package com.accidentalproductions.tetristats.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.accidentalproductions.tetristats.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels { HistoryViewModelFactory(requireActivity().application) }
    private lateinit var scoreAdapter: ScoreAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeScores()
    }

    private fun setupRecyclerView() {
        scoreAdapter = ScoreAdapter()
        binding.recyclerViewHistory.apply {
            adapter = scoreAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeScores() {
        viewModel.allScores.observe(viewLifecycleOwner) { scores ->
            scoreAdapter.submitList(scores)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 