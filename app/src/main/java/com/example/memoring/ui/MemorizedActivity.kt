package com.example.memoring.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.example.memoring.R

/** 퀴즈에서 맞힌(암기 완료) 단어 목록 화면 */
class MemorizedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memorized)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val words = LearningStats.memorizedWords(this)
        findViewById<TextView>(R.id.memCount).text = "총 ${words.size}개"

        val list = findViewById<LinearLayout>(R.id.memList)
        val scroll = findViewById<NestedScrollView>(R.id.scroll)
        val empty = findViewById<LinearLayout>(R.id.emptyState)

        if (words.isEmpty()) {
            scroll.visibility = View.GONE
            empty.visibility = View.VISIBLE
            return
        }

        val inflater = LayoutInflater.from(this)
        words.forEach { word ->
            val item = inflater.inflate(R.layout.item_memorized_word, list, false)
            item.findViewById<TextView>(R.id.memEng).text = word.word
            item.findViewById<TextView>(R.id.memKor).text = word.meaning
            list.addView(item)
        }
    }
}
