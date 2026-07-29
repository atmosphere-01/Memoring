package com.example.memoring

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.memoring.data.dummyCategories
import com.example.memoring.databinding.ActivityWordAddBinding

class WordAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWordAddBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // "전체"는 등록 대상 카테고리가 아니라서 제외
        val selectableCategories = dummyCategories.filter { it.categoryId != 0 }
        val categoryNames = selectableCategories.map { it.categoryName }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryNames)
        binding.spinnerCategory.adapter = adapter

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            val word = binding.etWord.text.toString().trim()
            val meaning = binding.etMeaning.text.toString().trim()
            val partOfSpeech = binding.etPartOfSpeech.text.toString().trim()
            val example = binding.etExample.text.toString().trim()

            if (word.isEmpty()) {
                binding.tvError.text = "영어 단어는 꼭 입력해주세요 (뜻을 비우면 자동 번역돼요)"
                binding.tvError.visibility = android.view.View.VISIBLE
                return@setOnClickListener
            }

            val selectedIndex = binding.spinnerCategory.selectedItemPosition
            val categoryId = selectableCategories[selectedIndex].categoryId

            val resultIntent = Intent().apply {
                putExtra("word", word)
                putExtra("meaning", meaning)
                putExtra("partOfSpeech", partOfSpeech)
                putExtra("example", example)
                putExtra("categoryId", categoryId)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}