package com.example.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ProductivityApplication
import com.example.R
import com.example.data.DateUtils
import com.example.data.VocabWordEntity
import com.example.databinding.DialogAddHabitBinding
import com.example.databinding.FragmentHabitsBinding
import com.example.ui.adapters.HabitAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

class HabitsFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductivityViewModel by lazy {
        val app = requireActivity().application as ProductivityApplication
        ViewModelProvider(requireActivity(), ProductivityViewModelFactory(app.repository))[ProductivityViewModel::class.java]
    }

    private lateinit var habitAdapter: HabitAdapter
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var currentWord: VocabWordEntity? = null
    private var isVocabExpanded = true
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTts()
        setupRecyclerView()
        setupListeners()
        setupVocabListeners()
        observeData()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(requireContext().applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            textToSpeech?.setSpeechRate(0.92f) // slightly deliberate for clear pronunciation practice
        }
    }

    private fun setupRecyclerView() {
        habitAdapter = HabitAdapter(
            onToggleCompleted = { habitId, isChecked ->
                viewModel.toggleHabitCompletion(habitId, isChecked)
            },
            onDeleteClicked = { habitWithStatus ->
                showDeleteConfirmationDialog(habitWithStatus.habit.id, habitWithStatus.habit.name)
            }
        )
        binding.recyclerHabits.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAddHabit.setOnClickListener {
            showAddHabitDialog()
        }
    }

    private fun setupVocabListeners() {
        // Expand/Collapse toggle
        binding.buttonToggleVocabExpand.setOnClickListener {
            isVocabExpanded = !isVocabExpanded
            updateVocabExpandState()
        }

        // Listen Audio (Pronunciation)
        binding.buttonListenAudio.setOnClickListener {
            currentWord?.let { word ->
                speakOrPlayWord(word)
            }
        }

        // Bookmark / Favorite toggle
        binding.buttonSaveVocab.setOnClickListener {
            currentWord?.let { word ->
                viewModel.toggleSaveWord(word)
                val msg = if (!word.isSaved) getString(R.string.vocab_saved) else getString(R.string.vocab_unsaved)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        // Practice Speaking action
        binding.buttonPracticeSpeech.setOnClickListener {
            currentWord?.let { word ->
                practiceSpeakingDialog(word)
            }
        }
    }

    private fun updateVocabExpandState() {
        if (isVocabExpanded) {
            binding.layoutVocabDetails.visibility = View.VISIBLE
            binding.buttonToggleVocabExpand.setImageResource(R.drawable.ic_expand_less)
        } else {
            binding.layoutVocabDetails.visibility = View.GONE
            binding.buttonToggleVocabExpand.setImageResource(R.drawable.ic_expand_more)
        }
    }

    private fun speakOrPlayWord(word: VocabWordEntity) {
        if (!word.audioUrl.isNullOrBlank()) {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .build()
                    )
                    setDataSource(word.audioUrl)
                    setOnPreparedListener { start() }
                    setOnErrorListener { _, _, _ ->
                        fallbackTtsSpeak(word.word)
                        true
                    }
                    prepareAsync()
                }
                return
            } catch (_: Exception) {
                fallbackTtsSpeak(word.word)
            }
        } else {
            fallbackTtsSpeak(word.word)
        }
    }

    private fun fallbackTtsSpeak(text: String) {
        if (isTtsReady && textToSpeech != null) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vocab_utterance")
            Toast.makeText(requireContext(), "Pronouncing: $text", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Pronunciation: ${currentWord?.phonetic ?: text}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun practiceSpeakingDialog(word: VocabWordEntity) {
        val dialogueText = word.sampleDialogue
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Practice Speaking: ${word.word}")
            .setMessage(
                "Phonetic: ${word.phonetic}\n\n" +
                        "💡 Tip: ${word.speakingTip}\n\n" +
                        "Try reading this aloud:\n\"${word.example}\""
            )
            .setPositiveButton("Listen to Sentence") { _, _ ->
                if (isTtsReady && textToSpeech != null) {
                    textToSpeech?.speak(word.example, TextToSpeech.QUEUE_FLUSH, null, "sentence_utterance")
                }
            }
            .setNeutralButton("Listen to Word") { _, _ ->
                speakOrPlayWord(word)
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Observe Word of the Day
                launch {
                    viewModel.wordOfTheDay.collect { vocab ->
                        currentWord = vocab
                        if (vocab != null) {
                            binding.cardWordOfTheDay.visibility = View.VISIBLE
                            binding.textVocabWord.text = vocab.word
                            binding.textVocabPhonetic.text = vocab.phonetic
                            binding.textVocabPos.text = "• ${vocab.partOfSpeech}"
                            binding.textVocabDefinition.text = vocab.definition
                            binding.textVocabSpeakingTip.text = vocab.speakingTip
                            binding.textVocabDialogue.text = vocab.sampleDialogue

                            val bookmarkIcon = if (vocab.isSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border
                            binding.buttonSaveVocab.setImageResource(bookmarkIcon)

                            val tint = if (vocab.isSaved) {
                                ContextCompat.getColor(requireContext(), R.color.app_text_primary)
                            } else {
                                ContextCompat.getColor(requireContext(), R.color.app_text_secondary)
                            }
                            binding.buttonSaveVocab.setColorFilter(tint)
                        } else {
                            binding.cardWordOfTheDay.visibility = View.GONE
                        }
                    }
                }

                // 2. Observe Habits for today
                launch {
                    viewModel.habitsForDate.collect { habits ->
                        habitAdapter.submitList(habits)

                        val today = DateUtils.getTodayDateString()
                        binding.textDateHeader.text = DateUtils.formatHeaderDate(today).uppercase()

                        if (habits.isEmpty()) {
                            binding.layoutEmptyHabits.visibility = View.VISIBLE
                            binding.recyclerHabits.visibility = View.GONE
                            binding.textHabitsCompleted.text = "No habits for today"
                            binding.progressIndicator.progress = 0
                            binding.textHabitCount.text = "0 habits"
                        } else {
                            binding.layoutEmptyHabits.visibility = View.GONE
                            binding.recyclerHabits.visibility = View.VISIBLE

                            val completedCount = habits.count { it.isCompleted }
                            val totalCount = habits.size
                            val percent = (completedCount * 100) / totalCount

                            binding.textHabitsCompleted.text =
                                getString(R.string.habits_completed_format, completedCount, totalCount) +
                                        " ($percent%)"
                            binding.progressIndicator.progress = percent
                            binding.textHabitCount.text = "$totalCount habits"
                        }
                    }
                }
            }
        }
    }

    private fun showAddHabitDialog() {
        val dialogBinding = DialogAddHabitBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_habit_dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val habitName = dialogBinding.inputHabitName.text?.toString()?.trim().orEmpty()
                if (habitName.isBlank()) {
                    dialogBinding.layoutHabitInput.error = getString(R.string.habit_name_error)
                } else {
                    dialogBinding.layoutHabitInput.error = null
                    viewModel.addHabit(habitName)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(habitId: Long, habitName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_habit_title)
            .setMessage(getString(R.string.delete_habit_confirm, habitName))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteHabit(habitId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTtsReady = false
        _binding = null
    }
}
