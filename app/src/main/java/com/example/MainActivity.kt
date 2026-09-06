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
            R.id.action_preferences -> {
                val bottomSheet = PreferencesBottomSheetFragment()
                bottomSheet.show(supportFragmentManager, PreferencesBottomSheetFragment.TAG)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
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
