package com.example.memoring

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.memoring.data.AppDb
import com.example.memoring.data.CategoryItem
import com.example.memoring.data.CURRENT_USER_ID
import com.example.memoring.data.util.CsvHelper
import com.example.memoring.data.repository.WordRepository
import com.example.memoring.databinding.ActivityWordAddBinding
import kotlinx.coroutines.launch

class WordAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWordAddBinding
    private lateinit var repository: WordRepository
    private var categories: List<CategoryItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDb.get(this)
        repository = WordRepository(db.wordDao(), CsvHelper)

        binding.btnBack.setOnClickListener {
            finish()
        }

        // 실제 DB에서 카테고리 불러오기
        lifecycleScope.launch {
            val cats = db.categoryDao().getCategoriesByUserId(CURRENT_USER_ID)
            categories = cats.map { CategoryItem(it.categoryId, it.categoryName) }

            val categoryNames = categories.map { it.categoryName }
            val adapter = ArrayAdapter(
                this@WordAddActivity,
                android.R.layout.simple_spinner_dropdown_item,
                categoryNames
            )
            binding.spinnerCategory.adapter = adapter
        }

        binding.btnSave.setOnClickListener {
            val word = binding.etWord.text.toString().trim()
            val meaning = binding.etMeaning.text.toString().trim()
            val example = binding.etExample.text.toString().trim()

            if (word.isEmpty() || meaning.isEmpty()) {
                binding.tvError.text = "영어 단어와 뜻은 꼭 입력해주세요"
                binding.tvError.visibility = android.view.View.VISIBLE
                return@setOnClickListener
            }

            if (categories.isEmpty()) {
                binding.tvError.text = "카테고리를 먼저 만들어주세요"
                binding.tvError.visibility = android.view.View.VISIBLE
                return@setOnClickListener
            }

            val selectedIndex = binding.spinnerCategory.selectedItemPosition
            val categoryId = categories[selectedIndex].categoryId

            binding.btnSave.isEnabled = false

            lifecycleScope.launch {
                val success = repository.processAndSaveWord(
                    englishWord = word,
                    meaningInput = meaning,
                    categoryId = categoryId
                )

                if (success) {
                    val resultIntent = Intent().apply {
                        putExtra("categoryId", categoryId)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    binding.btnSave.isEnabled = true
                    binding.tvError.text = "이미 등록된 단어이거나 저장에 실패했습니다"
                    binding.tvError.visibility = android.view.View.VISIBLE
                }
            }
        }
    }
}