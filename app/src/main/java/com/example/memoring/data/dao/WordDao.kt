package com.example.memoring.data.dao

import androidx.room.*
import com.example.memoring.data.entity.WordEntity

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

}