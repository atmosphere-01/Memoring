package com.example.memoring

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.example.memoring.adapter.WordAdapter
import com.example.memoring.data.WordListItem
import com.example.memoring.databinding.ActivityWordListBinding
import com.example.memoring.data.ALL_CATEGORY_ID
import com.example.memoring.viewmodel.WordListViewModel

class WordListActivity : AppCompatActivity() {

    companion object {
        /** 진입 시 미리 선택할 카테고리 (없으면 전체) */
        const val EXTRA_CATEGORY_ID = "categoryId"
    }

    private lateinit var binding: ActivityWordListBinding
    private lateinit var viewModel: WordListViewModel

    /** 진입 시 초기 선택 카테고리 (마이페이지 단어장 목록에서 넘어올 때 사용) */
    private var initialCategoryId = ALL_CATEGORY_ID
    private lateinit var adapter: WordAdapter

    private val addWordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val word = data?.getStringExtra("word") ?: return@registerForActivityResult
            val meaning = data.getStringExtra("meaning")
            val categoryId = data.getIntExtra("categoryId", 1)

            // 뜻이 비어 있으면 번역/사전 API가 자동으로 채워 DB에 저장
            viewModel.addWordViaApi(word, meaning?.ifBlank { null }, categoryId)
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
        binding = ActivityWordListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 시스템 바(상태바/네비바)에 콘텐츠가 겹치지 않도록 인셋만큼 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val pad = (16 * resources.displayMetrics.density).toInt()
            v.setPadding(pad, bars.top + pad, pad, bars.bottom + pad)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }

        initialCategoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, ALL_CATEGORY_ID)

        viewModel = ViewModelProvider(this)[WordListViewModel::class.java]

        setupRecyclerView()
        setupCategoryChips()
        setupSearch()
        observeViewModel()

        // 마이페이지 단어장 목록에서 넘어왔으면 해당 카테고리로 필터
        viewModel.onCategorySelect(initialCategoryId)

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
                    isChecked = category.categoryId == initialCategoryId
                    chipBackgroundColor = ContextCompat.getColorStateList(this@WordListActivity, R.color.chip_background_selector)
                    setTextColor(ContextCompat.getColorStateList(this@WordListActivity, R.color.chip_text_selector))
                    chipStrokeWidth = 1f
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