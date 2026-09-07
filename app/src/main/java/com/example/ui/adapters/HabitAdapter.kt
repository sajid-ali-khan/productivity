package com.example.ui.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.data.HabitWithStatus
import com.example.databinding.ItemHabitBinding

class HabitAdapter(
    private val onToggleCompleted: (habitId: Long, isChecked: Boolean) -> Unit,
    private val onItemClick: ((habit: HabitWithStatus) -> Unit)? = null,
    private val onItemLongClick: (habit: HabitWithStatus) -> Unit
) : ListAdapter<HabitWithStatus, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HabitViewHolder(
        private val binding: ItemHabitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HabitWithStatus) {
            binding.textHabitName.text = item.habit.name

            // Detach listener before setting checked state to avoid unwanted triggers
            binding.checkboxHabit.setOnCheckedChangeListener(null)
            binding.checkboxHabit.isChecked = item.isCompleted

            updateTextStyling(item.isCompleted)

            binding.checkboxHabit.setOnCheckedChangeListener { _, isChecked ->
                updateTextStyling(isChecked)
                onToggleCompleted(item.habit.id, isChecked)
            }

            binding.root.setOnClickListener {
                if (onItemClick != null) {
                    onItemClick.invoke(item)
                } else {
                    // Default tap toggles checkbox
                    binding.checkboxHabit.isChecked = !binding.checkboxHabit.isChecked
                }
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }

        private fun updateTextStyling(isCompleted: Boolean) {
            if (isCompleted) {
                binding.textHabitName.paintFlags =
                    binding.textHabitName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.textHabitName.alpha = 0.55f
            } else {
                binding.textHabitName.paintFlags =
                    binding.textHabitName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.textHabitName.alpha = 1.0f
            }
        }
    }

    class HabitDiffCallback : DiffUtil.ItemCallback<HabitWithStatus>() {
        override fun areItemsTheSame(oldItem: HabitWithStatus, newItem: HabitWithStatus): Boolean {
            return oldItem.habit.id == newItem.habit.id
        }

        override fun areContentsTheSame(oldItem: HabitWithStatus, newItem: HabitWithStatus): Boolean {
            return oldItem == newItem
        }
    }
}
