package com.example.memoring.data

val dummyCategories = listOf(
    CategoryItem(ALL_CATEGORY_ID, "전체"),
    CategoryItem(1, "TOEIC"),
    CategoryItem(2, "여행 영어"),
    CategoryItem(3, "내 단어장")
)

val dummyWords = listOf(
    WordListItem(
        wordId = 1, word = "apple", meaning = "사과",
        partOfSpeech = "명사", exampleSentence = "I eat an apple.",
        categoryId = 1, isFavorite = true, memorizationStatus = "KNOWN"
    ),
    WordListItem(
        wordId = 2, word = "maintain", meaning = "유지하다",
        partOfSpeech = "동사", exampleSentence = "She maintains order.",
        categoryId = 1, isFavorite = false, memorizationStatus = "CONFUSED"
    ),
    WordListItem(
        wordId = 3, word = "achieve", meaning = "달성하다",
        partOfSpeech = "동사", exampleSentence = "He achieved his goal.",
        categoryId = 1, isFavorite = true, memorizationStatus = "UNKNOWN"
    ),
    WordListItem(
        wordId = 4, word = "computer", meaning = "컴퓨터",
        partOfSpeech = "명사", exampleSentence = "I use a computer.",
        categoryId = 2, isFavorite = false, memorizationStatus = "KNOWN"
    ),
    WordListItem(
        wordId = 5, word = "airport", meaning = "공항",
        partOfSpeech = null, exampleSentence = null,
        categoryId = 2, isFavorite = false, memorizationStatus = "UNLEARNED"
    )
)