package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {
    @Query("SELECT * FROM vocab_words WHERE date = :date LIMIT 1")
    fun getWordForDate(date: String): Flow<VocabWordEntity?>

    @Query("SELECT * FROM vocab_words WHERE date = :date LIMIT 1")
    suspend fun getWordForDateSync(date: String): VocabWordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: VocabWordEntity)

    @Query("UPDATE vocab_words SET isSaved = :isSaved WHERE date = :date")
    suspend fun updateSaved(date: String, isSaved: Boolean)

    @Query("SELECT * FROM vocab_words ORDER BY date DESC")
    fun getAllVocabHistory(): Flow<List<VocabWordEntity>>
}
