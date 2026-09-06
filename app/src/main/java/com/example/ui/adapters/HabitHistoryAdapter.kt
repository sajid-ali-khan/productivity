package com.example.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.DayHabitHistory
import com.example.databinding.ItemHabitHistoryBinding

class HabitHistoryAdapter :
    ListAdapter<DayHabitHistory, HabitHistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding =
            ItemHabitHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(
        private val binding: ItemHabitHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DayHabitHistory) {
            val context = binding.root.context
            binding.textHistoryDate.text = item.displayDate

            val ratePercent = if (item.totalHabits > 0) {
                (item.completedCount * 100) / item.totalHabits
            } else 0
            binding.textHistoryRate.text =
                context.getString(R.string.habits_completed_format, item.completedCount, item.totalHabits) +
                        " ($ratePercent%)"

            if (item.followedHabits.isNotEmpty()) {
                val followedBuilder = StringBuilder()
                item.followedHabits.forEachIndexed { index, name ->
                    followedBuilder.append("✓ ").append(name)
                    if (index < item.followedHabits.size - 1) followedBuilder.append("\n")
                }
                binding.textHistoryFollowed.text = followedBuilder.toString()
                binding.textHistoryFollowed.visibility = View.VISIBLE
            } else {
                binding.textHistoryFollowed.visibility = View.GONE
            }

            if (item.skippedHabits.isNotEmpty()) {
                val skippedBuilder = StringBuilder()
                item.skippedHabits.forEachIndexed { index, name ->
                    skippedBuilder.append("✕ ").append(name)
                    if (index < item.skippedHabits.size - 1) skippedBuilder.append("\n")
                }
                binding.textHistorySkipped.text = skippedBuilder.toString()
                binding.textHistorySkipped.visibility = View.VISIBLE
            } else {
                binding.textHistorySkipped.visibility = View.GONE
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<DayHabitHistory>() {
        override fun areItemsTheSame(oldItem: DayHabitHistory, newItem: DayHabitHistory): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: DayHabitHistory, newItem: DayHabitHistory): Boolean {
            return oldItem == newItem
        }
    }
}
