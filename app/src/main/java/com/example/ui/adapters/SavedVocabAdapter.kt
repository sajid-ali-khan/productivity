package com.example.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.DateUtils
import com.example.data.VocabWordEntity
import com.example.databinding.ItemSavedVocabBinding

class SavedVocabAdapter(
    private val onListenClicked: (word: VocabWordEntity) -> Unit,
    private val onPracticeClicked: (word: VocabWordEntity) -> Unit,
    private val onRemoveSavedClicked: (word: VocabWordEntity) -> Unit
) : ListAdapter<VocabWordEntity, SavedVocabAdapter.SavedVocabViewHolder>(VocabDiffCallback()) {

    // Track collapsed items (default is expanded)
    private val collapsedDates = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedVocabViewHolder {
        val binding = ItemSavedVocabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SavedVocabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedVocabViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SavedVocabViewHolder(
        private val binding: ItemSavedVocabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VocabWordEntity) {
            val isExpanded = !collapsedDates.contains(item.date)

            binding.textSavedDate.text = "SAVED • ${DateUtils.formatHeaderDate(item.date).uppercase()}"
            binding.textVocabWord.text = item.word
            binding.textVocabPos.text = "• ${item.partOfSpeech}"
            binding.textVocabPhonetic.text = item.phonetic
            binding.textVocabDefinition.text = item.definition
            binding.textVocabExample.text = "\"${item.example}\""

            // Update Expand/Collapse State
            updateExpandState(isExpanded)

            binding.buttonToggleExpand.setOnClickListener {
                toggleExpand(item.date)
            }

            binding.layoutWordHeader.setOnClickListener {
                toggleExpand(item.date)
            }

            binding.buttonListenAudio.setOnClickListener {
                onListenClicked(item)
            }

            binding.buttonPracticeSpeech.setOnClickListener {
                onPracticeClicked(item)
            }

            binding.buttonRemoveSaved.setOnClickListener {
                onRemoveSavedClicked(item)
            }
        }

        private fun toggleExpand(date: String) {
            if (collapsedDates.contains(date)) {
                collapsedDates.remove(date)
                updateExpandState(true)
            } else {
                collapsedDates.add(date)
                updateExpandState(false)
            }
        }

        private fun updateExpandState(isExpanded: Boolean) {
            if (isExpanded) {
                binding.layoutVocabDetails.visibility = View.VISIBLE
                binding.buttonToggleExpand.setImageResource(R.drawable.ic_expand_less)
            } else {
                binding.layoutVocabDetails.visibility = View.GONE
                binding.buttonToggleExpand.setImageResource(R.drawable.ic_expand_more)
            }
        }
    }

    class VocabDiffCallback : DiffUtil.ItemCallback<VocabWordEntity>() {
        override fun areItemsTheSame(oldItem: VocabWordEntity, newItem: VocabWordEntity): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: VocabWordEntity, newItem: VocabWordEntity): Boolean {
            return oldItem == newItem
        }
    }
}
