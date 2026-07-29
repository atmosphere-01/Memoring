package com.example.memoring.data.dao

import androidx.room.*
import com.example.memoring.data.entity.QuizLogEntity
import com.example.memoring.data.entity.QuizSessionEntity
import kotlinx.coroutines.flow.Flow

data class QuizStatisticsRow(
    val sessions: Int,
    val questions: Int,
    val correct: Int,
    val wrong: Int
)

@Dao
interface QuizDao {
    @Insert
    suspend fun insertQuizSession(session: QuizSessionEntity): Long

    @Insert
    suspend fun insertQuizLogs(logs: List<QuizLogEntity>)

    @Query("SELECT * FROM quiz_sessions WHERE userId = :userId ORDER BY quizSessionId DESC")
    suspend fun getQuizHistory(userId: Int): List<QuizSessionEntity>

    @Insert
    suspend fun insertSession(entity: QuizSessionEntity): Long

    @Insert
    suspend fun insertLog(entity: QuizLogEntity): Long

    @Query("UPDATE quiz_sessions SET correctCnt = correctCnt + :correct, wrongCnt = wrongCnt + :wrong WHERE quizSessionId = :id")
    suspend fun incrementSession(id: Int, correct: Int, wrong: Int): Int

    @Query("SELECT COUNT(*) FROM quiz_logs WHERE quizSessionId = :sessionId AND wordId = :wordId")
    suspend fun answerCount(sessionId: Int, wordId: Int): Int

    @Query("SELECT COUNT(*) sessions, COALESCE(SUM(quizCnt), 0) questions, COALESCE(SUM(correctCnt), 0) correct, COALESCE(SUM(wrongCnt), 0) wrong FROM quiz_sessions WHERE userId = :userId")
    fun observeStatistics(userId: Int): Flow<QuizStatisticsRow>
}
