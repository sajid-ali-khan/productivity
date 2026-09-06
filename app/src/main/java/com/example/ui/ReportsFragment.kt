package com.example.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ProductivityApplication
import com.example.R
import com.example.databinding.FragmentReportsBinding
import com.example.ui.adapters.HabitHistoryAdapter
import com.example.ui.adapters.HabitReportAdapter
import com.example.ui.adapters.StudyHistoryAdapter
import kotlinx.coroutines.launch

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductivityViewModel by lazy {
        val app = requireActivity().application as ProductivityApplication
        ViewModelProvider(requireActivity(), ProductivityViewModelFactory(app.repository))[ProductivityViewModel::class.java]
    }

    private lateinit var habitReportAdapter: HabitReportAdapter
    private lateinit var habitHistoryAdapter: HabitHistoryAdapter
    private lateinit var studyHistoryAdapter: StudyHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupTabToggle()
        observeData()
    }

    private fun setupAdapters() {
        habitReportAdapter = HabitReportAdapter()
        binding.recyclerHabitReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitReportAdapter
        }

        habitHistoryAdapter = HabitHistoryAdapter()
        binding.recyclerHabitHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitHistoryAdapter
        }

        studyHistoryAdapter = StudyHistoryAdapter()
        binding.recyclerStudyHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = studyHistoryAdapter
        }
    }

    private fun setupTabToggle() {
        binding.toggleReportsTab.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_tab_habits -> {
                        binding.layoutHabitsReports.visibility = View.VISIBLE
                        binding.layoutStudyHistory.visibility = View.GONE
                    }
                    R.id.btn_tab_study -> {
                        binding.layoutHabitsReports.visibility = View.GONE
                        binding.layoutStudyHistory.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun observeData() {
        // Setup interactive graph click listeners
        binding.graphHabitsTrend.onDaySelectedListener = { dayData ->
            val label = if (dayData.isToday) "Today" else dayData.dayLabel
            binding.textGraphHabitSelection.text =
                "$label: ${dayData.completedCount} of ${dayData.totalCount} completed (${dayData.completionRatePercent}%)"
        }

        binding.graphStudyTime.onDaySelectedListener = { dayData ->
            val label = if (dayData.isToday) "Today" else dayData.dayLabel
            val sesText = if (dayData.sessionCount == 1) "1 session" else "${dayData.sessionCount} sessions"
            binding.textGraphStudySelection.text =
                "$label: ${dayData.formattedDuration} • $sesText"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. 7-Day Habit Trend Graph
                launch {
                    viewModel.last7DaysHabitsGraph.collect { graphData ->
                        binding.graphHabitsTrend.setData(graphData)
                        val selected = graphData.firstOrNull { it.isToday } ?: graphData.lastOrNull()
                        if (selected != null) {
                            val label = if (selected.isToday) "Today" else selected.dayLabel
                            binding.textGraphHabitSelection.text =
                                "$label: ${selected.completedCount} of ${selected.totalCount} completed (${selected.completionRatePercent}%)"
                        }
                    }
                }

                // 2. Habit Reports
                launch {
                    viewModel.habitReports.collect { reports ->
                        habitReportAdapter.submitList(reports)

                        if (reports.isNotEmpty()) {
                            val totalFollowed = reports.sumOf { it.followedDays }
                            val totalTracked = reports.sumOf { it.totalDaysTracked }
                            val overallRate = if (totalTracked > 0) {
                                (totalFollowed * 100) / totalTracked
                            } else 0

                            binding.textOverallRate.text = "$overallRate%"
                            binding.progressOverallRate.progress = overallRate
                        } else {
                            binding.textOverallRate.text = "0%"
                            binding.progressOverallRate.progress = 0
                        }
                    }
                }

                // 3. Day-by-day Habit History Logs
                launch {
                    viewModel.habitHistory.collect { historyList ->
                        habitHistoryAdapter.submitList(historyList)
                        binding.textEmptyHabitHistory.visibility =
                            if (historyList.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                // 4. 7-Day Study Time Graph
                launch {
                    viewModel.last7DaysStudyGraph.collect { studyGraphData ->
                        binding.graphStudyTime.setData(studyGraphData)
                        val selected = studyGraphData.firstOrNull { it.isToday } ?: studyGraphData.lastOrNull()
                        if (selected != null) {
                            val label = if (selected.isToday) "Today" else selected.dayLabel
                            val sesText = if (selected.sessionCount == 1) "1 session" else "${selected.sessionCount} sessions"
                            binding.textGraphStudySelection.text =
                                "$label: ${selected.formattedDuration} • $sesText"
                        }
                    }
                }

                // 5. Step-counter style Study History with Sessions
                launch {
                    viewModel.dailyStudyHistoryWithSessions.collect { studyList ->
                        studyHistoryAdapter.submitList(studyList)
                        binding.textEmptyStudyHistory.visibility =
                            if (studyList.isEmpty()) View.VISIBLE else View.GONE

                        val allSeconds = studyList.sumOf { it.totalSeconds }
                        val hours = allSeconds / 3600
                        val mins = (allSeconds % 3600) / 60
                        val formatted = if (hours > 0) {
                            "${hours}h ${mins}m"
                        } else {
                            "${mins}m"
                        }
                        binding.textStudyAllTimeTotal.text = formatted
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
