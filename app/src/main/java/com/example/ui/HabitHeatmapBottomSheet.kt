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
import com.example.R
import com.example.data.DateUtils
import com.example.data.HabitHeatmapData
import com.example.databinding.BottomSheetHabitHeatmapBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HabitHeatmapBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHabitHeatmapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductivityViewModel by lazy {
        val app = requireActivity().application as ProductivityApplication
        ViewModelProvider(requireActivity(), ProductivityViewModelFactory(app.repository))[ProductivityViewModel::class.java]
    }

    private var habitId: Long = -1L
    private var habitName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        habitId = arguments?.getLong(ARG_HABIT_ID) ?: -1L
        habitName = arguments?.getString(ARG_HABIT_NAME).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetHabitHeatmapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textHeatmapHabitName.text = habitName

        binding.viewHabitHeatmap.onCellSelectedListener = { cell ->
            val status = if (cell.isCompleted) "Completed ✓" else "Missed / Incomplete"
            val displayDate = DateUtils.formatDisplayDate(cell.dateStr)
            binding.textHeatmapSelectedInfo.text = "$displayDate: $status"
        }

        observeData()
    }

    private fun observeData() {
        if (habitId <= 0) return

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getHabitHeatmap(habitId).collectLatest { heatmapData: HabitHeatmapData? ->
                    if (heatmapData == null) return@collectLatest

                    binding.textHeatmapHabitName.text = heatmapData.habitName
                    binding.textHeatmapRateBadge.text = "${heatmapData.completionRatePercent}% Followed"
                    binding.textStatCurrentStreak.text = "${heatmapData.currentStreak} days"
                    binding.textStatBestStreak.text = "${heatmapData.bestStreak} days"
                    binding.textStatFollowedDays.text = "${heatmapData.followedDays} of ${heatmapData.totalDaysTracked}"

                    binding.viewHabitHeatmap.setHabitData(
                        completedDates = heatmapData.completedDates,
                        allTrackedDates = heatmapData.allTrackedDates
                    )

                    val todayStr = DateUtils.getTodayDateString()
                    val todayStatus = if (heatmapData.completedDates.contains(todayStr)) "Completed ✓" else "Not yet completed today"
                    binding.textHeatmapSelectedInfo.text = "Today • ${DateUtils.formatDisplayDate(todayStr)}: $todayStatus"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "HabitHeatmapBottomSheet"
        private const val ARG_HABIT_ID = "habit_id"
        private const val ARG_HABIT_NAME = "habit_name"

        fun newInstance(habitId: Long, habitName: String): HabitHeatmapBottomSheet {
            return HabitHeatmapBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_HABIT_ID, habitId)
                    putString(ARG_HABIT_NAME, habitName)
                }
            }
        }
    }
}
