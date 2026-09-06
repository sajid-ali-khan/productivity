package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [HabitEntity::class, HabitLogEntity::class, StudySessionEntity::class, VocabWordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun studyDao(): StudyDao
    abstract fun vocabDao(): VocabDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "productivity_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.habitDao(), database.studyDao())
                    }
                }
            }

            suspend fun populateInitialData(habitDao: HabitDao, studyDao: StudyDao) {
                // Pre-populate standard starter habits for productivity
                val initialHabits = listOf(
                    "Read 20 pages",
                    "Deep focus study session",
                    "Physical exercise / walk",
                    "Plan tomorrow's priorities"
                )
                initialHabits.forEach { name ->
                    habitDao.insertHabit(HabitEntity(name = name))
                }
            }
        }
    }
}
