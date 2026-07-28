package com.example.memoring.data

// 화면에 실제로 뿌릴 형태 (WordEntity + UserWordEntity 합친 것)
data class WordListItem(
    val wordId: Int,
    val word: String,
    val meaning: String,
    val partOfSpeech: String?,
    val exampleSentence: String?,
    val categoryId: Int,
    val isFavorite: Boolean,
    val memorizationStatus: String   // UNLEARNED / UNKNOWN / CONFUSED / KNOWN
)

// 카테고리 화면 표시용
data class CategoryItem(
    val categoryId: Int,
    val categoryName: String
)

const val ALL_CATEGORY_ID = 0

// 지금은 로그인 기능이 없는 단일 사용자 앱이라 임시로 고정
const val CURRENT_USER_ID = 1