package com.example.memoring.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoring.data.AppDb
import com.example.memoring.data.MemoringRepository
import com.example.memoring.data.WordListItem
import com.example.memoring.data.entity.WordEntity
import com.example.memoring.domain.ClockDateProvider
import kotlinx.coroutines.launch
import com.example.memoring.data.CategoryItem
import com.example.memoring.data.ALL_CATEGORY_ID
import com.example.memoring.data.CURRENT_USER_ID

class WordListViewModel(context: Context) : ViewModel() {

    private val repository = MemoringRepository(AppDb.get(context), ClockDateProvider())

    private var allWords: List<WordListItem> = emptyList()

    private val _categories = MutableLiveData<List<CategoryItem>>(emptyList())
    val categories: LiveData<List<CategoryItem>> get() = _categories

    private val _filteredWords = MutableLiveData<List<WordListItem>>(emptyList())
    val filteredWords: LiveData<List<WordListItem>> get() = _filteredWords

    private var selectedCategoryId = ALL_CATEGORY_ID
    private var searchQuery = ""

    init {
        loadCategories()
        loadWords()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val cats = repository.getCategories(CURRENT_USER_ID)
            val items = listOf(CategoryItem(ALL_CATEGORY_ID, "전체")) +
                    cats.map { CategoryItem(it.categoryId, it.categoryName) }
            _categories.value = items
        }
    }

    private fun loadWords() {
        viewModelScope.launch {
            allWords = repository.getWordsWithStatus(CURRENT_USER_ID, null)
            applyFilter()
        }
    }

    private fun applyFilter() {
        _filteredWords.value = allWords.filter { w ->
            val matchesCategory = selectedCategoryId == ALL_CATEGORY_ID || w.categoryId == selectedCategoryId
            val matchesQuery = searchQuery.isBlank() ||
                    w.word.contains(searchQuery, ignoreCase = true) ||
                    w.meaning.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        applyFilter()
    }

    fun onCategorySelect(categoryId: Int) {
        selectedCategoryId = categoryId
        applyFilter()
    }

    fun toggleFavorite(wordId: Int) {
        val word = allWords.find { it.wordId == wordId } ?: return
        viewModelScope.launch {
            repository.toggleFavorite(CURRENT_USER_ID, wordId, !word.isFavorite)
            loadWords()
        }
    }

    fun addWord(newWord: WordListItem) {
        // WordAddActivity에서 이미 저장 처리하므로 여기선 목록만 새로고침
        loadWords()
    }

    fun deleteWord(wordId: Int) {
        val word = allWords.find { it.wordId == wordId } ?: return
        viewModelScope.launch {
            repository.deleteWordById(CURRENT_USER_ID, wordId, word.categoryId)
            loadWords()
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
        val word = allWords.find { it.wordId == wordId } ?: return
        viewModelScope.launch {
            repository.updateWordFields(wordId, word.categoryId, meaning, partOfSpeech, example)
            repository.toggleFavorite(CURRENT_USER_ID, wordId, isFavorite)
            repository.updateMemorizationStatus(CURRENT_USER_ID, wordId, memorizationStatus)
            loadWords()
        }
    }
}