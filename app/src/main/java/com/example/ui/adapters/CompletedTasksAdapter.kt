package com.example.ui.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.data.TaskEntity
import com.example.databinding.ItemTaskCompletedBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CompletedTasksAdapter(
    private val onUncompleteClick: (TaskEntity) -> Unit,
    private val onDeleteClick: (TaskEntity) -> Unit
) : ListAdapter<TaskEntity, CompletedTasksAdapter.CompletedViewHolder>(CompletedDiffCallback()) {

    private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletedViewHolder {
        val binding = ItemTaskCompletedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CompletedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompletedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CompletedViewHolder(
        private val binding: ItemTaskCompletedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskEntity) {
            binding.textCompletedTitle.text = task.title
            binding.textCompletedTitle.paintFlags =
                binding.textCompletedTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            val completedTimestamp = task.completedAt ?: task.createdAt
            val formattedDate = dateFormat.format(Date(completedTimestamp))
            binding.textCompletedDate.text = "Completed: $formattedDate"

            binding.btnUncompleteTask.setOnClickListener {
                onUncompleteClick(task)
            }

            binding.btnDeleteTask.setOnClickListener {
                onDeleteClick(task)
            }
        }
    }

    class CompletedDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem == newItem
        }
    }
}
