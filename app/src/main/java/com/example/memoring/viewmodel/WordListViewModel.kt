package com.example.memoring.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.memoring.data.*

class WordListViewModel : ViewModel() {

    private var allWords = dummyWords

    private val _categories = MutableLiveData(dummyCategories)
    val categories: LiveData<List<CategoryItem>> get() = _categories

    private val _selectedCategoryId = MutableLiveData(ALL_CATEGORY_ID)
    private val _searchQuery = MutableLiveData("")

    private val _filteredWords = MutableLiveData<List<WordListItem>>()
    val filteredWords: LiveData<List<WordListItem>> get() = _filteredWords

    init {
        updateFilteredWords()
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
        // 더미 리스트라 지금은 즐겨찾기 토글이 필터링 결과에만 즉시 반영됨
        _filteredWords.value = _filteredWords.value?.map {
            if (it.wordId == wordId) it.copy(isFavorite = !it.isFavorite) else it
        }
    }
    fun addWord(newWord: WordListItem) {
        allWords = allWords + newWord
        updateFilteredWords()
    }

    fun updateWord(
        wordId: Int,
        meaning: String,
        partOfSpeech: String?,
        example: String?,
        isFavorite: Boolean,
        memorizationStatus: String
    ) {
        allWords = allWords.map {
            if (it.wordId == wordId) {
                it.copy(
                    meaning = meaning,
                    partOfSpeech = partOfSpeech,
                    exampleSentence = example,
                    isFavorite = isFavorite,
                    memorizationStatus = memorizationStatus
                )
            } else it
        }
        updateFilteredWords()
    }

    fun deleteWord(wordId: Int) {
        allWords = allWords.filter { it.wordId != wordId }
        updateFilteredWords()
    }
}