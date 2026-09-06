package com.example.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.DateUtils
import com.example.data.StudySessionEntity
import com.example.databinding.ItemTodaySessionBinding

class TodaySessionAdapter(
    private val onSwitchSession: (StudySessionEntity) -> Unit,
    private val onDeleteSession: (StudySessionEntity) -> Unit
) : ListAdapter<StudySessionEntity, TodaySessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    private var activeSessionId: Long? = null

    fun setActiveSessionId(id: Long?) {
        if (activeSessionId != id) {
            activeSessionId = id
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemTodaySessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(
        private val binding: ItemTodaySessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: StudySessionEntity) {
            val context = binding.root.context
            val isActive = session.id == activeSessionId

            // Primary Subject
            binding.textSessionSubject.text = session.subject.ifBlank { "General" }

            // Clean time range
            val timeRange = DateUtils.formatTimeRange(session.startTime, session.endTime)
            binding.textSessionTimeRange.text = if (timeRange.isNotBlank()) timeRange else "Today"

            // Compact duration (e.g. "30m", "1h 15m")
            binding.textSessionDuration.text = DateUtils.formatDurationCompact(session.durationSeconds)

            // Active state handling
            if (isActive) {
                binding.badgeActiveSession.visibility = View.VISIBLE
                binding.cardSession.strokeColor = ContextCompat.getColor(context, R.color.app_text_primary)
                binding.cardSession.strokeWidth = 2
                binding.buttonSwitchSession.text = "Active"
                binding.buttonSwitchSession.setIconResource(R.drawable.ic_pause)
                binding.buttonSwitchSession.setBackgroundColor(ContextCompat.getColor(context, R.color.app_text_primary))
                binding.buttonSwitchSession.setTextColor(ContextCompat.getColor(context, R.color.app_bg))
                binding.buttonSwitchSession.iconTint = ContextCompat.getColorStateList(context, R.color.app_bg)
            } else {
                binding.badgeActiveSession.visibility = View.GONE
                binding.cardSession.strokeColor = ContextCompat.getColor(context, R.color.app_stroke)
                binding.cardSession.strokeWidth = 1
                binding.buttonSwitchSession.text = "Resume"
                binding.buttonSwitchSession.setIconResource(R.drawable.ic_play)
                binding.buttonSwitchSession.setBackgroundColor(ContextCompat.getColor(context, R.color.app_surface_variant))
                binding.buttonSwitchSession.setTextColor(ContextCompat.getColor(context, R.color.app_text_primary))
                binding.buttonSwitchSession.iconTint = ContextCompat.getColorStateList(context, R.color.app_text_primary)
            }

            // Click interactions
            binding.buttonSwitchSession.setOnClickListener {
                onSwitchSession(session)
            }

            binding.cardSession.setOnClickListener {
                onSwitchSession(session)
            }

            binding.buttonDeleteSession.setOnClickListener {
                onDeleteSession(session)
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<StudySessionEntity>() {
        override fun areItemsTheSame(oldItem: StudySessionEntity, newItem: StudySessionEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StudySessionEntity, newItem: StudySessionEntity): Boolean {
            return oldItem == newItem
        }
    }
}
