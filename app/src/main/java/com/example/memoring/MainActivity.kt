package com.example.memoring

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.example.memoring.adapter.WordAdapter
import com.example.memoring.databinding.ActivityMainBinding
import com.example.memoring.viewmodel.WordListViewModel
import androidx.core.widget.addTextChangedListener
import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.memoring.data.WordListItem
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: WordListViewModel
    private lateinit var adapter: WordAdapter

    private val addWordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val word = data?.getStringExtra("word") ?: return@registerForActivityResult
            val meaning = data.getStringExtra("meaning") ?: ""
            val partOfSpeech = data.getStringExtra("partOfSpeech")
            val example = data.getStringExtra("example")
            val categoryId = data.getIntExtra("categoryId", 1)

            val newWord = WordListItem(
                wordId = (0..100000).random(),
                word = word,
                meaning = meaning,
                partOfSpeech = partOfSpeech?.ifBlank { null },
                exampleSentence = example?.ifBlank { null },
                categoryId = categoryId,
                isFavorite = false,
                memorizationStatus = "UNLEARNED"
            )
            viewModel.addWord(newWord)
        }
    }

    private val wordDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val wordId = data.getIntExtra("wordId", -1)
            if (wordId == -1) return@registerForActivityResult

            if (data.getBooleanExtra("deleted", false)) {
                viewModel.deleteWord(wordId)
            } else {
                viewModel.updateWord(
                    wordId = wordId,
                    meaning = data.getStringExtra("meaning") ?: "",
                    partOfSpeech = data.getStringExtra("partOfSpeech"),
                    example = data.getStringExtra("example"),
                    isFavorite = data.getBooleanExtra("isFavorite", false),
                    memorizationStatus = data.getStringExtra("memorizationStatus") ?: "UNLEARNED"
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[WordListViewModel::class.java]

        setupRecyclerView()
        setupCategoryChips()
        setupSearch()
        observeViewModel()

        binding.btnAddWord.setOnClickListener {
            addWordLauncher.launch(Intent(this, WordAddActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = WordAdapter(
            emptyList(),
            onFavoriteClick = { wordId -> viewModel.toggleFavorite(wordId) },
            onItemClick = { item ->
                val intent = Intent(this, WordDetailActivity::class.java).apply {
                    putExtra("wordId", item.wordId)
                    putExtra("word", item.word)
                    putExtra("meaning", item.meaning)
                    putExtra("partOfSpeech", item.partOfSpeech)
                    putExtra("example", item.exampleSentence)
                    putExtra("isFavorite", item.isFavorite)
                    putExtra("memorizationStatus", item.memorizationStatus)
                }
                wordDetailLauncher.launch(intent)
            }
        )
        binding.rvWords.layoutManager = LinearLayoutManager(this)
        binding.rvWords.adapter = adapter
    }

    private fun setupCategoryChips() {
        viewModel.categories.observe(this) { categories ->
            binding.chipGroupCategory.removeAllViews()
            categories.forEach { category ->
                val chip = Chip(this).apply {
                    text = category.categoryName
                    isCheckable = true
                    isChecked = category.categoryId == 0
                    chipBackgroundColor = ContextCompat.getColorStateList(this@MainActivity, R.color.chip_background_selector)
                    setTextColor(ContextCompat.getColorStateList(this@MainActivity, R.color.chip_text_selector))
                    chipStrokeWidth = 1f
                    chipStrokeColor = ContextCompat.getColorStateList(this@MainActivity, R.color.line_color)
                    setOnClickListener { viewModel.onCategorySelect(category.categoryId) }
                }
                binding.chipGroupCategory.addView(chip)
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.onSearchQueryChange(text?.toString() ?: "")
        }
    }

    private fun observeViewModel() {
        viewModel.filteredWords.observe(this) { words ->
            adapter.updateItems(words)
        }
    }
}