package com.example.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ProductivityApplication
import com.example.R
import com.example.data.VocabWordEntity
import com.example.databinding.FragmentSavedVocabBinding
import com.example.ui.adapters.SavedVocabAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

class SavedVocabFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentSavedVocabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductivityViewModel by lazy {
        val app = requireActivity().application as ProductivityApplication
        ViewModelProvider(requireActivity(), ProductivityViewModelFactory(app.repository))[ProductivityViewModel::class.java]
    }

    private lateinit var savedVocabAdapter: SavedVocabAdapter
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var mediaPlayer: MediaPlayer? = null

    private var allSavedWords: List<VocabWordEntity> = emptyList()
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedVocabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTts()
        setupRecyclerView()
        setupSearch()
        observeSavedWords()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(requireContext().applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            textToSpeech?.setSpeechRate(0.92f)
        }
    }

    private fun setupRecyclerView() {
        savedVocabAdapter = SavedVocabAdapter(
            onListenClicked = { word ->
                speakWord(word)
            },
            onPracticeClicked = { word ->
                showPracticeDialog(word)
            },
            onRemoveSavedClicked = { word ->
                viewModel.toggleSaveWord(word)
                Toast.makeText(requireContext(), R.string.vocab_unsaved, Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerSavedVocab.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = savedVocabAdapter
        }
    }

    private fun setupSearch() {
        binding.inputSearchVocab.doAfterTextChanged { text ->
            currentSearchQuery = text?.toString()?.trim().orEmpty()
            binding.buttonClearSearch.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
            filterAndDisplayWords()
        }

        binding.buttonClearSearch.setOnClickListener {
            binding.inputSearchVocab.text?.clear()
        }
    }

    private fun observeSavedWords() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.savedVocabWords.collect { savedList ->
                    allSavedWords = savedList
                    filterAndDisplayWords()
                }
            }
        }
    }

    private fun filterAndDisplayWords() {
        if (_binding == null) return

        if (allSavedWords.isEmpty()) {
            binding.layoutHeaderText.visibility = View.VISIBLE
            binding.textSavedSubheading.text = resources.getQuantityString(
                R.plurals.saved_words_count,
                0,
                0
            )
            binding.cardSearchBar.visibility = View.GONE
            binding.recyclerSavedVocab.visibility = View.GONE
            binding.layoutNoSearchResults.visibility = View.GONE
            binding.layoutEmptySaved.visibility = View.VISIBLE
            savedVocabAdapter.submitList(emptyList())
            return
        }

        binding.cardSearchBar.visibility = View.VISIBLE
        binding.layoutEmptySaved.visibility = View.GONE

        val filtered = if (currentSearchQuery.isEmpty()) {
            allSavedWords
        } else {
            val queryLower = currentSearchQuery.lowercase()
            allSavedWords.filter { item ->
                item.word.lowercase().contains(queryLower) ||
                        item.definition.lowercase().contains(queryLower) ||
                        item.example.lowercase().contains(queryLower) ||
                        item.partOfSpeech.lowercase().contains(queryLower)
            }
        }

        // Subheading Count
        if (currentSearchQuery.isEmpty()) {
            binding.textSavedSubheading.text = resources.getQuantityString(
                R.plurals.saved_words_count,
                allSavedWords.size,
                allSavedWords.size
            )
        } else {
            binding.textSavedSubheading.text = getString(
                R.string.showing_search_count,
                filtered.size,
                allSavedWords.size
            )
        }

        if (filtered.isEmpty()) {
            binding.recyclerSavedVocab.visibility = View.GONE
            binding.layoutNoSearchResults.visibility = View.VISIBLE
            binding.textNoResultsDesc.text = getString(R.string.no_search_results_desc, currentSearchQuery)
        } else {
            binding.layoutNoSearchResults.visibility = View.GONE
            binding.recyclerSavedVocab.visibility = View.VISIBLE
        }

        savedVocabAdapter.submitList(filtered)
    }

    private fun speakWord(word: VocabWordEntity) {
        if (isTtsReady && textToSpeech != null) {
            textToSpeech?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, "saved_vocab_word_utterance")
            Toast.makeText(requireContext(), "Pronouncing: ${word.word}", Toast.LENGTH_SHORT).show()
        } else if (textToSpeech != null) {
            textToSpeech?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, "saved_vocab_word_utterance")
            Toast.makeText(requireContext(), "Pronouncing: ${word.word}", Toast.LENGTH_SHORT).show()
        } else {
            initTts()
            Toast.makeText(requireContext(), "Pronunciation: ${word.phonetic.ifBlank { word.word }}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPracticeDialog(word: VocabWordEntity) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Practice Speaking: ${word.word}")
            .setMessage(
                "Phonetic: ${word.phonetic}\n\n" +
                        "💡 Tip: ${word.speakingTip}\n\n" +
                        "Try reading this aloud:\n\"${word.example}\""
            )
            .setPositiveButton("Listen to Sentence", null)
            .setNeutralButton("Listen to Word", null)
            .setNegativeButton(R.string.close, null)
            .create()

        dialog.show()

        // Override click listeners so the dialog stays open while practicing
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            if (isTtsReady && textToSpeech != null) {
                textToSpeech?.speak(word.example, TextToSpeech.QUEUE_FLUSH, null, "sentence_utterance")
            } else if (textToSpeech != null) {
                textToSpeech?.speak(word.example, TextToSpeech.QUEUE_FLUSH, null, "sentence_utterance")
            }
        }

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
            speakWord(word)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        _binding = null
    }

    companion object {
        const val TAG = "SavedVocabFragment"
    }
}
