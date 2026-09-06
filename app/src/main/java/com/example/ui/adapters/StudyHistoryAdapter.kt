package com.example.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.data.DailyStudyHistoryWithSessions
import com.example.data.DateUtils
import com.example.databinding.ItemStudyHistoryBinding

class StudyHistoryAdapter :
    ListAdapter<DailyStudyHistoryWithSessions, StudyHistoryAdapter.StudyViewHolder>(StudyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudyViewHolder {
        val binding =
            ItemStudyHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StudyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StudyViewHolder(
        private val binding: ItemStudyHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DailyStudyHistoryWithSessions) {
            binding.textStudyDate.text = item.displayDate
            binding.textStudyDuration.text = item.formattedDuration

            binding.progressStudyDay.progress = item.progressPercent

            val sessionsText = if (item.sessionCount == 1) {
                "1 session recorded"
            } else {
                "${item.sessionCount} sessions recorded"
            }
            binding.textStudySessionsDetail.text = sessionsText

            val goalText = if (item.progressPercent >= 100) {
                "Goal Met (${item.progressPercent}%)"
            } else {
                "${item.progressPercent}% of 2h goal"
            }
            binding.textStudyGoalStatus.text = goalText

            // Display itemized session breakdown (sorted in descending order of length, no redundant 'Session X' labels)
            if (item.sessions.isNotEmpty()) {
                val sb = StringBuilder()
                val sortedSessions = item.sessions.sortedByDescending { it.durationSeconds }
                sortedSessions.forEachIndexed { index, session ->
                    val subject = if (session.subject.isNotBlank()) session.subject.trim() else "General"
                    val dur = DateUtils.formatDurationCompact(session.durationSeconds)
                    val timeRange = DateUtils.formatTimeRange(session.startTime, session.endTime)

                    if (timeRange.isNotBlank()) {
                        sb.append("• $subject — $dur  ($timeRange)")
                    } else {
                        sb.append("• $subject — $dur")
                    }
                    if (index < sortedSessions.size - 1) {
                        sb.append("\n")
                    }
                }
                binding.textSessionsList.text = sb.toString()
                binding.layoutSessionsBreakdown.visibility = View.VISIBLE
            } else {
                binding.layoutSessionsBreakdown.visibility = View.GONE
            }
        }
    }

    class StudyDiffCallback : DiffUtil.ItemCallback<DailyStudyHistoryWithSessions>() {
        override fun areItemsTheSame(oldItem: DailyStudyHistoryWithSessions, newItem: DailyStudyHistoryWithSessions): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: DailyStudyHistoryWithSessions, newItem: DailyStudyHistoryWithSessions): Boolean {
            return oldItem == newItem
        }
    }
}

