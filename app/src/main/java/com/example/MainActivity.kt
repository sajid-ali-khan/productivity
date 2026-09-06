package com.example

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.data.AppPreferences
import com.example.databinding.ActivityMainBinding
import com.example.ui.HabitsFragment
import com.example.ui.PreferencesBottomSheetFragment
import com.example.ui.ReportsFragment
import com.example.ui.SavedVocabFragment
import com.example.ui.StudyTimerFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applyThemeMode(this)
        setTheme(AppPreferences.getFontThemeResId(this))

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateSystemBarAppearance()

        setSupportActionBar(binding.topToolbar)

        setupBackStackNavigation()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_habits
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    true
                } else {
                    super.onOptionsItemSelected(item)
                }
            }
            R.id.action_saved_vocab -> {
                openSavedVocabFragment()
                true
            }
            R.id.action_preferences -> {
                val bottomSheet = PreferencesBottomSheetFragment()
                bottomSheet.show(supportFragmentManager, PreferencesBottomSheetFragment.TAG)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openSavedVocabFragment() {
        supportActionBar?.title = getString(R.string.saved_vocabulary)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SavedVocabFragment())
            .addToBackStack(SavedVocabFragment.TAG)
            .commit()
    }

    private fun setupBackStackNavigation() {
        supportFragmentManager.addOnBackStackChangedListener {
            val isBackStackEmpty = supportFragmentManager.backStackEntryCount == 0
            supportActionBar?.setDisplayHomeAsUpEnabled(!isBackStackEmpty)
            supportActionBar?.setDisplayShowHomeEnabled(!isBackStackEmpty)

            if (isBackStackEmpty) {
                // Restore title based on current selected bottom nav item
                updateTitleForCurrentTab()
            }
        }
    }

    private fun updateTitleForCurrentTab() {
        val titleRes = when (binding.bottomNavigation.selectedItemId) {
            R.id.nav_habits -> R.string.habits_title
            R.id.nav_timer -> R.string.study_timer_title
            R.id.nav_reports -> R.string.reports_title
            else -> R.string.app_name
        }
        supportActionBar?.title = getString(titleRes)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Clear any sub-screens on the backstack when switching tabs
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }

            when (item.itemId) {
                R.id.nav_habits -> {
                    switchFragment(HabitsFragment(), getString(R.string.habits_title))
                    true
                }
                R.id.nav_timer -> {
                    switchFragment(StudyTimerFragment(), getString(R.string.study_timer_title))
                    true
                }
                R.id.nav_reports -> {
                    switchFragment(ReportsFragment(), getString(R.string.reports_title))
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: Fragment, title: String) {
        supportActionBar?.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun updateSystemBarAppearance() {
        val isDarkMode = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        // isAppearanceLightStatusBars = true produces dark icons on light background (Light mode)
        // isAppearanceLightStatusBars = false produces light/white icons on dark background (Dark mode)
        insetsController.isAppearanceLightStatusBars = !isDarkMode
        insetsController.isAppearanceLightNavigationBars = !isDarkMode
    }
}
