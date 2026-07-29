package com.example.memoring.ui

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.memoring.R
import com.example.memoring.WordListActivity
import com.example.memoring.data.AppDb
import com.example.memoring.data.CURRENT_USER_ID
import com.example.memoring.data.entity.WordEntity
import com.example.memoring.data.repository.WordRepository
import com.example.memoring.data.util.CsvHelper
import kotlinx.coroutines.launch
import java.util.Locale

/** 마이페이지: 프로필 · CSV 단어장 업로드 · 학습 통계 */
class MyPageActivity : AppCompatActivity() {

    private val toneColors = mapOf(
        "coral" to R.color.coral,
        "green" to R.color.green,
        "blue" to R.color.blue,
    )

    // CSV 파일 선택 런처
    private val pickCsv = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { importCsv(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        val root = findViewById<LinearLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        findViewById<LinearLayout>(R.id.uploadCard).setOnClickListener {
            // 대부분의 기기가 csv 를 text/csv 또는 그 외 MIME 로 보고하므로 넓게 허용 후 확장자로 검증
            pickCsv.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
        }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.navQuiz).setOnClickListener {
            startActivity(Intent(this, CategorySelectActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navWord).setOnClickListener {
            startActivity(Intent(this, WordListActivity::class.java))
        }
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        // TODO(백엔드): 설정 / 프로필 수정 화면 연결
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
        buildWeekChart()
        renderDecks()
    }

    private fun refreshStats() {
        findViewById<TextView>(R.id.statStreak).text = "${LearningStats.streak(this)}일"
        findViewById<TextView>(R.id.statMemorized).text =
            LearningStats.memorizedCount(this).toString()
        // 정답률: 소수점 첫째 자리 반올림, 기록 없으면 0.0%
        findViewById<TextView>(R.id.statAccuracy).text =
            String.format(Locale.KOREAN, "%.1f%%", LearningStats.accuracyPercent(this))
    }

    /** 이번 주 학습: 요일별 실제 학습량 막대 + 총합 */
    private fun buildWeekChart() {
        val container = findViewById<LinearLayout>(R.id.weekContainer)
        container.removeAllViews()

        val counts = LearningStats.thisWeekLearned(this)
        val labels = listOf("월", "화", "수", "목", "금", "토", "일")
        val todayIdx = LearningStats.todayWeekdayIndex()
        val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)

        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val trackH = dp(44)
        val minBar = dp(3)

        for (i in 0..6) {
            val isToday = i == todayIdx

            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { if (i > 0) marginStart = dp(7) }
            }
            val track = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM
                setBackgroundResource(R.drawable.bg_bar_track)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, trackH)
            }
            // 학습이 있으면 최소 높이 보장 후 비율만큼, 없으면 0
            val barH = if (counts[i] <= 0) 0 else minBar + (trackH - minBar) * counts[i] / maxCount
            val bar = View(this).apply {
                setBackgroundResource(if (isToday) R.drawable.bg_bar_coral else R.drawable.bg_bar_green)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barH)
            }
            track.addView(bar)

            val text = TextView(this).apply {
                text = labels[i]
                textSize = 9f
                gravity = Gravity.CENTER
                setTextColor(if (isToday) getColor(R.color.coral) else getColor(R.color.ink_soft))
                if (isToday) setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(4) }
            }

            col.addView(track)
            col.addView(text)
            container.addView(col)
        }

        // 총 학습 = 이번 주 학습량 합계
        findViewById<TextView>(R.id.weekTotal).text = "총 ${LearningStats.weeklyTotal(this)}개 학습"

        // 목표 달성률: 하루 10개=100%, 7일 모두 채우면 100%
        val goalPercent = LearningStats.weeklyGoalPercent(this)
        findViewById<TextView>(R.id.goalPercentText).text = "목표의 ${goalPercent}% 달성"

        // 지난주 대비 증감
        val diff = goalPercent - LearningStats.lastWeekGoalPercent(this)
        val vs = findViewById<TextView>(R.id.vsLastWeekText)
        vs.text = when {
            diff > 0 -> "지난주보다 +${diff}%"
            diff < 0 -> "지난주보다 ${diff}%"
            else -> "지난주와 동일"
        }
        vs.setTextColor(getColor(if (diff >= 0) R.color.green else R.color.coral))
    }

    /** 단어장 목록 = DB 카테고리별 단어 수 */
    private fun renderDecks() {
        val list = findViewById<LinearLayout>(R.id.deckList)
        val tones = listOf("coral", "green", "blue")
        lifecycleScope.launch {
            val db = AppDb.get(this@MyPageActivity)
            val categories = db.categoryDao().getCategoriesByUserId(CURRENT_USER_ID)
            val counts = categories.map { db.wordDao().getWordsByCategory(it.categoryId).size }

            findViewById<TextView>(R.id.deckCount).text = "${categories.size}개"
            list.removeAllViews()
            val inflater = LayoutInflater.from(this@MyPageActivity)
            categories.forEachIndexed { i, cat ->
                val item = inflater.inflate(R.layout.item_deck, list, false)
                val icon = item.findViewById<TextView>(R.id.deckIcon)
                icon.backgroundTintList = getColorStateList(toneColors[tones[i % tones.size]] ?: R.color.coral)
                item.findViewById<TextView>(R.id.deckName).text = cat.categoryName
                item.findViewById<TextView>(R.id.deckMeta).text = "${"%,d".format(counts[i])}개 단어"
                // 단어장 항목 클릭 → 해당 카테고리 단어 목록으로 진입
                item.setOnClickListener {
                    startActivity(
                        Intent(this@MyPageActivity, WordListActivity::class.java)
                            .putExtra(WordListActivity.EXTRA_CATEGORY_ID, cat.categoryId)
                    )
                }
                list.addView(item)
            }
        }
    }

    /** 선택한 CSV를 파싱해 "내 단어장" 카테고리로 DB에 적재 */
    private fun importCsv(uri: Uri) {
        val name = displayName(uri)
        if (name == null || !name.lowercase().endsWith(".csv")) return

        lifecycleScope.launch {
            val rows = runCatching {
                contentResolver.openInputStream(uri)?.use { CsvHelper.parseCsvStream(it) }
            }.getOrNull().orEmpty()
            if (rows.isEmpty()) return@launch

            val db = AppDb.get(this@MyPageActivity)
            // "내 단어장" 카테고리 찾기 (없으면 첫 카테고리 fallback)
            val categories = db.categoryDao().getCategoriesByUserId(CURRENT_USER_ID)
            val myVocabId = categories.firstOrNull { it.categoryName == "내 단어장" }?.categoryId
                ?: categories.firstOrNull()?.categoryId ?: return@launch

            // 이미 내 단어장에 있는 단어(소문자 기준)
            val existing = db.wordDao().getWordsByCategory(myVocabId)
                .map { it.word.trim().lowercase() }
                .toMutableSet()

            val words = rows.mapNotNull { (word, meaning, pos) ->
                val key = word.trim().lowercase()
                // CSV 내 중복 + 기존 DB 중복 모두 제거
                if (key.isEmpty() || !existing.add(key)) return@mapNotNull null
                WordEntity(
                    categoryId = myVocabId,
                    word = word.trim(),
                    meaning = meaning,
                    partOfSpeech = pos?.ifBlank { null }
                )
            }
            if (words.isNotEmpty()) db.wordDao().insertWords(words)
            renderDecks() // 내 단어장 개수 갱신

            // 예문 자동 채우기 (FreeDictionary) — 뒤이어 배경에서 진행
            runCatching {
                WordRepository(db.wordDao(), CsvHelper).fetchAndFillExamplesForCategory(myVocabId)
            }
        }
    }

    private fun displayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        return uri.lastPathSegment
    }
}
