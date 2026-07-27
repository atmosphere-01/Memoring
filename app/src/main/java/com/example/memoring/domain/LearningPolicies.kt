package com.example.memoring.domain

import com.example.memoring.domain.model.MemorizationStatus
import java.time.LocalDate

data class StreakResult(val continuousDay: Int, val lastLearningDate: LocalDate)

class LearningStreakCalculator {
    fun calculate(current: Int, lastLearningDate: LocalDate?, today: LocalDate): StreakResult {
        val days = when {
            lastLearningDate == null -> 1
            lastLearningDate == today -> current
            lastLearningDate == today.minusDays(1) -> current + 1
            else -> 1
        }
        return StreakResult(days, today)
    }
}

class MemorizationStatusPolicy {
    fun resolve(currentStatus: MemorizationStatus, isCorrect: Boolean): MemorizationStatus =
        when {
            isCorrect -> currentStatus
            currentStatus == MemorizationStatus.UNLEARNED -> MemorizationStatus.UNKNOWN
            currentStatus == MemorizationStatus.KNOWN -> MemorizationStatus.CONFUSED
            else -> currentStatus
        }
}

fun isReviewTarget(
    status: MemorizationStatus,
    wrongCount: Int,
    includeWrongKnown: Boolean = true
): Boolean = status == MemorizationStatus.UNKNOWN ||
    status == MemorizationStatus.CONFUSED ||
    (includeWrongKnown && status == MemorizationStatus.KNOWN && wrongCount > 0)
