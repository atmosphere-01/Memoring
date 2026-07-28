package com.example.memoring

import com.example.memoring.domain.*
import com.example.memoring.domain.model.*
import com.example.memoring.presentation.CATEGORY_REQUIRED_MESSAGE
import com.example.memoring.presentation.requireSelectedCategory
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class QuizCategorySelectionTest {
    @Test fun nullCategoryIsRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            requireSelectedCategory(null)
        }
        assertEquals(CATEGORY_REQUIRED_MESSAGE, error.message)
    }

    @Test fun nonPositiveCategoryIsRejected() {
        listOf(0, -1).forEach { categoryId ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                requireSelectedCategory(categoryId)
            }
            assertEquals(CATEGORY_REQUIRED_MESSAGE, error.message)
        }
    }

    @Test fun selectedCategoryIsPreserved() {
        assertEquals(27, requireSelectedCategory(27))
    }
}

class QuizGeneratorTest {
    private val words = listOf(
        candidate(1, MemorizationStatus.KNOWN, 0, "apple", "사과"),
        candidate(2, MemorizationStatus.UNKNOWN, 3, "car", "자동차"),
        candidate(3, MemorizationStatus.CONFUSED, 2, "book", "책"),
        candidate(4, MemorizationStatus.UNLEARNED, 0, "computer", "컴퓨터"),
        candidate(5, MemorizationStatus.KNOWN, 1, "desk", "책상")
    )
    private val generator = QuizGenerator(Random(7))

    @Test fun requestedCountAndUniqueQuestions() {
        val result = generator.generate(words, QuizType.WORD_TO_MEANING, 4)
        assertEquals(4, result.size)
        assertEquals(result.size, result.map { it.wordId }.distinct().size)
    }
    @Test fun answerIncludedAndOptionsUnique() {
        generator.generate(words, QuizType.MEANING_TO_WORD, 5).forEach {
            assertTrue(it.correctAnswer in it.options)
            assertEquals(it.options.size, it.options.distinct().size)
        }
    }
    @Test fun reviewPrioritizesUnknownThenConfused() {
        val result = generator.generate(words, QuizType.REVIEW, 2)
        assertEquals(listOf(2, 3), result.map { it.wordId })
    }
    @Test fun fewerWordsReturnsAvailableCount() {
        assertEquals(2, generator.generate(words.take(2), QuizType.WORD_TO_MEANING, 20).size)
    }
    private fun candidate(id: Int, status: MemorizationStatus, wrong: Int, word: String, meaning: String) =
        QuizCandidate(id, 1, word, meaning, status, wrong)
}

class MemorizationStatusPolicyTest {
    private val policy = MemorizationStatusPolicy()
    @Test fun unlearnedWrongBecomesUnknown() =
        assertEquals(MemorizationStatus.UNKNOWN, policy.resolve(MemorizationStatus.UNLEARNED, false))
    @Test fun knownWrongBecomesConfused() =
        assertEquals(MemorizationStatus.CONFUSED, policy.resolve(MemorizationStatus.KNOWN, false))
    @Test fun unknownCorrectIsStable() =
        assertEquals(MemorizationStatus.UNKNOWN, policy.resolve(MemorizationStatus.UNKNOWN, true))
    @Test fun confusedCorrectIsStable() =
        assertEquals(MemorizationStatus.CONFUSED, policy.resolve(MemorizationStatus.CONFUSED, true))
}

class LearningStreakCalculatorTest {
    private val calculator = LearningStreakCalculator()
    private val today = LocalDate.of(2026, 7, 26)
    @Test fun firstLearningIsOne() = assertEquals(1, calculator.calculate(0, null, today).continuousDay)
    @Test fun sameDayDoesNotIncrease() = assertEquals(5, calculator.calculate(5, today, today).continuousDay)
    @Test fun yesterdayIncreases() = assertEquals(6, calculator.calculate(5, today.minusDays(1), today).continuousDay)
    @Test fun gapResets() = assertEquals(1, calculator.calculate(5, today.minusDays(2), today).continuousDay)
}

class StatisticsMathTest {
    // DB Flow 결합은 DAO 통합 테스트 대상이며, 순수 비율/주간 경계는 고정 날짜로 검증한다.
    @Test fun zeroDenominatorsReturnZero() {
        assertEquals(0.0, percent(0, 0), 0.0)
    }
    @Test fun ratesAreRoundedToOneDecimal() {
        assertEquals(66.7, percent(2, 3), 0.0)
        assertEquals(75.0, percent(3, 4), 0.0)
    }
    @Test fun weekStartsMondayAndSumsThroughToday() {
        val today = LocalDate.of(2026, 7, 26) // Sunday
        val values = (0..8).associate { today.minusDays(it.toLong()) to 1 }
        val monday = today.minusDays(6)
        assertEquals(7, values.filterKeys { !it.isBefore(monday) && !it.isAfter(today) }.values.sum())
    }
    private fun percent(part: Int, total: Int) =
        if (total == 0) 0.0 else kotlin.math.round(part * 1000.0 / total) / 10.0
}
