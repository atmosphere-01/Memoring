package com.example.memoring.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.memoring.R
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

        renderDecks()

        findViewById<LinearLayout>(R.id.uploadCard).setOnClickListener {
            // 대부분의 기기가 csv 를 text/csv 또는 그 외 MIME 로 보고하므로 넓게 허용 후 확장자로 검증
            pickCsv.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
        }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.navQuiz).setOnClickListener {
            startActivity(Intent(this, CategorySelectActivity::class.java))
        }
        // TODO(백엔드): 설정 / 프로필 수정 화면 연결
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun refreshStats() {
        findViewById<TextView>(R.id.statStreak).text = "${LearningStats.streak(this)}일"
        findViewById<TextView>(R.id.statMemorized).text =
            LearningStats.memorizedCount(this).toString()
        // 정답률: 소수점 첫째 자리 반올림, 기록 없으면 0.0%
        findViewById<TextView>(R.id.statAccuracy).text =
            String.format(Locale.KOREAN, "%.1f%%", LearningStats.accuracyPercent(this))
    }

    private fun renderDecks() {
        val list = findViewById<LinearLayout>(R.id.deckList)
        list.removeAllViews()
        val decks = DeckStore.decks(this)
        findViewById<TextView>(R.id.deckCount).text = "${decks.size}개"

        val inflater = LayoutInflater.from(this)
        decks.forEach { deck ->
            val item = inflater.inflate(R.layout.item_deck, list, false)
            val icon = item.findViewById<TextView>(R.id.deckIcon)
            val colorRes = toneColors[deck.tone] ?: R.color.coral
            icon.backgroundTintList = getColorStateList(colorRes)
            item.findViewById<TextView>(R.id.deckName).text = deck.name
            item.findViewById<TextView>(R.id.deckMeta).text =
                "${deck.date} 업로드 · ${"%,d".format(deck.count)} 단어"
            list.addView(item)
        }
    }

    /** 선택한 CSV를 읽어 단어 수를 세고 단어장 목록에 추가 */
    private fun importCsv(uri: Uri) {
        val name = displayName(uri)
        if (name == null || !name.lowercase().endsWith(".csv")) return

        val text = runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return

        val count = DeckStore.countCsvWords(text)
        if (count == 0) return

        // TODO(백엔드): 여기서 실제 단어를 WordEntity 로 DB에 적재
        val deckName = name.removeSuffix(".csv").removeSuffix(".CSV").trim().ifEmpty { "새 단어장" }
        DeckStore.addDeck(this, deckName, count)
        renderDecks()
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
