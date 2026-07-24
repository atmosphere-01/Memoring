package com.example.memoring.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,
    val username: String,
    val continuousDay: Int = 0,
    val lastLearningDate: String? = null, // "YYYY-MM-DD" (플래시카드 학습 기준)
    val createdAt: String // "YYYY-MM-DD HH:mm:ss"
)