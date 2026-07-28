package com.example.memoring.domain

import com.example.memoring.data.MemoringRepository
import com.example.memoring.domain.model.LearningStatisticsUiModel
import kotlinx.coroutines.flow.combine
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

class LearningStatisticsUseCase(
    private val repository: MemoringRepository,
    private val dates: DateProvider
) {
    // 최근 학습 단어는 현재 스키마에 단어별 학습 시각/로그가 없어 임의 생성하지 않는다.
    // 필요하면 기존 플래시카드 로그의 timestamp를 조회하는 Flow를 이 combine에 추가한다.
    fun observe(userId: Int) = run {
        val today = dates.today()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        combine(
            repository.observeWordStatistics(userId),
            repository.observeQuizStatistics(userId),
            repository.observeDaily(userId, today.toString()),
            repository.observeDailyRange(userId, monday.toString(), today.toString())
        ) { words, quizzes, daily, week ->
            val weeklyCorrect = week.sumOf { it.correctCount }
            val weeklyWrong = week.sumOf { it.wrongCount }
            LearningStatisticsUiModel(
                totalWordCount = words.total, learnedWordCount = words.learned,
                memorizedWordCount = words.memorized, reviewWordCount = words.review,
                favoriteWordCount = words.favorite,
                memorizationRate = percent(words.memorized, words.total),
                totalQuizSessionCount = quizzes.sessions, totalQuizCount = quizzes.questions,
                totalCorrectCount = quizzes.correct, totalWrongCount = quizzes.wrong,
                quizAccuracyRate = percent(quizzes.correct, quizzes.questions),
                todayFlashcardCount = daily?.studiedCount ?: 0,
                todayQuizCount = (daily?.correctCount ?: 0) + (daily?.wrongCount ?: 0),
                todayCorrectCount = daily?.correctCount ?: 0, todayWrongCount = daily?.wrongCount ?: 0,
                weeklyFlashcardCount = week.sumOf { it.studiedCount },
                weeklyQuizCount = weeklyCorrect + weeklyWrong,
                weeklyCorrectCount = weeklyCorrect, weeklyWrongCount = weeklyWrong
            )
        }
    }

    fun percent(part: Int, total: Int): Double = if (total == 0) 0.0 else
        (part * 100.0 / total).toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()
}
