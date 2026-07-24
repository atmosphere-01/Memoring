package com.example.memoring.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_logs",
    foreignKeys = [
        ForeignKey(
            entity = QuizSessionEntity::class,
            parentColumns = ["quizSessionId"],
            childColumns = ["quizSessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["wordId"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QuizLogEntity(
    @PrimaryKey(autoGenerate = true)
    val quizLogId: Int = 0,
    val quizSessionId: Int,
    val wordId: Int,
    val selectedAnswer: String,
    val isCorrect: Boolean
)