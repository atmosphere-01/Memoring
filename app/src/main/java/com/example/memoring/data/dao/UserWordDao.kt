package com.example.memoring.data.dao

import androidx.room.*
import com.example.memoring.data.entity.UserWordEntity

@Dao
interface UserWordDao {
    // 사용자 단어 상태 저장 또는 업데이트 (UPSERT)
    @Upsert
    suspend fun upsertUserWord(userWord: UserWordEntity)

    // 특정 사용자의 특정 단어 상태 조회
    @Query("SELECT * FROM user_words WHERE userId = :userId AND wordId = :wordId")
    suspend fun getUserWord(userId: Int, wordId: Int): UserWordEntity?

    // 즐겨찾기(북마크) 단어 목록만 조회
    @Query("SELECT * FROM user_words WHERE userId = :userId AND isFavorite = 1")
    suspend fun getFavoriteWords(userId: Int): List<UserWordEntity>

    // 특정 암기 상태(예: KNOWN, REVIEW)인 단어 목록 조회
    @Query("SELECT * FROM user_words WHERE userId = :userId AND memorizationStatus = :status")
    suspend fun getUserWordsByStatus(userId: Int, status: String): List<UserWordEntity>

    @Query("SELECT * FROM user_words WHERE userId = :userId AND wordId = :wordId")
    suspend fun get(userId: Int, wordId: Int): UserWordEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: UserWordEntity): Long

    @Query("UPDATE user_words SET memorizationStatus = :status WHERE userId = :userId AND wordId = :wordId")
    suspend fun updateStatus(userId: Int, wordId: Int, status: String): Int

    @Query("UPDATE user_words SET correctCnt = correctCnt + :correct, wrongCnt = wrongCnt + :wrong, memorizationStatus = :status WHERE userId = :userId AND wordId = :wordId")
    suspend fun updateQuizResult(userId: Int, wordId: Int, correct: Int, wrong: Int, status: String): Int

    @Query("SELECT COUNT(*) FROM user_words WHERE userId = :userId AND memorizationStatus = 'KNOWN'")
    suspend fun countMemorized(userId: Int): Int
}
