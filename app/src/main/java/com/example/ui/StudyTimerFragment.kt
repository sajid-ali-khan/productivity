package com.example.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ProductivityApplication
import com.example.R
import com.example.data.DateUtils
import com.example.databinding.FragmentTimerBinding
import com.example.ui.adapters.TodaySessionAdapter
import kotlinx.coroutines.launch

class StudyTimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductivityViewModel by lazy {
        val app = requireActivity().application as ProductivityApplication
        ViewModelProvider(requireActivity(), ProductivityViewModelFactory(app.repository))[ProductivityViewModel::class.java]
    }

    private lateinit var todaySessionAdapter: TodaySessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure default category is General
        if (binding.inputStudySubject.text.isNullOrBlank()) {
            binding.inputStudySubject.setText(getString(R.string.quick_chip_general))
        }

        setupSessionsRecyclerView()
        setupListeners()
        setupQuickChips()
        observeData()
    }

    private fun setupSessionsRecyclerView() {
        todaySessionAdapter = TodaySessionAdapter(
            onSwitchSession = { session ->
                // Switch session even after saving; only one active at a time
                viewModel.switchToSession(session, autoResume = true)
                val displaySub = session.subject.ifBlank { "General" }
                Toast.makeText(requireContext(), "Resumed session: $displaySub", Toast.LENGTH_SHORT).show()
            },
            onDeleteSession = { session ->
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.delete)
                    .setMessage(getString(R.string.delete_session_confirm))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.deleteStudySession(session.id)
                        Toast.makeText(requireContext(), R.string.session_deleted, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )

        binding.recyclerTodaySessions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todaySessionAdapter
        }
    }

    private fun setupQuickChips() {
        val chips = listOf(
            binding.chipSubjectGeneral to getString(R.string.quick_chip_general),
            binding.chipSubjectMath to "Math",
            binding.chipSubjectReading to "Reading",
            binding.chipSubjectCoding to "Coding",
            binding.chipSubjectScience to "Science",
            binding.chipSubjectWriting to "Writing"
        )

        for ((chip, topic) in chips) {
            chip.setOnClickListener {
                viewModel.setSubject(topic)
                binding.inputStudySubject.setText(topic)
                binding.inputStudySubject.setSelection(topic.length)
            }
        }
    }

    private fun setupListeners() {
        binding.buttonStartStudy.setOnClickListener {
            val subject = binding.inputStudySubject.text?.toString()?.trim().orEmpty()
            viewModel.startTimer(subject.ifBlank { "General" })
        }

        binding.buttonPauseResume.setOnClickListener {
            when (viewModel.timerState.value) {
                TimerState.RUNNING -> viewModel.pauseTimer()
                TimerState.PAUSED -> viewModel.resumeTimer()
                TimerState.IDLE -> {
                    val subject = binding.inputStudySubject.text?.toString()?.trim().orEmpty()
                    viewModel.startTimer(subject.ifBlank { "General" })
                }
            }
        }

        binding.buttonStopSave.setOnClickListener {
            val subject = binding.inputStudySubject.text?.toString()?.trim().orEmpty()
            viewLifecycleOwner.lifecycleScope.launch {
                val saved = viewModel.stopAndSaveTimer(subject.ifBlank { "General" })
                binding.inputStudySubject.setText(getString(R.string.quick_chip_general))
                if (saved) {
                    Toast.makeText(requireContext(), R.string.session_saved, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), R.string.session_too_short, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.buttonDiscard.setOnClickListener {
            viewModel.discardTimer()
            binding.inputStudySubject.setText(getString(R.string.quick_chip_general))
            Toast.makeText(requireContext(), R.string.session_discarded, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Timer State Machine UI
                launch {
                    viewModel.timerState.collect { state ->
                        updateTimerUiForState(state)
                    }
                }

                // 2. Active Session Clock
                launch {
                    viewModel.elapsedSeconds.collect { totalSec ->
                        binding.textTimerClock.text = DateUtils.formatSecondsToClock(totalSec)
                    }
                }

                // 3. Whole Study Time Today & Goal
                launch {
                    viewModel.todayStudySeconds.collect { totalTodaySec ->
                        val hours = totalTodaySec / 3600
                        val mins = (totalTodaySec % 3600) / 60
                        val formatted = if (hours > 0) {
                            "${hours}h ${mins}m"
                        } else {
                            "${mins}m"
                        }
                        binding.textTodayTotalTime.text = formatted

                        val goalSeconds = 7200L // 2 hour daily goal
                        val progressPercent = ((totalTodaySec.toDouble() / goalSeconds.toDouble()) * 100).toInt()
                        binding.progressTodayStudy.progress = progressPercent.coerceIn(0, 100)
                        binding.textStudyGoalStatus.text = "$progressPercent% achieved"
                    }
                }

                // 4. All-Time Study Stats
                launch {
                    viewModel.allTimeStudySeconds.collect { allSec ->
                        val h = allSec / 3600
                        val m = (allSec % 3600) / 60
                        val formatted = if (h > 0) "${h}h ${m}m" else "${m}m"
                        binding.textAllTimeTotal.text = "All-Time: $formatted"
                    }
                }

                // 5. Active Session ID Tracker (For switching between sessions)
                launch {
                    viewModel.activeSessionId.collect { activeId ->
                        todaySessionAdapter.setActiveSessionId(activeId)
                        if (activeId != null) {
                            binding.textActiveSessionLabel.visibility = View.VISIBLE
                        } else {
                            binding.textActiveSessionLabel.visibility = View.GONE
                        }
                    }
                }

                // 6. Active Session Subject
                launch {
                    viewModel.currentSubject.collect { sub ->
                        val currentText = binding.inputStudySubject.text?.toString()?.trim().orEmpty()
                        if (sub.isNotBlank() && currentText != sub && viewModel.timerState.value != TimerState.IDLE) {
                            binding.inputStudySubject.setText(sub)
                            binding.inputStudySubject.setSelection(sub.length)
                        }
                    }
                }

                // 7. Today's Separate Study Sessions List
                launch {
                    viewModel.todayStudySessions.collect { sessions ->
                        todaySessionAdapter.submitList(sessions)
                        binding.textSessionsBadge.text = if (sessions.size == 1) "1 session" else "${sessions.size} sessions"
                        binding.textTodaySessionsCount.text = getString(R.string.today_sessions_count_format, sessions.size)

                        if (sessions.isEmpty()) {
                            binding.layoutEmptyTodaySessions.visibility = View.VISIBLE
                            binding.recyclerTodaySessions.visibility = View.GONE
                        } else {
                            binding.layoutEmptyTodaySessions.visibility = View.GONE
                            binding.recyclerTodaySessions.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun updateTimerUiForState(state: TimerState) {
        when (state) {
            TimerState.IDLE -> {
                binding.textTimerStatus.text = getString(R.string.study_status_ready)
                binding.layoutIdleControls.visibility = View.VISIBLE
                binding.layoutActiveControls.visibility = View.GONE
                binding.inputLayoutSubject.isEnabled = true
                for (i in 0 until binding.layoutQuickChips.childCount) {
                    binding.layoutQuickChips.getChildAt(i).isEnabled = true
                }
            }
            TimerState.RUNNING -> {
                binding.textTimerStatus.text = getString(R.string.study_status_active)
                binding.layoutIdleControls.visibility = View.GONE
                binding.layoutActiveControls.visibility = View.VISIBLE
                binding.buttonPauseResume.text = getString(R.string.pause_study)
                binding.buttonPauseResume.setIconResource(R.drawable.ic_pause)
                binding.inputLayoutSubject.isEnabled = false
                for (i in 0 until binding.layoutQuickChips.childCount) {
                    binding.layoutQuickChips.getChildAt(i).isEnabled = false
                }
            }
            TimerState.PAUSED -> {
                binding.textTimerStatus.text = getString(R.string.study_status_paused)
                binding.layoutIdleControls.visibility = View.GONE
                binding.layoutActiveControls.visibility = View.VISIBLE
                binding.buttonPauseResume.text = getString(R.string.resume_study)
                binding.buttonPauseResume.setIconResource(R.drawable.ic_play)
                binding.inputLayoutSubject.isEnabled = true
                for (i in 0 until binding.layoutQuickChips.childCount) {
                    binding.layoutQuickChips.getChildAt(i).isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
