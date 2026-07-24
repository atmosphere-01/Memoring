package com.example.memoring.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val wordId: Int = 0,
    val categoryId: Int,
    val word: String,
    val meaning: String,
    val partOfSpeech: String? = null,
    val exampleSentence: String? = null,
    val createdAt: String
)