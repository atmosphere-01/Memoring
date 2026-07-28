//TEST 완료
package com.example.memoring

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.memoring.data.AppDatabase
import com.example.memoring.data.api.ApiClient
import com.example.memoring.data.api.safeTranslateText
import com.example.memoring.data.entity.CategoryEntity
import com.example.memoring.data.entity.UserEntity
import com.example.memoring.data.entity.WordEntity
import com.example.memoring.data.repository.WordRepository
import com.example.memoring.data.util.CsvHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class DatabaseAndRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // 테스트용 DB
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun `1_기본_DB_생성_및_유저_조회_테스트`() {
        runBlocking {
            val testUser = UserEntity(username = "테스트유저",
                createdAt = getCurrentFormattedTime())
            val userId = database.userDao().insertUser(testUser)

            val fetchedUser = database.userDao().getUserById(userId.toInt())
            assertNotNull(fetchedUser)
            assertEquals("테스트유저", fetchedUser?.username)
            Log.d("DB_TEST", "DB 생성 및 유저 저장 완료 ID: $userId")
        }
    }

    @Test
    fun `2_MyMemory_번역_API_테스트`() {
        runBlocking {
            try {
                val englishText = "study"
                val response = ApiClient.myMemoryApi.safeTranslateText(
                    text = englishText,
                    langPair = "en|ko",
                    email = "soyean8751@gmail.com"
                )

                val translatedText = response.responseData?.translatedText
                assertNotNull(translatedText)
                Log.d("MYMEMORY_TEST", "MyMemory 번역 결과: $translatedText")
            } catch (e: Exception) {
                Log.e("MYMEMORY_TEST", "번역 API 실패: ${e.message}", e)
                throw e
            }
        }
    }

    @Test
    fun `3_외래키_제약조건_테스트`(){
        runBlocking {
            val userId = database.userDao().insertUser(
                UserEntity(username = "외래키테스터",
                    createdAt = getCurrentFormattedTime())
            ).toInt()

            val validCategoryId = database.categoryDao().insertCategory(
                CategoryEntity(userId = userId, categoryName = "TOEIC 필수 단어")
            ).toInt()

            //올바른 카테고리에 저장
            val validWord = WordEntity(
                categoryId = validCategoryId,
                word = "apple",
                meaning = "사과"
            )
            val wordId = database.wordDao().insertWord(validWord)
            assertTrue(wordId > 0)
            Log.d("FK_TEST", "올바른 카테고리 저장 성공! ID: $wordId")

            //존재하지 않는 카테고리 저장 실패(차단) 테스트
            var isExceptionThrown = false
            try {
                val invalidWord = WordEntity(
                    categoryId = 999999, // DB에 없는 카테고리 ID
                    word = "banana",
                    meaning = "바나나"
                )
                database.wordDao().insertWord(invalidWord)
            } catch (e: Exception) {
                isExceptionThrown = true
                Log.d("FK_TEST", "외래키 제약조건 차단 성공! (${e.message})")
            }
            assertTrue("존재하지 않는 카테고리 삽입 시 예외가 발생해야 합니다.", isExceptionThrown)
        }
    }

    @Test
    fun `4_WordRepository_통합_테스트`() {
        runBlocking {
            val userId = database.userDao().insertUser(
                UserEntity(username = "통합테스트유저", createdAt = getCurrentFormattedTime())
            ).toInt()

            val categoryId = database.categoryDao().insertCategory(
                CategoryEntity(userId = userId, categoryName = "기본 단어장")
            ).toInt()

            val repository = WordRepository(database.wordDao())
            val isSuccess = repository.processAndSaveWord(
                englishWord = "study",
                meaningInput = null,
                categoryId = categoryId,
                userEmail = "soyean8751@gmail.com"
            )

            assertTrue(isSuccess)
            Log.d("REPO_TEST", "Repository 연동 저장 결과 성공!")
        }
    }

    @Test
    fun `5_CSV_불러오기_및_예문_채우기_테스트`() {
        runBlocking {
            val userId = database.userDao().insertUser(
                UserEntity(username = "사전유저", createdAt= getCurrentFormattedTime())
            ).toInt()

            val categoryId = database.categoryDao().insertCategory(
                CategoryEntity(userId = userId, categoryName = "기본 사전")
            ).toInt()

            //CSV 읽어서 DB 삽입
            val csvWords = CsvHelper.readWordsFromCsv(context, categoryId)
            database.wordDao().insertWords(csvWords)
            Log.d("CSV_TEST", "CSV 단어 ${csvWords.size}개 삽입 완료")

            //Free Dictionary API로 예문 자동 채우기
            val repository = WordRepository(database.wordDao())
            repository.fetchAndFillExamplesForCategory(categoryId)

            //DB 검증
            val savedWords = database.wordDao().getWordsByCategory(categoryId)
            assertTrue(savedWords.isNotEmpty())

            savedWords.forEach { word ->
                Log.d(
                    "CSV_TEST",
                    "단어: ${word.word} | 뜻: ${word.meaning} | 예문: ${word.exampleSentence ?: "예문 없음"}"
                )
            }
        }
    }

    private fun getCurrentFormattedTime(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }
}