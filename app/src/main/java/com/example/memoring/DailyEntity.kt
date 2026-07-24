package com.example.memoring.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_learning",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // user_id와 learning_date 조합 중복 방지 제약조건
    indices = [Index(value = ["userId", "learningDate"], unique = true)]
)
data class DailyEntity(
    @PrimaryKey(autoGenerate = true)
    val dailyLearningId: Int = 0,
    val userId: Int,
    val learningDate: String, // "YYYY-MM-DD"
    val studiedCount: Int = 0,
    val memorizedCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0
)