package com.example.memoring.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.memoring.R
import com.example.memoring.data.AppDb
import com.example.memoring.data.CURRENT_USER_ID
import kotlinx.coroutines.launch

/**
 * 퀴즈 시작 전 카테고리 선택 화면 (카테고리 선택 필수).
 *
 * - DB(categories/words)에서 카테고리와 단어 수를 읽어온다.
 * - 단어가 0개인 "빈 카테고리"는 선택해도 시작되지 않는다.
 */
class CategorySelectActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_QUIZ = "quiz"
        const val MODE_FLASHCARD = "flashcard"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_select)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_QUIZ
        val isFlashcard = mode == MODE_FLASHCARD
        findViewById<TextView>(R.id.categorySelectTitle).text =
            if (isFlashcard) "플래시카드" else "퀴즈"
        findViewById<TextView>(R.id.categorySelectPrompt).text =
            if (isFlashcard) "어떤 단어장을 학습할까요?" else "어떤 카테고리로 풀어볼까요?"
        findViewById<TextView>(R.id.categorySelectSubtitle).text =
            if (isFlashcard) "단어장을 선택하면 플래시카드가 시작돼요"
            else "카테고리를 선택하면 퀴즈가 시작돼요"

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val list = findViewById<LinearLayout>(R.id.categoryList)
        val inflater = LayoutInflater.from(this)

        lifecycleScope.launch {
            val db = AppDb.get(this@CategorySelectActivity)
            val categories = db.categoryDao().getCategoriesByUserId(CURRENT_USER_ID)

            list.removeAllViews()
            categories.forEach { category ->
                val count = db.wordDao().getWordsByCategory(category.categoryId).size
                val item = inflater.inflate(R.layout.item_category, list, false)
                item.findViewById<TextView>(R.id.categoryName).text = category.categoryName

                val empty = count <= 0
                item.findViewById<TextView>(R.id.categoryCount).text =
                    if (empty) "단어 없음" else "${count}개 단어"
                item.alpha = if (empty) 0.45f else 1f
                item.findViewById<TextView>(R.id.categoryChevron).text = if (empty) "" else "›"

                item.setOnClickListener {
                    startLearning(mode, category.categoryId, count, category.categoryName)
                }
                list.addView(item)
            }
        }
    }

    /** 카테고리 검증 후 선택한 학습 화면을 시작한다. */
    private fun startLearning(mode: String, categoryId: Int, wordCount: Int, name: String) {
        // categoryId 가 0 이하이거나 단어가 없으면 시작 거부
        if (categoryId <= 0 || wordCount <= 0) return

        val target = if (mode == MODE_FLASHCARD) {
            Intent(this, FlashcardActivity::class.java)
                .putExtra(FlashcardActivity.EXTRA_CATEGORY_ID, categoryId)
                .putExtra(FlashcardActivity.EXTRA_CATEGORY_NAME, name)
        } else {
            Intent(this, QuizActivity::class.java)
                .putExtra(QuizActivity.EXTRA_CATEGORY_ID, categoryId)
                .putExtra(QuizActivity.EXTRA_CATEGORY_NAME, name)
        }
        startActivity(target)
    }
}
