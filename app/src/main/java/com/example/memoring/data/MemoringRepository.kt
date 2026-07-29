package com.example.memoring.data

import androidx.room.withTransaction
import com.example.memoring.data.entity.DailyEntity
import com.example.memoring.data.entity.QuizLogEntity
import com.example.memoring.data.entity.QuizSessionEntity
import com.example.memoring.data.entity.UserWordEntity
import com.example.memoring.domain.DateProvider
import com.example.memoring.domain.LearningStreakCalculator
import com.example.memoring.domain.MemorizationStatusPolicy
import com.example.memoring.domain.model.MemorizationStatus
import java.time.LocalDate

class MemoringRepository(
    private val db: AppDatabase,
    private val dates: DateProvider,
    private val streaks: LearningStreakCalculator = LearningStreakCalculator(),
    private val statusPolicy: MemorizationStatusPolicy = MemorizationStatusPolicy()
) {
    suspend fun quizCandidates(userId: Int, categoryId: Int?) =
        db.wordDao().getQuizCandidates(userId, categoryId)

    suspend fun createQuizSession(userId: Int, categoryId: Int?, count: Int): Int {
        requireNotNull(categoryId) { "퀴즈 카테고리를 선택해 주세요." }
        val id = db.quizDao().insertSession(
            QuizSessionEntity(
                userId = userId,
                categoryId = categoryId,
                quizCnt = count,
                lastTestedDate = dates.today().toString()
            )
        )
        check(id > 0) { "퀴즈 세션 생성에 실패했습니다." }
        return id.toInt()
    }

    suspend fun saveFlashcardProgress(
        userId: Int,
        wordId: Int,
        status: MemorizationStatus,
        countAsStudied: Boolean,
        completeSession: Boolean
    ) = db.withTransaction {
        val old = db.userWordDao().get(userId, wordId)
        if (old == null) {
            db.userWordDao().insert(
                UserWordEntity(userId = userId, wordId = wordId, memorizationStatus = status.name)
            )
        } else {
            check(db.userWordDao().updateStatus(userId, wordId, status.name) == 1)
        }
        val today = dates.today()
        ensureDaily(userId, today.toString())
        check(
            db.dailyDao().accumulate(
                userId, today.toString(), if (countAsStudied) 1 else 0, 0, 0,
                db.userWordDao().countMemorized(userId)
            ) == 1
        )
        if (completeSession) {
            val user = checkNotNull(db.userDao().getById(userId))
            val result = streaks.calculate(
                user.continuousDay,
                user.lastLearningDate?.let(LocalDate::parse),
                today
            )
            check(
                db.userDao().updateLearningStreak(
                    userId, result.continuousDay, today.toString()
                ) == 1
            )
        }
    }

    suspend fun saveQuizAnswer(
        sessionId: Int,
        userId: Int,
        wordId: Int,
        answer: String,
        correct: Boolean
    ): Boolean = db.withTransaction {
        if (db.quizDao().answerCount(sessionId, wordId) > 0) return@withTransaction false
        var userWord = db.userWordDao().get(userId, wordId)
        if (userWord == null) {
            db.userWordDao().insert(UserWordEntity(userId = userId, wordId = wordId))
            userWord = checkNotNull(db.userWordDao().get(userId, wordId))
        }
        val next = statusPolicy.resolve(
            MemorizationStatus.valueOf(userWord.memorizationStatus), correct
        )
        check(
            db.quizDao().insertLog(
                QuizLogEntity(
                    quizSessionId = sessionId,
                    wordId = wordId,
                    selectedAnswer = answer,
                    isCorrect = correct
                )
            ) > 0
        )
        check(
            db.userWordDao().updateQuizResult(
                userId, wordId, if (correct) 1 else 0, if (correct) 0 else 1, next.name
            ) == 1
        )
        check(
            db.quizDao().incrementSession(
                sessionId, if (correct) 1 else 0, if (correct) 0 else 1
            ) == 1
        )
        val today = dates.today().toString()
        ensureDaily(userId, today)
        check(
            db.dailyDao().accumulate(
                userId, today, 0, if (correct) 1 else 0, if (correct) 0 else 1,
                db.userWordDao().countMemorized(userId)
            ) == 1
        )
        true
    }

    private suspend fun ensureDaily(userId: Int, date: String) {
        if (db.dailyDao().get(userId, date) == null) {
            db.dailyDao().insert(DailyEntity(userId = userId, learningDate = date))
        }
    }

    fun observeUser(id: Int) = db.userDao().observeById(id)
    fun observeWordStatistics(id: Int) = db.wordDao().observeStatistics(id)
    fun observeQuizStatistics(id: Int) = db.quizDao().observeStatistics(id)
    fun observeDaily(id: Int, date: String) = db.dailyDao().observe(id, date)
    fun observeDailyRange(id: Int, start: String, end: String) =
        db.dailyDao().observeRange(id, start, end)

    suspend fun getWordsWithStatus(userId: Int, categoryId: Int?): List<com.example.memoring.data.WordListItem> {
        val words = if (categoryId != null) {
            db.wordDao().getWordsByCategory(categoryId)
        } else {
            db.categoryDao().getCategoriesByUserId(userId).flatMap {
                db.wordDao().getWordsByCategory(it.categoryId)
            }
        }
        return words.map { word ->
            val userWord = db.userWordDao().get(userId, word.wordId)
            com.example.memoring.data.WordListItem(
                wordId = word.wordId,
                word = word.word,
                meaning = word.meaning,
                partOfSpeech = word.partOfSpeech,
                exampleSentence = word.exampleSentence,
                categoryId = word.categoryId,
                isFavorite = userWord?.isFavorite ?: false,
                memorizationStatus = userWord?.memorizationStatus ?: "UNLEARNED"
            )
        }
    }

    suspend fun updateWordInfo(word: com.example.memoring.data.entity.WordEntity) =
        db.wordDao().updateWord(word)

    suspend fun deleteWordById(userId: Int, wordId: Int, categoryId: Int) {
        val words = db.wordDao().getWordsByCategory(categoryId)
        val target = words.find { it.wordId == wordId } ?: return
        db.wordDao().deleteWord(target)
    }

    suspend fun toggleFavorite(userId: Int, wordId: Int, isFavorite: Boolean) {
        val userWord = db.userWordDao().get(userId, wordId)
        if (userWord != null) {
            db.userWordDao().upsertUserWord(userWord.copy(isFavorite = isFavorite))
        }
    }

    suspend fun getCategories(userId: Int) = db.categoryDao().getCategoriesByUserId(userId)

    suspend fun updateWordFields(
        wordId: Int,
        categoryId: Int,
        meaning: String,
        partOfSpeech: String?,
        example: String?
    ) {
        val words = db.wordDao().getWordsByCategory(categoryId)
        val target = words.find { it.wordId == wordId } ?: return
        val updated = target.copy(
            meaning = meaning,
            partOfSpeech = partOfSpeech,
            exampleSentence = example
        )
        db.wordDao().updateWord(updated)
    }

    suspend fun updateMemorizationStatus(userId: Int, wordId: Int, status: String) {
        db.userWordDao().updateStatus(userId, wordId, status)
    }


}
