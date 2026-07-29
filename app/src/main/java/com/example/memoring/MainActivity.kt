package com.example.memoring

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.memoring.data.AppDb
import com.example.memoring.data.CURRENT_USER_ID
import com.example.memoring.data.repository.WordRepository
import com.example.memoring.data.util.CsvHelper
import com.example.memoring.ui.CategorySelectActivity
import com.example.memoring.ui.FlashcardActivity
import com.example.memoring.ui.LearningStats
import com.example.memoring.ui.MemorizedActivity
import com.example.memoring.ui.MyPageActivity
import com.example.memoring.ui.ReviewActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 시스템 바 인셋만큼 상/하 패딩 적용 (엣지투엣지 대응)
        val root = findViewById<LinearLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        // 접속 기록 → 연속일 갱신 (하루 1회만 반영)
        LearningStats.registerVisit(this)

        // 예문 없는 단어들의 예문을 FreeDictionary로 1회 채워둠 (배경)
        fillMissingExamplesOnce()

        findViewById<LinearLayout>(R.id.cardFlashcard).setOnClickListener {
            startActivity(Intent(this, FlashcardActivity::class.java))
        }
        // 퀴즈는 카테고리 선택을 거쳐야 시작 (카테고리 선택 필수)
        val toQuiz = { startActivity(Intent(this, CategorySelectActivity::class.java)) }
        findViewById<LinearLayout>(R.id.cardQuiz).setOnClickListener { toQuiz() }
        findViewById<LinearLayout>(R.id.navQuiz).setOnClickListener { toQuiz() }

        findViewById<LinearLayout>(R.id.cardMemorized).setOnClickListener {
            startActivity(Intent(this, MemorizedActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navMy).setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
        }
        // 단어 탭 → 단어 목록 화면 (팀원 구현)
        findViewById<LinearLayout>(R.id.navWord).setOnClickListener {
            startActivity(Intent(this, WordListActivity::class.java))
        }
        // 복습이 필요한 단어 전체보기 → 복습 필요 페이지
        findViewById<TextView>(R.id.reviewSeeAll).setOnClickListener {
            startActivity(Intent(this, ReviewActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // 퀴즈/플래시카드를 하고 돌아오면 목표·복습 목록이 즉시 반영되도록 화면 갱신
        refreshGoal()
        buildReviewList()
    }

    /** 예문이 비어 있는 단어들의 예문을 FreeDictionary로 채운다 (앱당 1회, 배경). */
    private fun fillMissingExamplesOnce() {
        val flags = getSharedPreferences("memoring_flags", MODE_PRIVATE)
        if (flags.getBoolean("examples_filled", false)) return
        lifecycleScope.launch {
            val db = AppDb.get(this@MainActivity)
            val repo = WordRepository(db.wordDao(), CsvHelper)
            db.categoryDao().getCategoriesByUserId(CURRENT_USER_ID).forEach { category ->
                runCatching { repo.fetchAndFillExamplesForCategory(category.categoryId) }
            }
            flags.edit().putBoolean("examples_filled", true).apply()
        }
    }

    private fun refreshGoal() {
        val done = LearningStats.goalCount(this)
        val target = LearningStats.GOAL_TARGET
        val shown = done.coerceAtMost(target)

        findViewById<TextView>(R.id.dateText).text = LearningStats.currentDateLabel()
        findViewById<TextView>(R.id.goalCountText).text = "${target}개 중 ${done}개 완료"
        findViewById<TextView>(R.id.goalCircle).text = "$shown / $target"
        findViewById<ProgressBar>(R.id.goalProgress).progress = shown
        findViewById<TextView>(R.id.streakText).text = "🔥 ${LearningStats.streak(this)}일 연속"
        findViewById<TextView>(R.id.goalSubtitle).text =
            if (done >= target) "오늘 목표 달성! 🎉" else "조금만 더 하면 목표 달성!"

        // 전체 단어 = DB(단어장)에 등록된 단어 수
        lifecycleScope.launch {
            val count = AppDb.get(this@MainActivity).wordDao().countAllWords()
            findViewById<TextView>(R.id.totalWords).text = count.toString()
        }
        // 암기 완료(퀴즈 정답 단어) / 복습 필요(오답·헷갈려요·모르겠어요) 개수 갱신
        findViewById<TextView>(R.id.memorizedCount).text =
            LearningStats.memorizedCount(this).toString()
        findViewById<TextView>(R.id.reviewNeededCount).text =
            LearningStats.reviewNeededCount(this).toString()
    }

    /** 홈 "복습이 필요한 단어" — 최근 복습 필요 단어 3개만 표시 */
    private fun buildReviewList() {
        val container = findViewById<LinearLayout>(R.id.reviewList)
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        LearningStats.reviewWords(this).take(3).forEach { word ->
            val item = inflater.inflate(R.layout.item_review_word, container, false)
            item.findViewById<TextView>(R.id.reviewEng).text = word.word
            item.findViewById<TextView>(R.id.reviewKor).text = word.meaning
            item.findViewById<TextView>(R.id.reviewStatus).text = word.status
            container.addView(item)
        }
    }
}
