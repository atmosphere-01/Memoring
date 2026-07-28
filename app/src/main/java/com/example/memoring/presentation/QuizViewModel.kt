package com.example.memoring.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoring.data.MemoringRepository
import com.example.memoring.domain.QuizGenerator
import com.example.memoring.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

const val CATEGORY_REQUIRED_MESSAGE = "퀴즈를 시작하려면 카테고리를 선택해 주세요."

internal fun requireSelectedCategory(categoryId: Int?): Int =
    requireNotNull(categoryId?.takeIf { it > 0 }) { CATEGORY_REQUIRED_MESSAGE }

data class QuizUiState(
    val quizSessionId: Int? = null, val questions: List<QuizQuestion> = emptyList(),
    val answers: List<AnsweredQuestion> = emptyList(), val currentIndex: Int = 0,
    val selectedAnswer: String? = null, val isAnswered: Boolean = false,
    val isCorrect: Boolean? = null, val correctCount: Int = 0, val wrongCount: Int = 0,
    val isCompleted: Boolean = false, val isLoading: Boolean = false, val errorMessage: String? = null
)

class QuizViewModel(private val repository: MemoringRepository, private val generator: QuizGenerator) : ViewModel() {
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()
    private var parameters: Params? = null
    private data class Params(val userId: Int, val categoryId: Int, val type: QuizType, val count: Int)

    fun startQuiz(userId: Int, categoryId: Int?, quizType: QuizType, questionCount: Int) {
        val selectedCategoryId = runCatching { requireSelectedCategory(categoryId) }
            .getOrElse {
                parameters = null
                _uiState.value = QuizUiState(errorMessage = CATEGORY_REQUIRED_MESSAGE)
                return
            }
        parameters = Params(userId, selectedCategoryId, quizType, questionCount)
        viewModelScope.launch {
            _uiState.value = QuizUiState(isLoading = true)
            runCatching {
                val candidates = repository.quizCandidates(userId, selectedCategoryId).map {
                    QuizCandidate(it.wordId, it.categoryId, it.word, it.meaning,
                        it.memorizationStatus?.let(MemorizationStatus::valueOf) ?: MemorizationStatus.UNLEARNED,
                        it.wrongCnt ?: 0)
                }
                val questions = generator.generate(candidates, quizType, questionCount)
                check(questions.isNotEmpty()) { "선택한 범위에 출제 가능한 단어가 없습니다." }
                val sessionId = repository.createQuizSession(userId, selectedCategoryId, questions.size)
                sessionId to questions
            }.onSuccess { (id, questions) ->
                _uiState.value = QuizUiState(quizSessionId = id, questions = questions,
                    answers = questions.map(::AnsweredQuestion))
            }.onFailure { _uiState.value = QuizUiState(errorMessage = it.message ?: "퀴즈 시작 실패") }
        }
    }

    fun submitAnswer(answer: String) {
        val state = _uiState.value
        if (answer.isBlank() || state.isAnswered || state.isLoading || state.isCompleted) return
        val question = state.questions.getOrNull(state.currentIndex) ?: return
        val session = state.quizSessionId ?: return
        val user = parameters?.userId ?: return
        val correct = answer == question.correctAnswer
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.saveQuizAnswer(session, user, question.wordId, answer, correct) }
                .onSuccess { saved ->
                    if (!saved) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "이미 답변한 문제입니다.") }
                    } else _uiState.update {
                        val changed = it.answers.toMutableList()
                        changed[it.currentIndex] = AnsweredQuestion(question, true, answer, correct)
                        it.copy(answers = changed, selectedAnswer = answer, isAnswered = true,
                            isCorrect = correct, correctCount = it.correctCount + if (correct) 1 else 0,
                            wrongCount = it.wrongCount + if (correct) 0 else 1, isLoading = false)
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "답안 저장 실패") }
                }
        }
    }

    fun moveToNextQuestion() {
        val state = _uiState.value
        if (!state.isAnswered) { _uiState.update { it.copy(errorMessage = "답안을 먼저 선택해 주세요.") }; return }
        val last = state.currentIndex == state.questions.lastIndex
        _uiState.update { it.copy(currentIndex = if (last) it.currentIndex else it.currentIndex + 1,
            selectedAnswer = null, isAnswered = false, isCorrect = null, isCompleted = last, errorMessage = null) }
    }
    fun retryQuiz() {
        val p = parameters ?: return
        startQuiz(p.userId, p.categoryId, p.type, p.count)
    }
}
