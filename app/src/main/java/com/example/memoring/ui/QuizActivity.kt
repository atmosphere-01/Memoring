package com.example.memoring.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.memoring.R

class QuizActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY_ID = "categoryId"
    }

    private var questions: List<QuizQuestion> = emptyList()
    private var index = 0
    private var answered = false
    private var correctCount = 0

    private val circledNums = listOf("①", "②", "③", "④")

    private lateinit var wordView: TextView
    private lateinit var progressView: TextView
    private lateinit var optionsContainer: LinearLayout
    private lateinit var btnNext: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        wordView = findViewById(R.id.quizWord)
        progressView = findViewById(R.id.quizProgress)
        optionsContainer = findViewById(R.id.optionsContainer)
        btnNext = findViewById(R.id.btnNext)

        // 카테고리 검증: id 가 없거나(=-1) 0 이하이면 퀴즈 시작 거부
        val categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1)
        if (categoryId <= 0) {
            finish()
            return
        }

        // TODO(백엔드): 검증된 categoryId 로 실제 단어 조회 + QuizSession 생성
        questions = DummyData.quizByCategory(categoryId)
        if (questions.isEmpty()) {
            finish()
            return
        }

        val categoryName = DummyData.categoryById(categoryId)?.name ?: "퀴즈"
        findViewById<TextView>(R.id.quizTitle).text = "$categoryName 퀴즈"

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        btnNext.setOnClickListener {
            if (!answered) return@setOnClickListener
            if (index < questions.size - 1) {
                index++
                bindQuestion()
            } else {
                showResult()
            }
        }

        bindQuestion()
    }

    private fun bindQuestion() {
        answered = false
        val q = questions[index]
        wordView.text = q.word
        progressView.text = "${index + 1} / ${questions.size}"

        setNextEnabled(false)
        btnNext.text = if (index == questions.size - 1) "결과 보기" else "다음 문제"

        optionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        q.options.forEachIndexed { i, option ->
            val view = inflater.inflate(R.layout.item_quiz_option, optionsContainer, false)
            view.findViewById<TextView>(R.id.optionNum).text = circledNums.getOrElse(i) { "" }
            view.findViewById<TextView>(R.id.optionText).text = option
            view.setOnClickListener { onOptionSelected(i) }
            optionsContainer.addView(view)
        }
    }

    private fun onOptionSelected(selected: Int) {
        if (answered) return
        answered = true
        val q = questions[index]
        val isCorrect = selected == q.answerIndex
        // 정답률 통계를 위해 정답/오답 모두 기록
        LearningStats.recordQuizAnswer(this, isCorrect)
        if (isCorrect) {
            correctCount++
            // 정답 1개를 오늘의 학습 목표에 반영하고, 암기 완료 목록에도 단어 추가
            LearningStats.addQuizCorrect(this)
            LearningStats.addMemorizedWord(this, q.word, q.options[q.answerIndex])
        } else {
            // 틀린 단어는 복습 필요로 기록
            LearningStats.addReviewWord(this, q.word)
        }

        // 정답은 초록, 오답 선택 시 빨강으로 표시하고 정답도 함께 강조
        for (i in 0 until optionsContainer.childCount) {
            val row = optionsContainer.getChildAt(i)
            row.isClickable = false
            when (i) {
                q.answerIndex -> row.setBackgroundResource(R.drawable.bg_option_correct)
                selected -> row.setBackgroundResource(R.drawable.bg_option_wrong)
            }
        }
        setNextEnabled(true)
    }

    private fun setNextEnabled(enabled: Boolean) {
        btnNext.isEnabled = enabled
        btnNext.alpha = if (enabled) 1f else 0.5f
    }

    private fun showResult() {
        wordView.text = "$correctCount / ${questions.size}"
        progressView.text = "완료"
        optionsContainer.removeAllViews()

        val done = TextView(this).apply {
            text = "🎉 퀴즈 완료! 총 ${questions.size}문제 중 ${correctCount}개 정답"
            textSize = 15f
            setTextColor(getColor(R.color.ink_soft))
            gravity = Gravity.CENTER
        }
        optionsContainer.addView(done)

        btnNext.text = "홈으로"
        setNextEnabled(true)
        btnNext.setOnClickListener { finish() }
    }
}
