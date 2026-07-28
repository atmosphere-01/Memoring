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
    val categoryId: Int, //카테고리 ID
    val word: String, //영단어
    val meaning: String, // 뜻
    val partOfSpeech: String? = null, //품사
    val exampleSentence: String? = null, //예문
    val createdAt: Long = System.currentTimeMillis() //등록날짜
)