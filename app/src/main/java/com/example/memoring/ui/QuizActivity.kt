package com.example.memoring.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.memoring.R
import com.example.memoring.data.AppDb
import com.example.memoring.data.entity.WordEntity
import kotlinx.coroutines.launch

class QuizActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY_ID = "categoryId"
        const val EXTRA_CATEGORY_NAME = "categoryName"
        private const val MAX_QUESTIONS = 10
    }

    private var questions: List<QuizQuestion> = emptyList()
    private var index = 0
    private var answered = false
    private var correctCount = 0

    private val circledNums = listOf("①", "②", "③", "④")

    private lateinit var wordView: TextView
    private lateinit var questionView: TextView
    private lateinit var progressView: TextView
    private lateinit var optionsContainer: LinearLayout
    private lateinit var btnNext: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        wordView = findViewById(R.id.quizWord)
        questionView = findViewById(R.id.quizQuestion)
        progressView = findViewById(R.id.quizProgress)
        optionsContainer = findViewById(R.id.optionsContainer)
        btnNext = findViewById(R.id.btnNext)

        // 카테고리 검증: id 가 없거나(=-1) 0 이하이면 퀴즈 시작 거부
        val categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1)
        if (categoryId <= 0) {
            finish()
            return
        }

        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "퀴즈"
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

        // DB의 해당 카테고리 단어로 문제 생성
        lifecycleScope.launch {
            val words = AppDb.get(this@QuizActivity).wordDao().getWordsByCategory(categoryId)
            questions = buildQuestions(categoryId, words)
            if (questions.isEmpty()) {
                finish()
                return@launch
            }
            bindQuestion()
        }
    }

    /**
     * DB 단어로 4지선다 문제 생성.
     * - 예문이 있고 그 안에 단어가 있으면 → 빈칸 채우기(보기=단어)
     * - 그 외 → 뜻 맞히기(보기=뜻)
     */
    private fun buildQuestions(categoryId: Int, words: List<WordEntity>): List<QuizQuestion> {
        val valid = words.filter { it.word.isNotBlank() && it.meaning.isNotBlank() }
        if (valid.size < 2) return emptyList()
        val allWords = valid.map { it.word }.distinct()
        val allMeanings = valid.map { it.meaning }.distinct()

        return valid.shuffled().take(MAX_QUESTIONS).map { w ->
            val example = w.exampleSentence?.trim()
            val blanked = if (example.isNullOrBlank()) null else blankOut(example, w.word)

            if (blanked != null && blanked != example) {
                // 빈칸 채우기: 예문의 단어를 _____ 로, 보기는 단어들
                val distractors = allWords.filter { !it.equals(w.word, ignoreCase = true) }
                    .shuffled().take(3)
                val options = (distractors + w.word).shuffled()
                QuizQuestion(
                    categoryId = categoryId,
                    word = w.word,
                    meaning = w.meaning,
                    prompt = blanked,
                    question = "빈칸에 알맞은 단어는?",
                    isSentence = true,
                    options = options,
                    answerIndex = options.indexOf(w.word)
                )
            } else {
                // 뜻 맞히기: 보기는 뜻들
                val distractors = allMeanings.filter { it != w.meaning }.shuffled().take(3)
                val options = (distractors + w.meaning).shuffled()
                QuizQuestion(
                    categoryId = categoryId,
                    word = w.word,
                    meaning = w.meaning,
                    prompt = w.word,
                    question = "이 단어의 뜻으로 알맞은 것은?",
                    isSentence = false,
                    options = options,
                    answerIndex = options.indexOf(w.meaning)
                )
            }
        }
    }

    /** 예문에서 대상 단어(대소문자 무시, 단어 경계)를 빈칸으로. 없으면 원문 그대로 반환. */
    private fun blankOut(sentence: String, word: String): String {
        val re = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
        return if (re.containsMatchIn(sentence)) re.replace(sentence, "_____") else sentence
    }

    private fun bindQuestion() {
        answered = false
        val q = questions[index]
        wordView.text = q.prompt
        // 빈칸 문장은 길어서 작게, 단어는 크게
        wordView.textSize = if (q.isSentence) 22f else 38f
        questionView.text = q.question
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
        // 정답률 통계를 위해 정답/오답 모두 기록 + 오늘 학습량 1건 누적
        LearningStats.recordQuizAnswer(this, isCorrect)
        LearningStats.recordStudied(this)
        if (isCorrect) {
            correctCount++
            // 정답 1개를 오늘의 학습 목표에 반영하고, 암기 완료 목록에도 단어 추가
            LearningStats.addQuizCorrect(this)
            LearningStats.addMemorizedWord(this, q.word, q.meaning)
        } else {
            // 틀린 단어는 복습 필요로 기록 (뜻과 함께)
            LearningStats.addReviewWord(this, q.word, q.meaning, "오답")
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
        wordView.textSize = 38f
        wordView.text = "$correctCount / ${questions.size}"
        questionView.text = ""
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
