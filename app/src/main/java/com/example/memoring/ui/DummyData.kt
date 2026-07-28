package com.example.memoring.ui

/** 화면 전용 더미 모델 (Room 엔티티와 분리해 UI 시안을 가볍게 유지) */
data class Flashcard(
    val category: String,
    val word: String,
    val meaning: String
)

data class QuizCategory(
    val id: Int,
    val name: String,
    val wordCount: Int
)

data class QuizQuestion(
    val categoryId: Int,
    val word: String,
    val options: List<String>,
    val answerIndex: Int
)

data class ReviewWord(
    val word: String,
    val meaning: String,
    val status: String
)

/** 앱 전역에서 재사용하는 더미 데이터 모음 */
object DummyData {

    val flashcards = listOf(
        Flashcard("TOEIC", "maintain", "유지하다"),
        Flashcard("TOEIC", "achieve", "달성하다"),
        Flashcard("TOEIC", "acquire", "습득하다"),
        Flashcard("여행 영어", "reservation", "예약"),
        Flashcard("TOEIC", "reliable", "믿을 수 있는"),
        Flashcard("내 단어장", "computer", "컴퓨터"),
    )

    /**
     * 퀴즈 카테고리. wordCount 가 0인 카테고리는 "빈 카테고리"로 퀴즈 시작이 거부된다.
     * (실제 앱에서는 백엔드가 CategoryEntity + 단어 수를 내려줌)
     */
    val categories = listOf(
        QuizCategory(1, "TOEIC", 3),
        QuizCategory(2, "여행 영어", 2),
        QuizCategory(3, "내 단어장", 0),
    )

    val quiz = listOf(
        QuizQuestion(1, "maintain", listOf("포기하다", "유지하다", "구매하다", "분석하다"), 1),
        QuizQuestion(1, "achieve", listOf("도착하다", "삭제하다", "달성하다", "빌리다"), 2),
        QuizQuestion(1, "reliable", listOf("믿을 수 있는", "위험한", "비싼", "느린"), 0),
        QuizQuestion(2, "reservation", listOf("취소", "환불", "예약", "지연"), 2),
        QuizQuestion(2, "apple", listOf("사과", "컴퓨터", "자동차", "책"), 0),
    )

    /** categoryId 로 카테고리 조회 (없으면 null) */
    fun categoryById(id: Int): QuizCategory? = categories.firstOrNull { it.id == id }

    /** 해당 카테고리의 퀴즈 문제 목록 */
    fun quizByCategory(categoryId: Int): List<QuizQuestion> =
        quiz.filter { it.categoryId == categoryId }

    val reviewWords = listOf(
        ReviewWord("maintain", "유지하다", "헷갈려요"),
        ReviewWord("achieve", "달성하다", "모르겠어요"),
        ReviewWord("acquire", "습득하다", "헷갈려요"),
    )
}
