package com.accidentalproductions.tetristats.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.accidentalproductions.tetristats.R
import com.accidentalproductions.tetristats.databinding.FragmentStatsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        setupProgressChart()
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
    
    private fun setupProgressChart() {
        with(binding.chartProgress) {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setDrawGridBackground(false)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            axisRight.isEnabled = false
            
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            
            axisLeft.setDrawGridLines(true)
            axisLeft.axisMinimum = 0f
        }
    }

    private fun updateProgressChart(scores: List<Entry>, dates: List<Long>) {
        if (scores.isEmpty()) {
            binding.chartProgress.clear()
            binding.chartProgress.invalidate()
            return
        }
        
        val dataSet = LineDataSet(scores, "Score Progress").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            color = resources.getColor(R.color.tetris_navy, null)
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(resources.getColor(R.color.tetris_navy, null))
            circleRadius = 4f
            setDrawValues(false)
            highLightColor = Color.rgb(244, 117, 117)
        }
        
        val lineData = LineData(dataSet)
        binding.chartProgress.data = lineData
        
        // Format X-axis labels (dates)
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        binding.chartProgress.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < dates.size) {
                    dateFormat.format(Date(dates[index]))
                } else {
                    ""
                }
            }
        }
        
        binding.chartProgress.invalidate()
    }

    private fun observeStats() {
        viewModel.filteredScores.observe(viewLifecycleOwner) { scores ->
            scoreAdapter.submitList(scores)
        }

        viewModel.averageScore.observe(viewLifecycleOwner) { average ->
            binding.textViewAverageScore.text = "%,.0f".format(average)
        }

        viewModel.highScore.observe(viewLifecycleOwner) { highScore ->
            binding.textViewHighScore.text = "%,d".format(highScore)
        }
        
        viewModel.scoresByDate.observe(viewLifecycleOwner) { scores ->
            // Convert scores to entries for the chart
            if (scores.isNotEmpty()) {
                val entries = mutableListOf<Entry>()
                val dates = mutableListOf<Long>()
                
                scores.forEachIndexed { index, score ->
                    entries.add(Entry(index.toFloat(), score.scoreValue.toFloat()))
                    dates.add(score.dateRecorded)
                }
                
                updateProgressChart(entries, dates)
            } else {
                binding.chartProgress.clear()
                binding.chartProgress.invalidate()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 