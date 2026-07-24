package com.example.memoring.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QuizSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val quizSessionId: Int = 0,
    val userId: Int,
    val categoryId: Int,
    val quizCnt: Int = 0,
    val correctCnt: Int = 0,
    val wrongCnt: Int = 0,
    val lastTestedDate: String // "YYYY-MM-DD"
)