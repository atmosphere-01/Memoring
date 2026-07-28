package com.example.memoring.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.memoring.R

/**
 * 퀴즈 시작 전 카테고리 선택 화면 (카테고리 선택 필수).
 *
 * - 단어가 0개인 "빈 카테고리"는 선택해도 시작되지 않는다.
 * - 선택한 카테고리의 id 를 검증한 뒤에만 [QuizActivity] 로 넘긴다.
 */
class CategorySelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_select)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val list = findViewById<LinearLayout>(R.id.categoryList)
        val inflater = LayoutInflater.from(this)

        // TODO(백엔드): DummyData.categories 를 실제 CategoryEntity 목록으로 교체
        DummyData.categories.forEach { category ->
            val item = inflater.inflate(R.layout.item_category, list, false)
            item.findViewById<TextView>(R.id.categoryName).text = category.name

            val empty = category.wordCount <= 0
            item.findViewById<TextView>(R.id.categoryCount).text =
                if (empty) "단어 없음" else "${category.wordCount}개 단어"

            // 빈 카테고리는 흐리게 표시하고 화살표 숨김
            item.alpha = if (empty) 0.45f else 1f
            item.findViewById<TextView>(R.id.categoryChevron).text = if (empty) "" else "›"

            item.setOnClickListener { startQuiz(category) }
            list.addView(item)
        }
    }

    /** 카테고리 검증 후 퀴즈 시작. 검증 실패 시 시작을 거부한다. */
    private fun startQuiz(category: QuizCategory) {
        // categoryId 가 null/0/음수이거나 단어가 없으면 시작 거부
        if (category.id <= 0) return
        if (category.wordCount <= 0) return

        val intent = Intent(this, QuizActivity::class.java)
            .putExtra(QuizActivity.EXTRA_CATEGORY_ID, category.id)
        startActivity(intent)
    }
}
