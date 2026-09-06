package com.example.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.HabitReportItem
import com.example.databinding.ItemHabitReportBinding

class HabitReportAdapter : ListAdapter<HabitReportItem, HabitReportAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemHabitReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReportViewHolder(
        private val binding: ItemHabitReportBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HabitReportItem) {
            val context = binding.root.context
            binding.textReportHabitName.text = item.name

            val badgeText = if (item.totalDaysTracked == 0) {
                "NEW"
            } else {
                "${item.completionRatePercent}% FOLLOWED"
            }
            binding.textReportRateBadge.text = badgeText

            binding.progressReportHabit.progress = item.completionRatePercent

            binding.textReportFollowedDays.text =
                context.getString(R.string.habits_followed_count, item.followedDays)

            binding.textReportSkippedDays.text =
                context.getString(R.string.habits_skipped_count, item.skippedDays)

            binding.textReportStreak.text =
                context.getString(R.string.habit_streak, item.currentStreak)
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<HabitReportItem>() {
        override fun areItemsTheSame(oldItem: HabitReportItem, newItem: HabitReportItem): Boolean {
            return oldItem.habitId == newItem.habitId
        }

        override fun areContentsTheSame(oldItem: HabitReportItem, newItem: HabitReportItem): Boolean {
            return oldItem == newItem
        }
    }
}
