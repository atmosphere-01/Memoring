package com.example.memoring.data.dao

import androidx.room.*
import com.example.memoring.data.entity.DailyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyDao {
    // 오늘 날짜 기록이 있으면 UPDATE, 없으면 INSERT (UNIQUE 제약조건 자동 활용)
    @Upsert
    suspend fun upsertDailyLearning(daily: DailyEntity)

    // 특정 날짜의 학습 기록 조회 ("YYYY-MM-DD")
    @Query("SELECT * FROM daily_learning WHERE userId = :userId AND learningDate = :date")
    suspend fun getDailyByDate(userId: Int, date: String): DailyEntity?

    // 월별 학습 기록 전체 조회 (예: '2026-07%')
    @Query("SELECT * FROM daily_learning WHERE userId = :userId AND learningDate LIKE :yearMonth || '%'")
    suspend fun getMonthlyLearning(userId: Int, yearMonth: String): List<DailyEntity>

    @Query("SELECT * FROM daily_learning WHERE userId = :userId AND learningDate = :date")
    suspend fun get(userId: Int, date: String): DailyEntity?

    @Query("SELECT * FROM daily_learning WHERE userId = :userId AND learningDate = :date")
    fun observe(userId: Int, date: String): Flow<DailyEntity?>

    @Query("SELECT * FROM daily_learning WHERE userId = :userId AND learningDate BETWEEN :start AND :end ORDER BY learningDate")
    fun observeRange(userId: Int, start: String, end: String): Flow<List<DailyEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DailyEntity): Long

    @Query("UPDATE daily_learning SET studiedCount = studiedCount + :studied, correctCount = correctCount + :correct, wrongCount = wrongCount + :wrong, memorizedCount = :memorized WHERE userId = :userId AND learningDate = :date")
    suspend fun accumulate(userId: Int, date: String, studied: Int, correct: Int, wrong: Int, memorized: Int): Int
}
