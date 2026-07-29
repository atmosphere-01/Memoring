package com.example.memoring.data.dao

import androidx.room.*
import com.example.memoring.data.entity.WordEntity
import kotlinx.coroutines.flow.Flow

data class QuizCandidateRow(
    val wordId: Int,
    val categoryId: Int,
    val word: String,
    val meaning: String,
    val memorizationStatus: String?,
    val wrongCnt: Int?
)

data class WordStatisticsRow(
    val total: Int,
    val learned: Int,
    val memorized: Int,
    val review: Int,
    val favorite: Int
)

//단어 데이터 관리
@Dao
interface WordDao {

    // csv 파일로 영어단어 추가
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>): List<Long>

    // 영어단어 사용자 직접 추가
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity): Long

    // 특정 카테고리에 속한 모든 단어 조회
    @Query("SELECT * FROM words WHERE categoryId = :categoryId")
    suspend fun getWordsByCategory(categoryId: Int): List<WordEntity>

    // 전체 단어 조회 (단어 목록 화면용)
    @Query("SELECT * FROM words ORDER BY wordId")
    suspend fun getAllWords(): List<WordEntity>

    // 전체 등록 단어 수 (홈 '전체 단어')
    @Query("SELECT COUNT(*) FROM words")
    suspend fun countAllWords(): Int

    // 단어 삭제
    @Delete
    suspend fun deleteWord(word: WordEntity)

    //단어 수정
    @Update
    suspend fun updateWord(word: WordEntity)

    // 💡 예문(exampleSentence)이 아직 없는 단어들만 가져오기(2026-7-25)
    @Query("SELECT * FROM words WHERE categoryId = :categoryId AND (exampleSentence IS NULL OR exampleSentence = '')")
    suspend fun getWordsWithoutExample(categoryId: Int): List<WordEntity>

    // 💡 특정 카테고리에 이미 같은 단어가 있는지 확인
    @Query("SELECT EXISTS(SELECT 1 FROM words WHERE categoryId = :categoryId AND LOWER(word) = LOWER(:word))")
    suspend fun isWordExists(categoryId: Int, word: String): Boolean

    @Query(
        """SELECT w.wordId, w.categoryId, w.word, w.meaning, uw.memorizationStatus, uw.wrongCnt
           FROM words w
           JOIN categories c ON c.categoryId = w.categoryId
           LEFT JOIN user_words uw ON uw.wordId = w.wordId AND uw.userId = :userId
           WHERE c.userId = :userId AND (:categoryId IS NULL OR w.categoryId = :categoryId)"""
    )
    suspend fun getQuizCandidates(userId: Int, categoryId: Int?): List<QuizCandidateRow>

    @Query(
        """SELECT COUNT(w.wordId) total,
           COALESCE(SUM(CASE WHEN uw.memorizationStatus IN ('UNKNOWN','CONFUSED','KNOWN') THEN 1 ELSE 0 END), 0) learned,
           COALESCE(SUM(CASE WHEN uw.memorizationStatus = 'KNOWN' THEN 1 ELSE 0 END), 0) memorized,
           COALESCE(SUM(CASE WHEN uw.memorizationStatus IN ('UNKNOWN','CONFUSED') OR (uw.memorizationStatus = 'KNOWN' AND uw.wrongCnt > 0) THEN 1 ELSE 0 END), 0) review,
           COALESCE(SUM(CASE WHEN uw.isFavorite = 1 THEN 1 ELSE 0 END), 0) favorite
           FROM words w
           JOIN categories c ON c.categoryId = w.categoryId
           LEFT JOIN user_words uw ON uw.wordId = w.wordId AND uw.userId = :userId
           WHERE c.userId = :userId"""
    )
    fun observeStatistics(userId: Int): Flow<WordStatisticsRow>

}
