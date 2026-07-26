package com.example.memoring.data.dao

import androidx.room.*
import com.example.memoring.data.entity.QuizLogEntity
import com.example.memoring.data.entity.QuizSessionEntity

@Dao
interface QuizDao {
    @Insert
    suspend fun insertQuizSession(session: QuizSessionEntity): Long

    @Insert
    suspend fun insertQuizLogs(logs: List<QuizLogEntity>)

    @Query("SELECT * FROM quiz_sessions WHERE userId = :userId ORDER BY quizSessionId DESC")
    suspend fun getQuizHistory(userId: Int): List<QuizSessionEntity>
}