package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocab_words")
data class VocabWordEntity(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String,
    val speakingTip: String,
    val sampleDialogue: String,
    val audioUrl: String? = null,
    val isSaved: Boolean = false
)
