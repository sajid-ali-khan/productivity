package com.example.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.TaskEntity
import com.example.databinding.ItemTaskActiveBinding

class ActiveTasksAdapter(
    private val onToggleComplete: (TaskEntity) -> Unit,
    private val onToggleStar: (TaskEntity) -> Unit,
    private val onItemClick: (TaskEntity) -> Unit,
    private val onItemLongClick: (TaskEntity) -> Unit
) : ListAdapter<TaskEntity, ActiveTasksAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskActiveBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskActiveBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskEntity) {
            binding.textTaskTitle.text = task.title

            if (task.notes.isNotBlank()) {
                binding.textTaskNotes.text = task.notes
                binding.textTaskNotes.visibility = View.VISIBLE
            } else {
                binding.textTaskNotes.visibility = View.GONE
            }

            if (task.isStarred) {
                binding.btnStarTask.setImageResource(R.drawable.ic_star_filled)
                binding.btnStarTask.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.app_text_primary)
                )
            } else {
                binding.btnStarTask.setImageResource(R.drawable.ic_star_border)
                binding.btnStarTask.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.app_text_muted)
                )
            }

            binding.btnCompleteTask.setOnClickListener {
                onToggleComplete(task)
            }

            binding.btnStarTask.setOnClickListener {
                onToggleStar(task)
            }

            binding.root.setOnClickListener {
                onItemClick(task)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(task)
                true
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem == newItem
        }
    }
}
