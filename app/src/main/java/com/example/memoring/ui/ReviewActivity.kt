package com.example.memoring.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.example.memoring.R

/** 복습이 필요한(퀴즈 오답·플래시카드에서 헷갈린) 단어 목록 화면 */
class ReviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val words = LearningStats.reviewWords(this)
        findViewById<TextView>(R.id.reviewCount).text = "총 ${words.size}개"

        val list = findViewById<LinearLayout>(R.id.reviewFullList)
        val scroll = findViewById<NestedScrollView>(R.id.scroll)
        val empty = findViewById<LinearLayout>(R.id.emptyState)

        if (words.isEmpty()) {
            scroll.visibility = View.GONE
            empty.visibility = View.VISIBLE
            return
        }

        val inflater = LayoutInflater.from(this)
        words.forEach { word ->
            val item = inflater.inflate(R.layout.item_review_word, list, false)
            item.findViewById<TextView>(R.id.reviewEng).text = word.word
            item.findViewById<TextView>(R.id.reviewKor).text = word.meaning
            item.findViewById<TextView>(R.id.reviewStatus).text = word.status
            list.addView(item)
        }
    }
}
