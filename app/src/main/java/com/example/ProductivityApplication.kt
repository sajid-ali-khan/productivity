package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.ProductivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ProductivityApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy {
        ProductivityRepository(
            database.habitDao(),
            database.studyDao(),
            database.vocabDao(),
            database.taskDao()
        )
    }
}
