package com.example.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.AppPreferences
import com.example.databinding.BottomSheetPreferencesBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView

class PreferencesBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPreferencesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPreferencesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeSelector()
        setupFontSelector()
        setupAboutAndFeedback()

        binding.buttonClosePrefs.setOnClickListener {
            dismiss()
        }
    }

    private fun setupAboutAndFeedback() {
        binding.cardGithubRepo.setOnClickListener {
            openUrl(getString(R.string.github_repo_url))
        }

        binding.cardReportIssue.setOnClickListener {
            openUrl(getString(R.string.github_issues_url))
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open browser: $url", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupThemeSelector() {
        val currentTheme = AppPreferences.getThemeMode(requireContext())
        when (currentTheme) {
            "light" -> binding.toggleGroupTheme.check(R.id.btn_theme_light)
            "dark" -> binding.toggleGroupTheme.check(R.id.btn_theme_dark)
            else -> binding.toggleGroupTheme.check(R.id.btn_theme_system)
        }

        binding.toggleGroupTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newMode = when (checkedId) {
                    R.id.btn_theme_light -> "light"
                    R.id.btn_theme_dark -> "dark"
                    else -> "system"
                }
                if (newMode != currentTheme) {
                    AppPreferences.setThemeMode(requireContext(), newMode)
                    dismiss()
                    requireActivity().recreate()
                }
            }
        }
    }

    private fun setupFontSelector() {
        val currentFont = AppPreferences.getFontKey(requireContext())
        updateFontCardsSelection(currentFont)

        val fontCardMap = mapOf(
            binding.cardFontSpaceGrotesk to "space_grotesk",
            binding.cardFontEbGaramond to "eb_garamond",
            binding.cardFontExo2 to "exo2",
            binding.cardFontZillaSlab to "zilla_slab",
            binding.cardFontAcme to "acme",
            binding.cardFontSystem to "system"
        )

        for ((card, fontKey) in fontCardMap) {
            card.setOnClickListener {
                if (fontKey != currentFont) {
                    AppPreferences.setFontKey(requireContext(), fontKey)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.font_applied) + ": " + AppPreferences.getFontDisplayName(fontKey),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                    requireActivity().recreate()
                }
            }
        }
    }

    private fun updateFontCardsSelection(selectedKey: String) {
        val cards = listOf(
            Triple(binding.cardFontSpaceGrotesk, binding.checkSpaceGrotesk, "space_grotesk"),
            Triple(binding.cardFontEbGaramond, binding.checkEbGaramond, "eb_garamond"),
            Triple(binding.cardFontExo2, binding.checkExo2, "exo2"),
            Triple(binding.cardFontZillaSlab, binding.checkZillaSlab, "zilla_slab"),
            Triple(binding.cardFontAcme, binding.checkAcme, "acme"),
            Triple(binding.cardFontSystem, binding.checkSystem, "system")
        )

        val activeStroke = ContextCompat.getColor(requireContext(), R.color.app_text_primary)
        val defaultStroke = ContextCompat.getColor(requireContext(), R.color.app_stroke)

        for ((card, check, key) in cards) {
            val isSelected = key == selectedKey
            check.visibility = if (isSelected) View.VISIBLE else View.GONE
            card.strokeColor = if (isSelected) activeStroke else defaultStroke
            card.strokeWidth = if (isSelected) 4 else 2
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PreferencesBottomSheet"
    }
}
