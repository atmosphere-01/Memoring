package com.example.memoring.domain

import com.example.memoring.domain.model.MemorizationStatus
import com.example.memoring.domain.model.QuizCandidate
import com.example.memoring.domain.model.QuizQuestion
import com.example.memoring.domain.model.QuizType
import kotlin.random.Random

class QuizGenerator(private val random: Random = Random.Default) {
    fun generate(
        candidates: List<QuizCandidate>,
        quizType: QuizType,
        questionCount: Int
    ): List<QuizQuestion> {
        if (questionCount <= 0) return emptyList()
        val unique = candidates.distinctBy { it.wordId }
        val selected = when (quizType) {
            QuizType.REVIEW -> selectReview(unique, questionCount)
            else -> unique.shuffled(random).take(questionCount)
        }
        return selected.map { answer ->
            val displayType = if (quizType == QuizType.REVIEW) QuizType.WORD_TO_MEANING else quizType
            val correct = answer.answerFor(displayType)
            val distractors = unique.asSequence()
                .filter { it.categoryId == answer.categoryId && it.wordId != answer.wordId }
                .map { it.answerFor(displayType) }
                .filter { it != correct }
                .distinct()
                .toList()
                .shuffled(random)
                .take(3)
            QuizQuestion(
                wordId = answer.wordId,
                questionText = if (displayType == QuizType.MEANING_TO_WORD) answer.meaning else answer.word,
                options = (distractors + correct).shuffled(random),
                correctAnswer = correct,
                quizType = quizType
            )
        }
    }

    private fun selectReview(words: List<QuizCandidate>, count: Int): List<QuizCandidate> {
        fun priority(item: QuizCandidate): Int = when {
            item.status == MemorizationStatus.UNKNOWN -> 0
            item.status == MemorizationStatus.CONFUSED -> 1
            item.wrongCount > 0 -> 2
            item.status == MemorizationStatus.UNLEARNED -> 3
            else -> 4
        }
        // 우선순위 그룹 안에서는 먼저 섞은 뒤 오답 수 내림차순을 적용한다.
        return words.shuffled(random)
            .sortedWith(compareBy<QuizCandidate> { priority(it) }.thenByDescending { it.wrongCount })
            .take(count)
    }

    private fun QuizCandidate.answerFor(type: QuizType): String =
        if (type == QuizType.MEANING_TO_WORD) word else meaning
}
