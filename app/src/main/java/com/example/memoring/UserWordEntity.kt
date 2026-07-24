package com.example.memoring.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_words",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
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
data class UserWordEntity(
    @PrimaryKey(autoGenerate = true)
    val userWordId: Int = 0,
    val userId: Int,
    val wordId: Int,
    val isFavorite: Boolean = false,
    val memorizationStatus: String = "UNLEARNED", // UNLEARNED, UNKNOWN, CONFUSED, KNOWN
    val correctCnt: Int = 0,
    val wrongCnt: Int = 0
)