package com.example.memoring.ui

/** 플래시카드 화면용 더미 카드 */
data class Flashcard(
    val category: String,
    val word: String,
    val meaning: String
)

/**
 * 퀴즈 한 문제.
 * - isSentence=false: [prompt]에 단어, 보기=뜻 (뜻 맞히기)
 * - isSentence=true : [prompt]에 빈칸(_____) 예문, 보기=단어 (빈칸 채우기)
 */
data class QuizQuestion(
    val categoryId: Int,
    val word: String,        // 대상 영단어 (기록용)
    val meaning: String,     // 뜻 (기록용)
    val prompt: String,      // 크게 표시할 것: 단어 또는 빈칸 예문
    val question: String,    // 안내 문구
    val isSentence: Boolean, // 빈칸 문장형이면 true
    val options: List<String>,
    val answerIndex: Int
)

/** 화면 전용 더미 데이터 (플래시카드) */
object DummyData {

    val flashcards = listOf(
        Flashcard("TOEIC", "maintain", "유지하다"),
        Flashcard("TOEIC", "achieve", "달성하다"),
        Flashcard("TOEIC", "acquire", "습득하다"),
        Flashcard("여행 영어", "reservation", "예약"),
        Flashcard("TOEIC", "reliable", "믿을 수 있는"),
        Flashcard("내 단어장", "computer", "컴퓨터"),
    )
}
