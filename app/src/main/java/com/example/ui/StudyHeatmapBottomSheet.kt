package com.example.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ProductivityApplication
import com.example.data.DateUtils
import com.example.data.StudyHeatmapData
import com.example.databinding.BottomSheetStudyHeatmapBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StudyHeatmapBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetStudyHeatmapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductivityViewModel by lazy {
        val app = requireActivity().application as ProductivityApplication
        ViewModelProvider(requireActivity(), ProductivityViewModelFactory(app.repository))[ProductivityViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetStudyHeatmapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewStudyHeatmap.onCellSelectedListener = { cell ->
            val displayDate = DateUtils.formatDisplayDate(cell.dateStr)
            val durationText = if (cell.durationSeconds > 0) {
                DateUtils.formatDurationCompact(cell.durationSeconds) + " studied"
            } else {
                "No study time recorded"
            }
            binding.textStudyHeatmapSelectedInfo.text = "$displayDate: $durationText"
        }

        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.studyHeatmap.collectLatest { heatmapData: StudyHeatmapData ->
                    binding.textStudyHeatmapTotalBadge.text =
                        DateUtils.formatDurationCompact(heatmapData.totalSeconds) + " Total"
                    binding.textStatStudyActiveDays.text = "${heatmapData.activeDaysCount} days"
                    binding.textStatStudyCurrentStreak.text = "${heatmapData.currentStreak} days"
                    binding.textStatStudyLongestStreak.text = "${heatmapData.longestStreak} days"

                    binding.viewStudyHeatmap.setStudyData(heatmapData.dailyDurations)

                    val todayStr = DateUtils.getTodayDateString()
                    val todaySec = heatmapData.dailyDurations[todayStr] ?: 0L
                    val todayDurationText = if (todaySec > 0) {
                        DateUtils.formatDurationCompact(todaySec) + " studied"
                    } else {
                        "0m studied so far"
                    }
                    binding.textStudyHeatmapSelectedInfo.text =
                        "Today • ${DateUtils.formatDisplayDate(todayStr)}: $todayDurationText"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "StudyHeatmapBottomSheet"

        fun newInstance(): StudyHeatmapBottomSheet {
            return StudyHeatmapBottomSheet()
        }
    }
}
