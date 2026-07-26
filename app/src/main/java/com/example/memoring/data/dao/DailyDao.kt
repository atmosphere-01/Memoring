package com.example.memoring.data.dao

import androidx.room.*
import com.example.memoring.data.entity.DailyEntity

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
}