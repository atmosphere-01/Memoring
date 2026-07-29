package com.example.memoring.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.memoring.data.*
import com.example.memoring.data.entity.WordEntity
import com.example.memoring.data.repository.WordRepository
import com.example.memoring.data.util.CsvHelper
import kotlinx.coroutines.launch

class WordListViewModel(app: Application) : AndroidViewModel(app) {

    private val wordDao = AppDb.get(app).wordDao()
    private val repository = WordRepository(wordDao, CsvHelper)

    private var allWords: List<WordListItem> = emptyList()

    private val _categories = MutableLiveData(dummyCategories)
    val categories: LiveData<List<CategoryItem>> get() = _categories

    private val _selectedCategoryId = MutableLiveData(ALL_CATEGORY_ID)
    private val _searchQuery = MutableLiveData("")

    private val _filteredWords = MutableLiveData<List<WordListItem>>()
    val filteredWords: LiveData<List<WordListItem>> get() = _filteredWords

    init {
        // 개발자 제공 CSV(있으면) 로드 — 없으면 addWord 시 번역 API로 대체
        CsvHelper.loadAssetDictionary(app)
        reload()
    }

    /** DB에서 전체 단어를 다시 읽어온다 */
    private fun reload() {
        viewModelScope.launch {
            allWords = wordDao.getAllWords().map { it.toListItem() }
            updateFilteredWords()
        }
    }

    private fun updateFilteredWords() {
        val catId = _selectedCategoryId.value ?: ALL_CATEGORY_ID
        val query = _searchQuery.value ?: ""
        _filteredWords.value = allWords.filter { w ->
            val matchesCategory = catId == ALL_CATEGORY_ID || w.categoryId == catId
            val matchesQuery = query.isBlank() ||
                    w.word.contains(query, ignoreCase = true) ||
                    w.meaning.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        updateFilteredWords()
    }

    fun onCategorySelect(categoryId: Int) {
        _selectedCategoryId.value = categoryId
        updateFilteredWords()
    }

    fun toggleFavorite(wordId: Int) {
        // 즐겨찾기는 아직 화면 표시용(메모리)만 — UserWord 연동은 추후
        allWords = allWords.map { if (it.wordId == wordId) it.copy(isFavorite = !it.isFavorite) else it }
        updateFilteredWords()
    }

    /** 단어 추가: 번역/사전 API로 뜻·예문을 채워 DB에 저장 후 목록 갱신 */
    fun addWordViaApi(englishWord: String, meaning: String?, categoryId: Int) {
        viewModelScope.launch {
            // 저장은 화면을 벗어나도 취소되지 않도록 앱 스코프에서 수행 (API가 느려도 유실 방지)
            AppScope.io.launch {
                repository.processAndSaveWord(englishWord, meaning, categoryId)
            }.join()
            reload()
        }
    }

    fun updateWord(
        wordId: Int,
        meaning: String,
        partOfSpeech: String?,
        example: String?,
        isFavorite: Boolean,
        memorizationStatus: String
    ) {
        viewModelScope.launch {
            allWords.firstOrNull { it.wordId == wordId }?.let { target ->
                wordDao.updateWord(
                    WordEntity(
                        wordId = wordId,
                        categoryId = target.categoryId,
                        word = target.word,
                        meaning = meaning,
                        partOfSpeech = partOfSpeech,
                        exampleSentence = example
                    )
                )
            }
            // 즐겨찾기/암기상태는 화면 표시용으로만 반영
            allWords = allWords.map {
                if (it.wordId == wordId) it.copy(
                    meaning = meaning, partOfSpeech = partOfSpeech, exampleSentence = example,
                    isFavorite = isFavorite, memorizationStatus = memorizationStatus
                ) else it
            }
            updateFilteredWords()
        }
    }

    fun deleteWord(wordId: Int) {
        viewModelScope.launch {
            allWords.firstOrNull { it.wordId == wordId }?.let { target ->
                wordDao.deleteWord(
                    WordEntity(
                        wordId = wordId,
                        categoryId = target.categoryId,
                        word = target.word,
                        meaning = target.meaning
                    )
                )
            }
            reload()
        }
    }

    private fun WordEntity.toListItem() = WordListItem(
        wordId = wordId,
        word = word,
        meaning = meaning,
        partOfSpeech = partOfSpeech,
        exampleSentence = exampleSentence,
        categoryId = categoryId,
        isFavorite = false,
        memorizationStatus = "UNLEARNED"
    )
}
