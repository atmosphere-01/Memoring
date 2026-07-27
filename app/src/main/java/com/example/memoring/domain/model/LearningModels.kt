package com.example.memoring.domain.model

enum class MemorizationStatus { UNLEARNED, UNKNOWN, CONFUSED, KNOWN }

enum class QuizType { WORD_TO_MEANING, MEANING_TO_WORD, REVIEW }

data class QuizCandidate(
    val wordId: Int,
    val categoryId: Int,
    val word: String,
    val meaning: String,
    val status: MemorizationStatus,
    val wrongCount: Int
)

data class QuizQuestion(
    val wordId: Int,
    val questionText: String,
    val options: List<String>,
    val correctAnswer: String,
    val quizType: QuizType
)

data class WordItem(
    val wordId: Int,
    val word: String,
    val meaning: String,
    val exampleSentence: String?,
    val status: MemorizationStatus
)

data class AnsweredQuestion(
    val question: QuizQuestion,
    val isAnswered: Boolean = false,
    val selectedAnswer: String? = null,
    val isCorrect: Boolean? = null
)

data class LearningStatisticsUiModel(
    val totalWordCount: Int = 0,
    val learnedWordCount: Int = 0,
    val memorizedWordCount: Int = 0,
    val reviewWordCount: Int = 0,
    val favoriteWordCount: Int = 0,
    val memorizationRate: Double = 0.0,
    val totalQuizSessionCount: Int = 0,
    val totalQuizCount: Int = 0,
    val totalCorrectCount: Int = 0,
    val totalWrongCount: Int = 0,
    val quizAccuracyRate: Double = 0.0,
    val todayFlashcardCount: Int = 0,
    val todayQuizCount: Int = 0,
    val todayCorrectCount: Int = 0,
    val todayWrongCount: Int = 0,
    val weeklyFlashcardCount: Int = 0,
    val weeklyQuizCount: Int = 0,
    val weeklyCorrectCount: Int = 0,
    val weeklyWrongCount: Int = 0
)
